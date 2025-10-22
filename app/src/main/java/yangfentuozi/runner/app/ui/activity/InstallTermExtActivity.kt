package yangfentuozi.runner.app.ui.activity

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.Toast
import androidx.annotation.StringRes
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.databinding.ActivityStreamActivityBinding
import yangfentuozi.runner.server.ServerMain
import yangfentuozi.runner.server.callback.IExitCallback
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class InstallTermExtActivity : BaseActivity() {
    private lateinit var binding: ActivityStreamActivityBinding
    private var callback: IExitCallback? = null
    private var pipe: Array<ParcelFileDescriptor>? = null
    private var readThread: Thread? = null
    private lateinit var termExtCacheDir: File
    private val isStopped = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        termExtCacheDir = File(externalCacheDir, "termExtCache")

        binding = ActivityStreamActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        binding.text1.typeface = Typeface.createFromAsset(assets, "Mono.ttf")

        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri != null) {
            handleReceivedFile(uri)
        } else {
            showErrorAndFinish("! Invalid intent or file")
        }
    }

    private fun setupToolbar() {
        binding.appBar.setLiftable(true)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun handleReceivedFile(uri: Uri) {
        val cacheFile = copyUriToCache(uri) ?: return

        if (!Runner.pingServer()) {
            showErrAndFinish(R.string.service_not_running)
            return
        }

        setupInstallation(cacheFile)
    }

    private fun copyUriToCache(uri: Uri): File? {
        try {
            val input = contentResolver.openInputStream(uri)
            if (input == null) {
                showErrorAndFinish("! Failed to open file")
                return null
            }

            // 清理并创建缓存目录
            termExtCacheDir.deleteRecursively()
            termExtCacheDir.mkdirs()

            val cacheFile = File(termExtCacheDir, "termux_ext.zip")
            input.use { inputStream ->
                cacheFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream, bufferSize = ServerMain.PAGE_SIZE)
                }
            }
            return cacheFile
        } catch (e: FileNotFoundException) {
            showErrorAndFinish("! File not found:\n${e.message}")
            return null
        } catch (e: IOException) {
            showErrorAndFinish("! Failed to copy file:\n${e.message}")
            return null
        }
    }

    private fun setupInstallation(cacheFile: File) {
        callback = createExitCallback()
        pipe = ParcelFileDescriptor.createPipe()

        readThread = Thread {
            try {
                pipe?.get(0)?.let { readPipe ->
                    ParcelFileDescriptor.AutoCloseInputStream(readPipe).bufferedReader().use { reader ->
                        while (!isStopped.get()) {
                            val line = reader.readLine() ?: break
                            onMessage(line)
                        }
                    }
                }
            } catch (e: IOException) {
                if (!isStopped.get()) {
                    onMessage("! Pipe read error: ${e.message}")
                }
            }
        }.apply { start() }
        
        val service = Runner.service
        if (service == null) {
            showErrAndFinish(R.string.service_not_running)
            return
        }
        
        pipe?.get(1)?.let { writePipe ->
            service.installTermExt(cacheFile.absolutePath, callback, writePipe)
        }
    }

    private fun createExitCallback() = object : IExitCallback.Stub() {
        override fun onExit(exitValue: Int) {
            if (isStopped.get()) return

            // 延迟，确保所有输出都已读取
            binding.root.postDelayed({
                val message = if (exitValue == 0) {
                    "- Installation successful"
                } else {
                    "! Installation failed (exit code: $exitValue)"
                }
                onMessage(message)
                onMessage("- Cleanup temp: ${termExtCacheDir.absolutePath}")
                cleanup()
            }, 200)
        }

        override fun errorMessage(message: String?) {
        }
    }

    private fun onMessage(message: String?) {
        if (message == null) return
        runOnMainThread {
            binding.text1.append("$message\n")
            binding.scrollView.post {
                binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun showErrorAndFinish(message: String) {
        runOnMainThread {
            onMessage(message)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showErrAndFinish(@StringRes resId: Int) {
        runOnMainThread {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        if (!isStopped.compareAndSet(false, true)) {
            return // 已经清理过了
        }

        callback = null
        
        // 中断读取线程
        readThread?.interrupt()
        
        // 安全地关闭管道
        closePipeSafely()
        
        // 清理缓存目录（在后台线程中执行）
        Thread {
            try {
                termExtCacheDir.deleteRecursively()
            } catch (e: Exception) {
                // 忽略清理错误
            }
        }.start()
    }

    private fun closePipeSafely() {
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
        pipe = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}