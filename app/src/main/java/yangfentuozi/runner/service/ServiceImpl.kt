package yangfentuozi.runner.service

import android.ddm.DdmHandleAppName
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import rikka.hidden.compat.PackageManagerApis
import yangfentuozi.runner.BuildConfig
import yangfentuozi.runner.service.callback.ExecResultCallback
import yangfentuozi.runner.service.callback.IExecResultCallback
import yangfentuozi.runner.service.callback.IInstallTermExtCallback
import yangfentuozi.runner.service.callback.InstallTermExtCallback
import yangfentuozi.runner.service.data.CommandInfo
import yangfentuozi.runner.service.data.EnvInfo
import yangfentuozi.runner.service.data.ProcessInfo
import yangfentuozi.runner.service.data.TermExtVersion
import yangfentuozi.runner.service.database.CommandDao
import yangfentuozi.runner.service.database.DataDbHelper
import yangfentuozi.runner.service.database.EnvironmentDao
import yangfentuozi.runner.service.fakecontext.FakeContext
import yangfentuozi.runner.service.util.ProcessUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.regex.Pattern
import java.util.zip.ZipFile
import kotlin.system.exitProcess

class ServiceImpl : IService.Stub() {
    companion object {
        const val TAG = "runner_server"
        const val DATA_PATH = "/data/local/tmp/runner"
        const val USR_PATH = "$DATA_PATH/usr"
        const val HOME_PATH = "$DATA_PATH/home"
        const val STARTER = "$HOME_PATH/.local/bin/starter"
        const val JNI_PROCESS_UTILS = "$HOME_PATH/.local/lib/libprocessutils.so"
        val PAGE_SIZE: Int = Os.sysconf(OsConstants._SC_PAGESIZE).toInt()

        fun tarGzDirectory(srcDir: File, tarGzFile: File) {
            TarArchiveOutputStream(GzipCompressorOutputStream(FileOutputStream(tarGzFile))).use { taos ->
                taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
                tarFileRecursive(srcDir, srcDir, taos)
            }
        }

        fun tarFileRecursive(rootDir: File, srcFile: File, taos: TarArchiveOutputStream) {
            var name = rootDir.toURI().relativize(srcFile.toURI()).path
            val isSymlink = try {
                srcFile.absolutePath != srcFile.canonicalPath
            } catch (_: IOException) {
                false
            }
            if (srcFile.isDirectory && !isSymlink) {
                if (name.isNotEmpty() && !name.endsWith("/")) name += "/"
                if (name.isNotEmpty()) {
                    val entry = TarArchiveEntry(srcFile, name)
                    taos.putArchiveEntry(entry)
                    taos.closeArchiveEntry()
                }
                srcFile.listFiles()?.forEach { child ->
                    tarFileRecursive(rootDir, child, taos)
                }
            } else if (isSymlink) {
                try {
                    val linkTarget = Os.readlink(srcFile.absolutePath)
                    val entry = TarArchiveEntry(name, TarArchiveEntry.LF_SYMLINK)
                    entry.linkName = linkTarget
                    taos.putArchiveEntry(entry)
                    taos.closeArchiveEntry()
                } catch (_: ErrnoException) {
                }
            } else {
                val entry = TarArchiveEntry(srcFile, name)
                entry.size = srcFile.length()
                taos.putArchiveEntry(entry)
                FileInputStream(srcFile).use { fis ->
                    val buffer = ByteArray(PAGE_SIZE)
                    var len: Int
                    while (fis.read(buffer).also { len = it } != -1) {
                        taos.write(buffer, 0, len)
                    }
                }
                taos.closeArchiveEntry()
            }
        }

        fun extractTarGz(tarGzFile: File, destDir: File) {
            if (!destDir.exists()) destDir.mkdirs()
            FileInputStream(tarGzFile).use { fis ->
                GzipCompressorInputStream(fis).use { gis ->
                    TarArchiveInputStream(gis).use { tais ->
                        var entry: TarArchiveEntry?
                        while (tais.nextEntry.also { entry = it } != null) {
                            val outFile = File(destDir, entry!!.name)
                            if (entry.isDirectory) {
                                if (!outFile.exists()) outFile.mkdirs()
                            } else if (entry.isSymbolicLink) {
                                val target = File(entry.linkName)
                                try {
                                    Os.symlink(target.path, outFile.path)
                                } catch (e: Exception) {
                                    Log.w(
                                        TAG,
                                        "extractTarGz: symlink failed: $outFile -> $target",
                                        e
                                    )
                                }
                            } else {
                                val parent = outFile.parentFile
                                if (parent != null && !parent.exists()) parent.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    val buffer = ByteArray(PAGE_SIZE)
                                    var len: Int
                                    while (tais.read(buffer).also { len = it } != -1) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                                outFile.setLastModified(entry.modTime.time)
                                outFile.setExecutable((entry.mode and "100".toInt(8)) != 0)
                            }
                        }
                    }
                }
            }
        }

        fun rmRF(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { f -> rmRF(f) }
            }
            file.delete()
        }

        fun ifExistsOrMkdirs(file: File) {
            if (!file.exists()) file.mkdirs()
        }

        fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
            BufferedInputStream(inputStream).use { `in` ->
                BufferedOutputStream(outputStream).use { out ->
                    val b = ByteArray(PAGE_SIZE)
                    var len: Int
                    while (`in`.read(b).also { len = it } != -1) {
                        out.write(b, 0, len)
                    }
                }
            }
        }
    }

    val mHandler: Handler
    private var dataDbHelper: DataDbHelper = DataDbHelper(FakeContext.get())
    private var commandDao: CommandDao = CommandDao(dataDbHelper.database)
    private var environmentDao: EnvironmentDao = EnvironmentDao(dataDbHelper.database)
    private val processUtils = ProcessUtils()

    init {
        DdmHandleAppName.setAppName(TAG, Os.getuid())
        Log.i(TAG, "start")
        ifExistsOrMkdirs(File("$HOME_PATH/.local/bin"))
        ifExistsOrMkdirs(File("$HOME_PATH/.local/lib"))
        mHandler = Handler(Looper.getMainLooper())
        var app: ZipFile? = null
        try {
            app = ZipFile(
                PackageManagerApis.getApplicationInfo(
                    BuildConfig.APPLICATION_ID,
                    0,
                    0
                )?.sourceDir
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                if (e is RemoteException) "get application info error" else "open apk zip file error",
                e
            )
        }
        if (app == null) {
            Log.w(TAG, "ignore unzip library from app zip file")
        } else {
            try {
                var entry = app.getEntry("lib/" + Build.SUPPORTED_ABIS[0] + "/libstarter.so")
                if (entry != null) {
                    Log.i(TAG, "unzip starter")
                    val `in` = app.getInputStream(entry)
                    val file = File(STARTER)
                    if (!file.exists()) file.createNewFile()
                    copyFile(`in`, FileOutputStream(file))
                    Os.chmod(STARTER, "700".toInt(8))
                } else {
                    Log.e(TAG, "libstarter.so doesn't exist")
                }
                entry = app.getEntry("lib/" + Build.SUPPORTED_ABIS[0] + "/libprocessutils.so")
                if (entry != null) {
                    Log.i(TAG, "unzip libprocessutils.so")
                    val `in` = app.getInputStream(entry)
                    val file = File(JNI_PROCESS_UTILS)
                    if (!file.exists()) file.createNewFile()
                    copyFile(`in`, FileOutputStream(file))
                    Os.chmod(JNI_PROCESS_UTILS, "500".toInt(8))
                    processUtils.loadLibrary()
                } else {
                    Log.e(TAG, "libprocessutils.so doesn't exist")
                }
            } catch (e: IOException) {
                Log.e(TAG, "unzip error", e)
            } catch (e: ErrnoException) {
                Log.e(TAG, "set permission error", e)
            } finally {
                try {
                    app.close()
                } catch (e: IOException) {
                    Log.e(TAG, "close apk zip file error", e)
                }
            }
        }
    }

    override fun destroy() {
        Log.i(TAG, "stop")
        dataDbHelper.close()
        exitProcess(0)
    }

    override fun exit() {
        destroy()
    }

    override fun version(): Int {
        return BuildConfig.VERSION_CODE
    }

    override fun exec(
        cmd: String?,
        ids: String?,
        procName: String?,
        callback: IExecResultCallback?
    ) {
        Thread {
            val callbackWrapper = ExecResultCallback(callback)
            try {
                if (!File(STARTER).exists()) {
                    Log.e(TAG, "starter not found")
                    callbackWrapper.onOutput("-1")
                    callbackWrapper.onOutput("starter not found")
                    callbackWrapper.onExit(127)
                    return@Thread
                } else if (!File("$USR_PATH/bin/bash").exists()) {
                    Log.e(TAG, "bash not found")
                    callbackWrapper.onOutput("-1")
                    callbackWrapper.onOutput("bash not found, may be you don't install terminal extension")
                    callbackWrapper.onExit(127)
                    return@Thread
                }
                try {
                    Os.chmod(STARTER, "700".toInt(8))
                    Os.chmod("$USR_PATH/bin/bash", "700".toInt(8))
                } catch (e: ErrnoException) {
                    Log.w(TAG, "set permission error", e)
                }
                val finalIds = if (ids.isNullOrEmpty()) "-1" else ids
                val finalProcName = if (procName.isNullOrEmpty()) "execTask" else procName
                val processBuilder = ProcessBuilder(STARTER, finalIds, finalProcName)
                val processEnv = processBuilder.environment()
                val customEnv = allEnv
                processEnv["PREFIX"] = USR_PATH
                processEnv["HOME"] = HOME_PATH
                processEnv["TMPDIR"] = "$USR_PATH/tmp"
                processEnv.merge(
                    "PATH",
                    "$HOME_PATH/.local/bin:$USR_PATH/bin:$USR_PATH/bin/applets"
                ) { old, new -> "$new:$old" }
                processEnv.merge(
                    "LD_LIBRARY_PATH",
                    "$HOME_PATH/.local/lib:$USR_PATH/lib"
                ) { old, new -> "$new:$old" }
                if (customEnv != null) {
                    for (entry in customEnv) {
                        entry.key?.let { key ->
                            entry.value?.let { value ->
                                processEnv.merge(key, value) { old, new ->
                                    new.replace(
                                        Regex("\\$(${Pattern.quote(key)}|\\{${Pattern.quote(key)}\\})"),
                                        old
                                    )
                                }
                            }
                        }
                    }
                }
                processBuilder.redirectErrorStream(true)
                val p = processBuilder.start()
                val out = p.outputStream
                out.write(
                    """
                    echo $$;. $USR_PATH/etc/profile;${cmd ?: ""};exit
                    """.trimIndent().toByteArray()
                )
                out.flush()
                out.close()
                val bufferedReader = BufferedReader(InputStreamReader(p.inputStream))
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    callbackWrapper.onOutput(line)
                }
                callbackWrapper.onExit(p.waitFor())
            } catch (e: InterruptedException) {
                Log.e(TAG, Log.getStackTraceString(e))
                callbackWrapper.onOutput(" ! Exception: " + Log.getStackTraceString(e))
            } catch (e: IOException) {
                Log.e(TAG, Log.getStackTraceString(e))
                callbackWrapper.onOutput(" ! Exception: " + Log.getStackTraceString(e))
            }
        }.start()
    }

    override fun size(): Int = commandDao.size()

    override fun read(position: Int): CommandInfo? = commandDao.read(position)

    override fun readAll(): Array<CommandInfo>? = commandDao.readAll()?.toTypedArray()

    override fun delete(position: Int) {
        commandDao.delete(position)
    }

    override fun edit(cmdInfo: CommandInfo?, position: Int) {
        if (cmdInfo != null) commandDao.edit(cmdInfo, position)
    }

    override fun insert(cmdInfo: CommandInfo?) {
        if (cmdInfo != null) commandDao.insert(cmdInfo)
    }

    override fun move(fromPosition: Int, toPosition: Int) {
        commandDao.move(fromPosition, toPosition)
    }

    override fun insertInto(cmdInfo: CommandInfo?, position: Int) {
        if (cmdInfo != null) commandDao.insertInto(cmdInfo, position)
    }

    override fun deleteEnv(key: String?) {
        if (key != null) environmentDao.delete(key)
    }

    override fun insertEnv(key: String?, value: String?): Boolean {
        return if (key != null && value != null) environmentDao.insert(key, value) else false
    }

    override fun updateEnv(from: EnvInfo?, to: EnvInfo?): Boolean {
        return if (from != null && to != null) environmentDao.update(
            from.key,
            from.value,
            to.key,
            to.value
        ) else false
    }

    override fun getEnv(key: String?): String? {
        return if (key != null) environmentDao.getValue(key) else null
    }

    override fun getAllEnv(): Array<EnvInfo>? {
        return environmentDao.all?.toTypedArray()
    }

    override fun getProcesses(): Array<ProcessInfo>? {
        return if (processUtils.isLibraryLoaded) {
            Log.i(TAG, "get processes")
            processUtils.getProcesses()
        } else {
            Log.e(TAG, "process utils library not loaded")
            null
        }
    }

    override fun sendSignal(pid: IntArray?, signal: Int): BooleanArray? {
        if (pid == null) return null
        return if (processUtils.isLibraryLoaded) {
            BooleanArray(pid.size) { i ->
                Log.i(TAG, "kill process: ${pid[i]}")
                processUtils.sendSignal(pid[i], signal)
            }
        } else {
            Log.e(TAG, "process utils library not loaded")
            null
        }
    }

    override fun backupData(output: String?, data: Boolean, termHome: Boolean, termUsr: Boolean) {
        if (output == null) return
        val outputDir = File(output)
        if (!outputDir.exists()) outputDir.mkdirs()
        try {
            if (termHome) {
                val homeDir = File(HOME_PATH)
                val homeTarGz = File(outputDir, "home.tar.gz")
                tarGzDirectory(homeDir, homeTarGz)
            }
            if (termUsr) {
                val usrDir = File(USR_PATH)
                val usrTarGz = File(outputDir, "usr.tar.gz")
                tarGzDirectory(usrDir, usrTarGz)
            }
            if (data) {
                val dbFile = File(dataDbHelper.database.path)
                if (dbFile.exists()) {
                    val outDb = File(outputDir, "data.db")
                    copyFile(FileInputStream(dbFile), FileOutputStream(outDb))
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "backupData error", e)
            throw RemoteException(Log.getStackTraceString(e))
        }
    }

    override fun restoreData(input: String?) {
        if (input == null) return
        val inputFile = File(input)
        if (!inputFile.exists()) {
            Log.e(TAG, "restoreData: input file does not exist: $input")
            return
        }
        val database = File(input, "data.db")
        if (database.exists()) {
            try {
                copyFile(FileInputStream(database), FileOutputStream(DataDbHelper.DATABASE_NAME))
            } catch (e: IOException) {
                Log.e(TAG, "restoreData: copy database error", e)
            }
            dataDbHelper.database.close()
            dataDbHelper.close()
            dataDbHelper = DataDbHelper(FakeContext.get())
            commandDao = CommandDao(dataDbHelper.database)
            environmentDao = EnvironmentDao(dataDbHelper.database)
        }
        val home = File(input, "home.tar.gz")
        if (home.exists()) {
            try {
                extractTarGz(home, File(HOME_PATH))
            } catch (e: IOException) {
                Log.e(TAG, "restoreData: extract tar gz error", e)
            }
        }
        val usr = File(input, "usr.tar.gz")
        if (usr.exists()) {
            try {
                extractTarGz(usr, File(USR_PATH))
            } catch (e: IOException) {
                Log.e(TAG, "restoreData: extract tar gz error", e)
            }
        }
    }

    override fun installTermExt(termExtZip: String?, callback: IInstallTermExtCallback?) {
        Thread {
            val callbackWrapper = InstallTermExtCallback(callback)
            try {
                Log.i(TAG, "install terminal extension: $termExtZip")
                callbackWrapper.onMessage(" - Install terminal extension: $termExtZip")
                val app = ZipFile(termExtZip)
                val buildPropEntry = app.getEntry("build.prop")
                val installShEntry = app.getEntry("install.sh")
                if (buildPropEntry == null) {
                    Log.e(TAG, "'build.prop' doesn't exist")
                    callbackWrapper.onMessage(" ! 'build.prop' doesn't exist")
                    callbackWrapper.onExit(false)
                    return@Thread
                }
                if (installShEntry == null) {
                    Log.e(TAG, "'install.sh' doesn't exist")
                    callbackWrapper.onMessage(" ! 'install.sh' doesn't exist")
                    callbackWrapper.onExit(false)
                    return@Thread
                }
                val buildProp = app.getInputStream(buildPropEntry)
                val termExtVersion = TermExtVersion(buildProp)
                buildProp.close()
                Log.i(
                    TAG, """
                        terminal extension:
                        version: ${termExtVersion.versionName} (${termExtVersion.versionCode})
                        abi: ${termExtVersion.abi}
                    """.trimIndent()
                )
                callbackWrapper.onMessage(
                    """
                         - Terminal extension:
                         - Version: ${termExtVersion.versionName} (${termExtVersion.versionCode})
                         - ABI: ${termExtVersion.abi}
                    """.trimIndent()
                )
                val indexOf = Build.SUPPORTED_ABIS.indexOf(termExtVersion.abi)
                if (indexOf == -1) {
                    Log.e(TAG, "unsupported ABI: ${termExtVersion.abi}")
                    callbackWrapper.onMessage(" ! Unsupported ABI: ${termExtVersion.abi}")
                    callbackWrapper.onExit(false)
                    return@Thread
                } else if (indexOf != 0) {
                    Log.w(TAG, "ABI is not preferred: ${termExtVersion.abi}")
                    callbackWrapper.onMessage(" - ABI is not preferred: ${termExtVersion.abi}")
                }
                Log.i(TAG, "unzip files.")
                callbackWrapper.onMessage(" - Unzip files.")
                val entries = app.entries()
                while (entries.hasMoreElements()) {
                    val zipEntry = entries.nextElement()
                    try {
                        if (zipEntry.isDirectory) {
                            Log.i(
                                TAG,
                                "unzip '${zipEntry.name}' to '$DATA_PATH/install_temp/${zipEntry.name}'"
                            )
                            callbackWrapper.onMessage(" - Unzip '${zipEntry.name}' to '$DATA_PATH/install_temp/${zipEntry.name}'")
                            val file = File("$DATA_PATH/install_temp/${zipEntry.name}")
                            if (!file.exists()) file.mkdirs()
                        } else {
                            val file = File("$DATA_PATH/install_temp/${zipEntry.name}")
                            Log.i(TAG, "unzip '${zipEntry.name}' to '${file.absolutePath}'")
                            callbackWrapper.onMessage(" - Unzip '${zipEntry.name}' to '${file.absolutePath}'")
                            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                            if (!file.exists()) file.createNewFile()
                            copyFile(app.getInputStream(zipEntry), FileOutputStream(file))
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "unable to unzip file: ${zipEntry.name}", e)
                        callbackWrapper.onMessage(
                            " ! Unable to unzip file: ${zipEntry.name}\n" + Log.getStackTraceString(
                                e
                            )
                        )
                        callbackWrapper.onMessage(" - Cleanup $DATA_PATH/install_temp")
                        rmRF(File("$DATA_PATH/install_temp"))
                        callbackWrapper.onExit(false)
                        return@Thread
                    }
                }
                Log.i(TAG, "complete unzipping")
                callbackWrapper.onMessage(" - Complete unzipping")
                val installScript = "$DATA_PATH/install_temp/install.sh"
                if (!File(installScript).setExecutable(true)) {
                    Log.e(TAG, "unable to set executable")
                    callbackWrapper.onMessage(" ! Unable to set executable")
                    callbackWrapper.onMessage(" - Clean up $DATA_PATH/install_temp")
                    rmRF(File("$DATA_PATH/install_temp"))
                    callbackWrapper.onExit(false)
                    return@Thread
                }
                Log.i(TAG, "execute install script")
                callbackWrapper.onMessage(" - Execute install script")
                try {
                    val process = Runtime.getRuntime().exec("/system/bin/sh")
                    val out = process.outputStream
                    out.write(("$installScript 2>&1\n").toByteArray())
                    out.flush()
                    out.close()
                    val br = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        Log.i(TAG, "output: $line")
                        callbackWrapper.onMessage(" - ScriptOuts: $line")
                    }
                    br.close()
                    val ev = process.waitFor()
                    if (ev == 0) {
                        Log.i(TAG, "exit with 0")
                        callbackWrapper.onMessage(" - Install script exit successfully")
                    } else {
                        Log.e(TAG, "exit with non-zero value $ev")
                        callbackWrapper.onMessage(" ! Install script exit with non-zero value $ev")
                        callbackWrapper.onMessage(" - Cleanup $DATA_PATH/install_temp")
                        rmRF(File("$DATA_PATH/install_temp"))
                        callbackWrapper.onExit(false)
                        return@Thread
                    }
                } catch (e: Exception) {
                    callbackWrapper.onMessage(" ! " + Log.getStackTraceString(e))
                    callbackWrapper.onMessage(" - Cleanup $DATA_PATH/install_temp")
                    rmRF(File("$DATA_PATH/install_temp"))
                    callbackWrapper.onExit(false)
                    return@Thread
                }
                callbackWrapper.onMessage(" - Cleanup $DATA_PATH/install_temp")
                rmRF(File("$DATA_PATH/install_temp"))
                Log.i(TAG, "finish")
                callbackWrapper.onMessage(" - Finish")
                callbackWrapper.onExit(true)
            } catch (e: IOException) {
                Log.e(TAG, "read terminal extension file error!", e)
                callbackWrapper.onMessage(
                    " ! Read terminal extension file error!\n" + Log.getStackTraceString(
                        e
                    )
                )
                callbackWrapper.onExit(false)
            }
        }.start()
    }

    override fun removeTermExt() {
        Log.i(TAG, "remove terminal extension")
        rmRF(File(USR_PATH))
        Log.i(TAG, "finish")
    }

    override fun getTermExtVersion(): TermExtVersion {
        val buildProp = File("$USR_PATH/build.prop")
        var result: TermExtVersion? = null
        if (buildProp.exists() && buildProp.isFile) {
            try {
                FileInputStream(buildProp).use { `in` ->
                    result = TermExtVersion(`in`)
                }
            } catch (e: IOException) {
                Log.e(TAG, "getTermExtVersion error", e)
                throw RemoteException(Log.getStackTraceString(e))
            }
        }
        return result ?: TermExtVersion("", -1, "")
    }
}
