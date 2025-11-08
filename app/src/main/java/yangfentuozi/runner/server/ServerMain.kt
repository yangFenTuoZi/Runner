package yangfentuozi.runner.server

import android.ddm.DdmHandleAppName
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
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
import rikka.rish.RishConfig
import rikka.rish.RishService
import yangfentuozi.runner.BuildConfig
import yangfentuozi.runner.server.callback.IExitCallback
import yangfentuozi.runner.server.util.ExecUtils
import yangfentuozi.runner.server.util.ProcessUtils
import yangfentuozi.runner.shared.data.EnvInfo
import yangfentuozi.runner.shared.data.ProcessInfo
import yangfentuozi.runner.shared.data.TermExtVersion
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.Pattern
import java.util.zip.ZipFile
import kotlin.system.exitProcess

class ServerMain : IService.Stub() {
    companion object {
        const val TAG = "runner_server"
        const val DATA_PATH = "/data/local/tmp/runner"
        const val USR_PATH = "$DATA_PATH/usr"
        const val HOME_PATH = "$DATA_PATH/home"
        const val LIB_PROCESS_UTILS = "$HOME_PATH/.local/lib/libprocessutils.so"
        const val LIB_EXEC_UTILS = "$HOME_PATH/.local/lib/libexecutils.so"
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
    }

    private val mHandler: Handler
    private val processUtils = ProcessUtils()
    private val execUtils = ExecUtils()
    private val rishService: RishService
    private var customEnv: List<EnvInfo> = emptyList()

    init {
        // 设置进程名
        DdmHandleAppName.setAppName(TAG, Os.getuid())

        Log.i(TAG, "start")

        // 确保数据文件夹存在
        File("$HOME_PATH/.local/bin").mkdirs()
        File("$HOME_PATH/.local/lib").mkdirs()

        // 准备 Handler
        mHandler = Handler(Looper.getMainLooper())

        // 解压所需的库文件
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
            fun releaseLibFromApp(name: String, isBin: Boolean): Boolean {
                val entry = app.getEntry("lib/" + Build.SUPPORTED_ABIS[0] + "/lib${name}.so")
                return if (entry != null) {
                    Log.i(TAG, "unzip $name")
                    val out = if (isBin)
                        "$HOME_PATH/.local/bin/$name"
                    else
                        "$HOME_PATH/.local/lib/lib${name}.so"
                    val perm = if (isBin) "700" else "500"

                    try {
                        app.getInputStream(entry).use { `in` ->
                            val file = File(out)
                            if (!file.exists()) file.createNewFile()
                            FileOutputStream(file).use { fos ->
                                `in`.copyTo(fos, bufferSize = PAGE_SIZE)
                            }
                        }
                        Os.chmod(out, perm.toInt(8))
                        true
                    } catch (e: IOException) {
                        Log.e(TAG, "unzip lib${name}.so error", e)
                        false
                    } catch (e: ErrnoException) {
                        Log.e(TAG, "set permission error", e)
                        false
                    }
                } else {
                    Log.e(TAG, "lib${name}.so doesn't exist")
                    false
                }
            }

            if (releaseLibFromApp("processutils", false)) {
                // 初始化 ProcessUtils
                processUtils.loadLibrary()
            }
            if (releaseLibFromApp("executils", false)) {
                // 初始化 ExecUtils
                execUtils.loadLibrary()
            }
            if (releaseLibFromApp("rish", false)) {
                // 初始化 Rish
                RishConfig.setLibraryPath("$HOME_PATH/.local/lib")
                RishConfig.init()
            }
            try {
                app.close()
            } catch (e: IOException) {
                Log.e(TAG, "close apk zip file error", e)
            }
        }

        // 启动 RishService
        Log.i(TAG, "start RishService")
        rishService = RishService()
        updateRishServiceEnv()
    }

    override fun destroy() {
        Log.i(TAG, "stop")
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
        procName: String?,
        callback: IExitCallback?,
        stdout: ParcelFileDescriptor
    ) {
        Thread {
            fun errOutput(line: String?) {
                try {
                    callback?.errorMessage(line)
                } catch (_: RemoteException) {
                }
            }

            fun exit(code: Int) {
                try {
                    callback?.onExit(code)
                } catch (_: RemoteException) {
                }
            }

            try {
                if (!execUtils.isLibraryLoaded) {
                    Log.e(TAG, "executils library not loaded")
                    errOutput("-1")
                    errOutput("executils library not loaded")
                    exit(127)
                    return@Thread
                }
                if (!File("$USR_PATH/bin/bash").exists()) {
                    Log.e(TAG, "bash not found")
                    errOutput("-1")
                    errOutput("bash not found, may be you don't install terminal extension")
                    exit(127)
                    return@Thread
                }
                try {
                    Os.chmod("$USR_PATH/bin/bash", "700".toInt(8))
                } catch (e: ErrnoException) {
                    Log.w(TAG, "set permission error", e)
                }

                val envp = mergedEnv

                // 准备命令参数（不使用 -c，通过 stdin 传递命令）
                val argv = arrayOf(
                    "bash",
                    "--nice-name",
                    if (procName.isNullOrEmpty()) "execTask" else procName
                )

                // 创建管道用于 stdin
                val stdinPipe = ParcelFileDescriptor.createPipe()
                val stdinRead = stdinPipe[0]
                val stdinWrite = stdinPipe[1]

                // 使用 JNI 执行命令
                val pid = execUtils.exec(
                    "$USR_PATH/bin/bash",  // 可执行文件路径
                    argv,
                    envp,
                    stdinRead.detachFd(),
                    stdout.fd,
                    stdout.detachFd()
                )

                // 通过 stdin 传递命令
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(stdinWrite).use { stream ->
                        stream.write(". $USR_PATH/etc/profile; $cmd; exit\n".toByteArray())
                        stream.flush()
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "write to stdin error", e)
                }

                if (pid < 0) {
                    Log.e(TAG, "exec failed")
                    errOutput("-1")
                    errOutput("exec failed")
                    exit(127)
                    return@Thread
                }

                Log.i(TAG, "Process started with PID: $pid")

                // 等待进程结束
                val exitCode = execUtils.waitpid(pid)
                exit(exitCode)
            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceToString())
                errOutput("-1")
                errOutput("! Exception: ${e.stackTraceToString()}")
                exit(255)
            }
        }.start()
    }

    override fun syncAllData(envs: MutableList<EnvInfo>?) {
        customEnv = envs ?: emptyList()
        updateRishServiceEnv()
    }

    override fun getShellService(): IBinder? {
        return rishService
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
                val isGroup = if (pid[i] < 0) " group" else ""
                if (pid[i] <= 1 && pid[i] >= -1) {
                    Log.w(TAG, "skip killing process$isGroup: ${pid[i]}")
                    true
                } else {
                    Log.i(TAG, "kill process$isGroup: ${pid[i]}")
                    processUtils.sendSignal(pid[i], signal)
                }
            }
        } else {
            Log.e(TAG, "process utils library not loaded")
            null
        }
    }

    override fun backupData(output: String?, termHome: Boolean, termUsr: Boolean) {
        if (output == null) return
        val outputDir = File(output)
        if (!outputDir.exists()) outputDir.mkdirs()
        try {
            if (termHome) {
                val homeDir = File(HOME_PATH)
                if (homeDir.exists()) {
                    val homeTarGz = File(outputDir, "home.tar.gz")
                    tarGzDirectory(homeDir, homeTarGz)
                }
            }
            if (termUsr) {
                val usrDir = File(USR_PATH)
                if (usrDir.exists()) {
                    val usrTarGz = File(outputDir, "usr.tar.gz")
                    tarGzDirectory(usrDir, usrTarGz)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "backupData error", e)
            throw RemoteException(e.stackTraceToString())
        }
    }

    override fun restoreData(input: String?) {
        if (input == null) return
        val inputDir = File(input)
        if (!inputDir.exists()) {
            Log.e(TAG, "restoreData: input directory does not exist: $input")
            return
        }
        try {
            val homeTarGz = File(inputDir, "home.tar.gz")
            if (homeTarGz.exists()) {
                File(HOME_PATH).deleteRecursively()
                extractTarGz(homeTarGz, File(HOME_PATH))
            }
            val usrTarGz = File(inputDir, "usr.tar.gz")
            if (usrTarGz.exists()) {
                File(USR_PATH).deleteRecursively()
                extractTarGz(usrTarGz, File(USR_PATH))
            }
        } catch (e: IOException) {
            Log.e(TAG, "restoreData error", e)
            throw RemoteException(e.stackTraceToString())
        }
    }

    override fun installTermExt(
        termExtZip: String?,
        callback: IExitCallback?,
        stdout: ParcelFileDescriptor
    ) {
        Thread {
            val writer = ParcelFileDescriptor.AutoCloseOutputStream(stdout).bufferedWriter()
            fun writeOutput(line: String?) {
                try {
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "write output error", e)
                }
            }

            fun exit(isSuccessful: Boolean) {
                try {
                    writer.close()
                } catch (e: IOException) {
                    Log.e(TAG, "close writer error", e)
                }
                try {
                    callback?.onExit(if (isSuccessful) 0 else 1)
                } catch (_: RemoteException) {
                }
            }
            try {
                Log.i(TAG, "install terminal extension: $termExtZip")
                writeOutput("- Install terminal extension: $termExtZip")
                val app = ZipFile(termExtZip)
                val buildPropEntry = app.getEntry("build.prop")
                val installShEntry = app.getEntry("install.sh")
                if (buildPropEntry == null) {
                    Log.e(TAG, "'build.prop' doesn't exist")
                    writeOutput(" ! 'build.prop' doesn't exist")
                    exit(false)
                    return@Thread
                }
                if (installShEntry == null) {
                    Log.e(TAG, "'install.sh' doesn't exist")
                    writeOutput(" ! 'install.sh' doesn't exist")
                    exit(false)
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
                writeOutput(
                    """
                       - Terminal extension:
                       - Version: ${termExtVersion.versionName} (${termExtVersion.versionCode})
                       - ABI: ${termExtVersion.abi}
                    """.trimIndent()
                )
                val indexOf = Build.SUPPORTED_ABIS.indexOf(termExtVersion.abi)
                if (indexOf == -1) {
                    Log.e(TAG, "unsupported ABI: ${termExtVersion.abi}")
                    writeOutput("! Unsupported ABI: ${termExtVersion.abi}")
                    exit(false)
                    return@Thread
                } else if (indexOf != 0) {
                    Log.w(TAG, "ABI is not preferred: ${termExtVersion.abi}")
                    writeOutput("- ABI is not preferred: ${termExtVersion.abi}")
                }
                fun cleanupAndReturn(isSuccessful: Boolean) {
                    writeOutput("- Cleanup $DATA_PATH/install_temp")
                    File("$DATA_PATH/install_temp").deleteRecursively()
                    exit(isSuccessful)
                }
                Log.i(TAG, "unzip files.")
                writeOutput("- Unzip files.")
                val entries = app.entries()
                while (entries.hasMoreElements()) {
                    val zipEntry = entries.nextElement()
                    try {
                        if (zipEntry.isDirectory) {
                            Log.i(
                                TAG,
                                "unzip '${zipEntry.name}' to '$DATA_PATH/install_temp/${zipEntry.name}'"
                            )
                            writeOutput("- Unzip '${zipEntry.name}' to '$DATA_PATH/install_temp/${zipEntry.name}'")
                            val file = File("$DATA_PATH/install_temp/${zipEntry.name}")
                            if (!file.exists()) file.mkdirs()
                        } else {
                            val file = File("$DATA_PATH/install_temp/${zipEntry.name}")
                            Log.i(TAG, "unzip '${zipEntry.name}' to '${file.absolutePath}'")
                            writeOutput("- Unzip '${zipEntry.name}' to '${file.absolutePath}'")
                            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                            if (!file.exists()) file.createNewFile()
                            app.getInputStream(zipEntry).use { input ->
                                FileOutputStream(file).use {
                                    input.copyTo(it, bufferSize = PAGE_SIZE)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "unable to unzip file: ${zipEntry.name}", e)
                        writeOutput(
                            "! Unable to unzip file: ${zipEntry.name}\n${e.stackTraceToString()}"
                        )
                        cleanupAndReturn(false)
                        return@Thread
                    }
                }
                Log.i(TAG, "complete unzipping")
                writeOutput("- Complete unzipping")
                val installScript = "$DATA_PATH/install_temp/install.sh"
                if (!File(installScript).setExecutable(true)) {
                    Log.e(TAG, "unable to set executable")
                    writeOutput("! Unable to set executable")
                    cleanupAndReturn(false)
                    return@Thread
                }
                Log.i(TAG, "execute install script")
                writeOutput("- Execute install script")
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
                        writeOutput("- ScriptOuts: $line")
                    }
                    br.close()
                    val ev = process.waitFor()
                    if (ev == 0) {
                        Log.i(TAG, "exit with 0")
                        writeOutput("- Install script exit successfully")
                    } else {
                        Log.e(TAG, "exit with non-zero value $ev")
                        writeOutput("! Install script exit with non-zero value $ev")
                        cleanupAndReturn(false)
                        return@Thread
                    }
                } catch (e: Exception) {
                    writeOutput("! ${e.stackTraceToString()}")
                    cleanupAndReturn(false)
                    return@Thread
                }
                writeOutput("- Finish")
                Log.i(TAG, "finish")
                cleanupAndReturn(true)
                return@Thread
            } catch (e: IOException) {
                Log.e(TAG, "read terminal extension file error!", e)
                writeOutput(
                    "! Read terminal extension file error!\n${e.stackTraceToString()}"
                )
                exit(false)
                return@Thread
            }
        }.start()
    }

    override fun removeTermExt() {
        Log.i(TAG, "remove terminal extension")
        File(USR_PATH).deleteRecursively()
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
                throw RemoteException(e.stackTraceToString())
            }
        }
        return result ?: TermExtVersion("", -1, "")
    }

    val mergedEnv: Array<String>
        get() {
            // 准备环境变量
            val envMap = mutableMapOf<String, String>()
            envMap["PREFIX"] = USR_PATH
            envMap["HOME"] = HOME_PATH
            envMap["TMPDIR"] = "$USR_PATH/tmp"
            envMap["PATH"] = "$HOME_PATH/.local/bin:$USR_PATH/bin:$USR_PATH/bin/applets"
            envMap["LD_LIBRARY_PATH"] = "$HOME_PATH/.local/lib:$USR_PATH/lib"

            // 添加自定义环境变量
            for (entry in customEnv) {

                // 跳过未启用的
                if (!entry.enabled) continue

                entry.key?.let { key ->
                    entry.value?.let { value ->
                        val oldValue = envMap[key]
                        envMap[key] = if (oldValue != null) {
                            value.replace(
                                Regex("\\$(${Pattern.quote(key)}|\\{${Pattern.quote(key)}\\})"),
                                oldValue
                            )
                        } else {
                            value
                        }
                    }
                }
            }

            return envMap.map { "${it.key}=${it.value}" }.toTypedArray()
        }

    fun updateRishServiceEnv() {
        rishService.updateEnv(mergedEnv)
    }
}
