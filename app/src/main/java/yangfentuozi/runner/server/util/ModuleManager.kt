package yangfentuozi.runner.server.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import yangfentuozi.runner.server.Server.Companion.HOME_PATH
import yangfentuozi.runner.server.Server.Companion.PAGE_SIZE
import yangfentuozi.runner.server.Server.Companion.SHELLS_PATH
import yangfentuozi.runner.server.Server.Companion.SHELL_PATH
import yangfentuozi.runner.server.Server.Companion.TAG
import yangfentuozi.runner.server.Server.Companion.USR_PATH
import yangfentuozi.runner.server.callback.IExitCallback
import yangfentuozi.runner.shared.data.TermModuleInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipFile

object ModuleManager {
    const val MODULES_PATH = "$USR_PATH/opt"

    private val workerThread by lazy(LazyThreadSafetyMode.NONE) {
        HandlerThread("ModuleWorker").apply { start() }
    }

    val workerHandler by lazy {
        Handler(workerThread.looper)
    }

    fun init() {
        arrayOf(
            USR_PATH,
            HOME_PATH,
            "$USR_PATH/bin",
            "$USR_PATH/lib",
            "$USR_PATH/etc",
            "$USR_PATH/etc/profile.d",
            "$USR_PATH/opt",
            "$USR_PATH/tmp"
        ).forEach {
            File(it).mkdirs()
        }
        try {
            FileWriter("$USR_PATH/etc/profile").use {
                it.write(
                    $$"""
                    #!/system/bin/sh

                    # !!!请不要修改此文件，服务每次启动都会覆盖它!!!
                    # !!!Please do not modify this file, it will be overwritten each time the service starts.!!!

                    # 加载profile.d下的所有sh配置
                    for i in $PREFIX/etc/profile.d/*.sh; do
                      if [ -r $i ]; then
                        . $i
                      fi
                    done
                    unset i
                """.trimIndent())
            }
        } catch (e: IOException) {
            Log.e(TAG, "writing profile error", e)
        }

        if (!File(SHELL_PATH).exists()) {
            try {
                Os.symlink("/system/bin/sh", SHELL_PATH)
            } catch (_: ErrnoException) {
            }
        }

        if (!File(SHELLS_PATH).exists()) {
            try {
                FileWriter(SHELLS_PATH).use {
                    it.write("/system/bin/sh")
                }
            } catch (e: IOException) {
                Log.e(TAG, "writing shells file error", e)
            }
        }
    }

    private fun exec(cmd: String) {
        val runnable = Runnable {
            val process = Runtime.getRuntime().exec("/system/bin/sh")
            process.outputStream.use {
                it.write(cmd.toByteArray())
            }
            process.waitFor()
        }

        if (Looper.myLooper() == workerThread.looper) {
            runnable.run()
        } else {
            workerHandler.post(runnable)
        }
    }

    fun isModule(moduleId: String): Boolean {
        if (moduleId.isEmpty()) return false
        val moduleProp = File("$MODULES_PATH/$moduleId/module.prop")
        return moduleProp.exists() && moduleProp.isFile
    }

    fun listModules(): List<TermModuleInfo> {
        val modules = mutableListOf<TermModuleInfo>()
        File(MODULES_PATH).listFiles()?.forEach { file ->
            val moduleProp = File(file, "module.prop")
            if (file.isDirectory && moduleProp.exists() && moduleProp.isFile) {
                try {
                    FileInputStream(moduleProp).use { `in` ->
                        modules.add(TermModuleInfo.fromInputStream(`in`))
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "getModuleInfo error", e)
                }
            }
        }
        return modules
    }

    fun getModuleInfo(moduleId: String): TermModuleInfo? {
        if (moduleId.isEmpty()) return null
        val moduleProp = File("$MODULES_PATH/$moduleId/module.prop")
        return if (moduleProp.exists() && moduleProp.isFile) {
            try {
                FileInputStream(moduleProp).use { `in` ->
                    TermModuleInfo.fromInputStream(`in`)
                }
            } catch (e: IOException) {
                Log.e(TAG, "getModuleInfo error", e)
                null
            }
        } else null
    }

    fun install(
        modZip: String,
        callback: IExitCallback,
        stdout: ParcelFileDescriptor
    ) {
        workerHandler.post {
            val writer = ParcelFileDescriptor.AutoCloseOutputStream(stdout).bufferedWriter()
            var unzipped = false
            var previousVersionBackupDir: String? = null
            var moduleDir = ""

            fun print(line: String) {
                Log.i(TAG, line)
                try {
                    writer.write("- ")
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "write output error", e)
                }
            }

            fun exit(successful: Boolean) {
                if (unzipped) {
                    if (previousVersionBackupDir != null) {
                        if (successful) {
                            print("Cleanup the previous version backup")
                            File(previousVersionBackupDir!!).deleteRecursively()
                        } else {
                            print("Cleanup and restore the previous version backup")
                            File(moduleDir).deleteRecursively()
                            File(previousVersionBackupDir!!).renameTo(File(moduleDir))
                        }
                    } else {
                        if (!successful) {
                            print("Cleanup temp dir")
                            File(moduleDir).deleteRecursively()
                        }
                    }
                }

                try {
                    writer.close()
                } catch (e: IOException) {
                    Log.e(TAG, "close writer error", e)
                }
                try {
                    callback.onExit(if (successful) 0 else 1)
                } catch (_: RemoteException) {
                }
            }

            fun abort(line: String) {
                Log.e(TAG, line)
                try {
                    writer.write("! ")
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "write output error", e)
                }
                exit(false)
            }

            try {
                print("Install terminal module: $modZip")
                val zipFile = ZipFile(modZip)
                val modulePropEntry = zipFile.getEntry("module.prop")
                val installShEntry = zipFile.getEntry("install.sh")
                if (modulePropEntry == null) {
                    abort("'module.prop' doesn't exist")
                    return@post
                }
                if (installShEntry == null) {
                    abort("'install.sh' doesn't exist")
                    return@post
                }
                val moduleInfo =
                    zipFile.getInputStream(modulePropEntry)
                        .use { TermModuleInfo.fromInputStream(it) }
                """
                moduleId: ${moduleInfo.moduleId}
                moduleName: ${moduleInfo.moduleName}
                version: ${moduleInfo.versionName}
                versionCode: ${moduleInfo.versionCode}
                author: ${moduleInfo.author}
                description: ${moduleInfo.description}
                """.trimIndent().split("\n").forEach {
                    print(it)
                }

                if (moduleInfo.moduleId.isNullOrEmpty()) {
                    abort("Module id is null or empty")
                    return@post
                }

                val moduleId = moduleInfo.moduleId!!
                moduleDir = "$MODULES_PATH/$moduleId"
                if (isModule(moduleId)) {
                    print("Backup the previous version")
                    previousVersionBackupDir = "${moduleDir}_backup_${UUID.randomUUID()}"
                    File(moduleDir).renameTo(File(previousVersionBackupDir))
                }

                print("Unzip files")
                unzipped = true
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val zipEntry = entries.nextElement()
                    try {
                        val file = File("$moduleDir/${zipEntry.name}")
                        print("Unzip '${zipEntry.name}' to '${file.absolutePath}'")
                        if (zipEntry.isDirectory) {
                            if (!file.exists()) file.mkdirs()
                        } else {
                            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                            if (!file.exists()) file.createNewFile()
                            zipFile.getInputStream(zipEntry).use { input ->
                                FileOutputStream(file).use {
                                    input.copyTo(it, bufferSize = PAGE_SIZE)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        abort("Unable to unzip file: ${zipEntry.name}\n${e.stackTraceToString()}")
                        return@post
                    }
                }
                print("Complete unzipping")

                print("Execute install script")
                try {
                    val process = Runtime.getRuntime().exec("/system/bin/sh")
                    process.outputStream.use {
                        it.write(("export HOME=$HOME_PATH\nexport MODDIR=$MODULES_PATH/$moduleId\nexport PREFIX=$USR_PATH\n/system/bin/sh $moduleDir/install.sh 2>&1\n").toByteArray())
                    }
                    process.inputStream.bufferedReader().use {
                        var line: String?
                        while (it.readLine().also { string -> line = string } != null) {
                            print("$line")
                        }
                    }
                    val ev = process.waitFor()
                    if (ev == 0) {
                        print("Install script exit successfully")
                    } else {
                        abort("Install script exit with non-zero value $ev")
                        return@post
                    }
                } catch (e: Exception) {
                    abort(e.stackTraceToString())
                    return@post
                }
                File("$MODULES_PATH/$moduleId/disable").createNewFile()

                print("Finish")
                exit(true)
                return@post
            } catch (e: IOException) {
                abort("Read terminal module file error\n${e.stackTraceToString()}")
                return@post
            }
        }
    }

    fun uninstall(
        moduleId: String,
        callback: IExitCallback,
        stdout: ParcelFileDescriptor,
        purge: Boolean
    ) {
        workerHandler.post {
            val writer = ParcelFileDescriptor.AutoCloseOutputStream(stdout).bufferedWriter()
            val moduleDir = "$MODULES_PATH/$moduleId"
            val exists = isModule(moduleId)

            fun print(line: String) {
                Log.i(TAG, line)
                try {
                    writer.write("- ")
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "write output error", e)
                }
            }

            fun exit(successful: Boolean) {
                if (exists) {
                    print("Cleanup module dir")
                    File(moduleDir).deleteRecursively()
                }
                try {
                    writer.close()
                } catch (e: IOException) {
                    Log.e(TAG, "close writer error", e)
                }
                try {
                    callback.onExit(if (successful) 0 else 1)
                } catch (_: RemoteException) {
                }
            }

            fun abort(line: String) {
                Log.e(TAG, line)
                try {
                    writer.write("! ")
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "write output error", e)
                }
                exit(false)
            }

            print("Uninstall terminal module: $moduleId")
            if (!exists) {
                abort("$moduleId hasn't been installed!")
                return@post
            }

            val uninstallScript = "$moduleDir/uninstall.sh"
            print("Execute uninstall script")
            try {
                val process = Runtime.getRuntime().exec("/system/bin/sh")
                process.outputStream.use {
                    it.write(("export HOME=$HOME_PATH\nexport MODDIR=$MODULES_PATH/$moduleId\nexport PREFIX=$USR_PATH\nexport PURGE=$purge\n/system/bin/sh $uninstallScript 2>&1\n").toByteArray())
                }
                process.inputStream.bufferedReader().use {
                    var line: String?
                    while (it.readLine().also { string -> line = string } != null) {
                        print("$line")
                    }
                }
                val ev = process.waitFor()
                if (ev == 0) {
                    print("Uninstall script exit successfully")
                } else {
                    abort("Uninstall script exit with non-zero value $ev")
                    return@post
                }
            } catch (e: Exception) {
                abort(e.stackTraceToString())
                return@post
            }
            print("Finish")
            exit(true)
            return@post
        }
    }
}