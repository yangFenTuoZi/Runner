package yangfentuozi.runner.app.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.server.ServerMain
import yangfentuozi.runner.server.callback.IExitCallback
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class InstallTermExtViewModel(application: Application) : AndroidViewModel(application) {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val isStopped = AtomicBoolean(false)
    private val termExtCacheDir = File(application.externalCacheDir, "termExtCache")

    fun startInstallation(uri: Uri?, onShowToastRes: (Int) -> Unit) {
        if (uri == null) {
            appendOutput("! Invalid intent or file\n")
            _hasError.value = true
            return
        }

        // 在启动协程之前就设置安装状态，确保 BackHandler 能立即生效
        _isInstalling.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // 复制文件到缓存
            val cacheFile = try {
                val input = getApplication<Application>().contentResolver.openInputStream(uri)
                if (input == null) {
                    appendOutput("! Failed to open file\n")
                    _hasError.value = true
                    _isInstalling.value = false
                    return@launch
                }

                // 清理并创建缓存目录
                termExtCacheDir.deleteRecursively()
                termExtCacheDir.mkdirs()

                val file = File(termExtCacheDir, "termux_ext.zip")
                input.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream, bufferSize = ServerMain.PAGE_SIZE)
                    }
                }
                appendOutput("- File copied to cache: ${file.absolutePath}\n")
                file
            } catch (e: FileNotFoundException) {
                appendOutput("! File not found:\n${e.message}\n")
                _hasError.value = true
                _isInstalling.value = false
                return@launch
            } catch (e: IOException) {
                appendOutput("! Failed to copy file:\n${e.message}\n")
                _hasError.value = true
                _isInstalling.value = false
                return@launch
            }

            // 检查服务
            if (!Runner.pingServer()) {
                appendOutput("! Service not running\n")
                _hasError.value = true
                _isInstalling.value = false
                return@launch
            }

            val service = Runner.service
            if (service == null) {
                appendOutput("! Service not available\n")
                _hasError.value = true
                _isInstalling.value = false
                return@launch
            }

            // 开始安装（状态已经在函数开始时设置为 true）
            appendOutput("- Starting installation...\n")

            var pipe: Array<ParcelFileDescriptor>? = null
            try {
                pipe = ParcelFileDescriptor.createPipe()
                val readPipe = pipe[0]
                val writePipe = pipe[1]

                val exitCallback = object : IExitCallback.Stub() {
                    override fun onExit(exitValue: Int) {
                        if (isStopped.get()) return

                        // 延迟确保所有输出都已读取
                        Thread.sleep(200)

                        val message = if (exitValue == 0) {
                            "- Installation successful\n"
                        } else {
                            "! Installation failed (exit code: $exitValue)\n"
                        }
                        appendOutput(message)
                        appendOutput("- Cleanup temp: ${termExtCacheDir.absolutePath}\n")

                        _isInstalling.value = false

                        // 清理缓存
                        try {
                            termExtCacheDir.deleteRecursively()
                        } catch (_: Exception) {
                        }
                    }

                    override fun errorMessage(message: String?) {
                        if (message != null) {
                            appendOutput("$message\n")
                        }
                    }
                }

                // 启动安装
                service.installTermExt(cacheFile.absolutePath, exitCallback, writePipe)

                // 读取输出
                ParcelFileDescriptor.AutoCloseInputStream(readPipe).bufferedReader().use { reader ->
                    while (!isStopped.get()) {
                        val line = reader.readLine() ?: break
                        appendOutput("$line\n")
                    }
                }
            } catch (e: IOException) {
                if (!isStopped.get()) {
                    appendOutput("! Pipe read error: ${e.message}\n")
                    _hasError.value = true
                    _isInstalling.value = false
                }
            } finally {
                // 关闭管道
                pipe?.let { pipes ->
                    try {
                        pipes[0].close()
                    } catch (_: IOException) {
                    }
                    try {
                        pipes[1].close()
                    } catch (_: IOException) {
                    }
                }
            }
        }
    }

    private fun appendOutput(text: String) {
        _output.value += text
    }

    fun cleanup() {
        isStopped.set(true)
        // 清理缓存目录（在后台线程中执行）
        viewModelScope.launch(Dispatchers.IO) {
            try {
                termExtCacheDir.deleteRecursively()
            } catch (_: Exception) {
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}

