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
import yangfentuozi.runner.server.util.ModuleManager
import yangfentuozi.runner.server.util.ProcessUtils
import yangfentuozi.runner.shared.data.ProcessInfo
import yangfentuozi.runner.shared.data.TermModuleInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.system.exitProcess

class Server : IService.Stub() {
    companion object {
        const val PROCESS_NAME = "runner_server"
        const val TAG = "ServerMain"
        const val DATA_PATH = "/data/local/tmp/runner"
        const val USR_PATH = "$DATA_PATH/usr"
        const val HOME_PATH = "$DATA_PATH/home"
        const val SHELLS_PATH = "$USR_PATH/etc/shells"
        const val SHELL_PATH = "$USR_PATH/etc/shell"
        const val LIB_PROCESS_UTILS = "$USR_PATH/lib/librunner_process.so"
        const val LIB_EXEC_UTILS = "$USR_PATH/lib/librunner_exec.so"
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

    init {
        // 设置进程名
        DdmHandleAppName.setAppName(PROCESS_NAME, Os.getuid())

        Log.i(TAG, "start")

        // 确保数据文件夹存在
        ModuleManager.init()

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
                        "$USR_PATH/bin/$name"
                    else
                        "$USR_PATH/lib/lib${name}.so"
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

            if (releaseLibFromApp("runner_process", false)) {
                // 初始化 ProcessUtils
                processUtils.loadLibrary()
            }
            if (releaseLibFromApp("runner_exec", false)) {
                // 初始化 ExecUtils
                execUtils.loadLibrary()
            }
            if (releaseLibFromApp("runner_rish", false)) {
                // 初始化 Rish
                RishConfig.setLibraryPath("$USR_PATH/lib")
                RishConfig.init()
            }
            releaseLibFromApp("rpkg", true)
            try {
                app.close()
            } catch (e: IOException) {
                Log.e(TAG, "close apk zip file error", e)
            }
        }

        // 启动 RishService
        Log.i(TAG, "start RishService")
        rishService = RishService()
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
                    errOutput("executils library not loaded")
                    exit(127)
                    return@Thread
                }
                val shell = currentShell
                if (!File(shell).exists()) {
                    Log.e(TAG, "$shell not found")
                    errOutput("$shell not found, please choose a proper shell!")
                    exit(127)
                    return@Thread
                }

                // 准备命令参数（不使用 -c，通过 stdin 传递命令）
                val argv = arrayOf(shell.substring(shell.indexOfLast { it == '/' }))

                // 创建管道用于 stdin
                val stdinPipe = ParcelFileDescriptor.createPipe()
                val stdinRead = stdinPipe[0]
                val stdinWrite = stdinPipe[1]

                // 使用 JNI 执行命令
                val pid = execUtils.exec(
                    shell,  // 可执行文件路径
                    argv,
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
                errOutput("Exception: ${e.stackTraceToString()}")
                exit(255)
            }
        }.start()
    }

    override fun getShellService(): IBinder {
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

    override fun installTermModule(
        modZip: String,
        callback: IExitCallback,
        stdout: ParcelFileDescriptor
    ) {
        ModuleManager.install(modZip, callback, stdout)
    }

    override fun uninstallTermModule(
        moduleId: String,
        callback: IExitCallback,
        stdout: ParcelFileDescriptor,
        purge: Boolean
    ) {
        ModuleManager.uninstall(moduleId, callback, stdout, purge)
    }

    override fun getTermModules(): Array<TermModuleInfo> {
        return ModuleManager.listModules().toTypedArray()
    }

    override fun getAvailableShells(): Array<out String> {
        return File(SHELLS_PATH).readLines().toTypedArray()
    }

    override fun getCurrentShell(): String {
        return Os.readlink(SHELL_PATH)
    }

    override fun setCurrentShell(shell: String) {
        File(SHELL_PATH).delete()
        Os.symlink(shell, SHELL_PATH)
    }
}
