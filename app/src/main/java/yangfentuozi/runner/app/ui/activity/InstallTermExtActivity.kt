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
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class InstallTermExtActivity : BaseActivity() {
    private lateinit var binding: ActivityStreamActivityBinding
    private var callback: IExitCallback? = null
    private var pipe: Array<ParcelFileDescriptor>? = null
    private var readThread: Thread? = null
    private lateinit var termExtCacheDir: File
    private var breakRead = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        termExtCacheDir = File(externalCacheDir, "termExtCache")

        binding = ActivityStreamActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.appBar.setLiftable(true)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        binding.text1.typeface = Typeface.createFromAsset(assets, "Mono.ttf")

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_VIEW == action && type != null) {
            val uri = intent.data
            if (uri != null)
                handleReceivedFile(uri)
            else {
                onMessage("! Invalid file")
                return
            }
        } else if (Intent.ACTION_SEND == action && type != null) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null)
                handleReceivedFile(uri)
            else {
                onMessage("! Invalid file")
                return
            }
        } else {
            onMessage("! Invalid intent")
            return
        }
    }

    private fun handleReceivedFile(uri: Uri) {
        var input: InputStream?
        try {
            input = contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            onMessage("! File not found:\n" + e.stackTraceToString())
            return
        }
        if (input == null) {
            onMessage("! Failed to open file")
            return
        }
        termExtCacheDir.deleteRecursively()
        termExtCacheDir.mkdirs()
        val file = File(termExtCacheDir, "termux_ext.zip")
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val output = FileOutputStream(file)
            input.copyTo(output, bufferSize = ServerMain.PAGE_SIZE)
            input.close()
            output.close()
        } catch (_: IOException) {
            onMessage("! Failed to copy file")
            return
        }

        if (!Runner.pingServer()) {
            showErrAndFinish(R.string.service_not_running)
            finish()
        }

        callback = object : IExitCallback.Stub() {

            override fun onExit(exitValue: Int) {
                Thread {
                    Thread.sleep(100)
                    stopAndClean()
                    onMessage(if (exitValue == 0) "- Installation successful" else "! Installation failed")
                    onMessage("\n- Cleanup temp: ${termExtCacheDir.absolutePath}")
                }.start()
            }
        }

        pipe = ParcelFileDescriptor.createPipe()

        readThread = Thread {
            try {
                val reader = ParcelFileDescriptor.AutoCloseInputStream(pipe?.get(0)).bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null && !breakRead) {
                    onMessage(line)
                }
            } catch (e: IOException) {
                if (breakRead) return@Thread
                onMessage("! Pipe read error:\n" + e.stackTraceToString())
            } finally {
                try {
                    pipe?.get(0)?.close()
                    pipe?.get(1)?.close()
                } catch (_: IOException) {
                }
            }
        }.apply { start() }

        Runner.service?.installTermExt(file.absolutePath, callback, pipe!![1])
    }

    private fun onMessage(message: String?) {
        runOnMainThread {
            binding.text1.append(message + "\n")
            binding.scrollView.post {
                binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
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
        stopAndClean()
    }

    fun stopAndClean() {
        breakRead = true
        callback = null
        Thread.sleep(100)
        readThread?.interrupt()
        pipe?.get(0)?.close()
        pipe?.get(1)?.close()
        termExtCacheDir.deleteRecursively()
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