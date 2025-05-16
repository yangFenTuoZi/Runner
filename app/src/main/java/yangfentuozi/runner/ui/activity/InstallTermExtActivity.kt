package yangfentuozi.runner.ui.activity

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.Toast
import androidx.annotation.StringRes
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseActivity
import yangfentuozi.runner.databinding.ActivityStreamActivityBinding
import yangfentuozi.runner.service.ServiceImpl
import yangfentuozi.runner.service.callback.IInstallTermExtCallback
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class InstallTermExtActivity : BaseActivity() {
    private lateinit var binding: ActivityStreamActivityBinding
    private var callback: IInstallTermExtCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStreamActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.appBar.setLiftable(true)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        binding.text1.typeface = Typeface.MONOSPACE

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_VIEW == action && type != null) {
            val uri = intent.data
            if (uri != null)
                handleReceivedFile(uri)
            else {
                onMessage(" ! Invalid file")
                return
            }
        } else if (Intent.ACTION_SEND == action && type != null) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null)
                handleReceivedFile(uri)
            else {
                onMessage(" ! Invalid file")
                return
            }
        } else {
            onMessage(" ! Invalid intent")
            return
        }
    }

    private fun handleReceivedFile(uri: Uri) {
        var input: InputStream? = null
        try {
            input = contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            onMessage(" ! File not found:\n" + e.stackTraceToString())
            return
        }
        if (input == null) {
            onMessage(" ! Failed to open file")
            return
        }
        val termExtCacheDir = getExternalFilesDir("termExtCache")
        ServiceImpl.rmRF(termExtCacheDir)
        termExtCacheDir?.mkdirs()
        val file = File(termExtCacheDir, "termux_ext.zip");
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            ServiceImpl.copyFile(input, FileOutputStream(file))
        } catch (_: IOException) {
            onMessage(" ! Failed to copy file")
            return
        }

        if (!Runner.pingServer()) {
            showErrAndFinish(R.string.service_not_running)
            finish()
        }

        callback = object : IInstallTermExtCallback.Stub() {
            override fun onMessage(message: String?) {
                this@InstallTermExtActivity.onMessage(message)
            }

            override fun onExit(isSuccessful: Boolean) {
                onMessage(if (isSuccessful) " - Installation successful" else " ! Installation failed")
                onMessage("\n - Cleanup temp: ${termExtCacheDir?.absolutePath}")
                ServiceImpl.rmRF(termExtCacheDir)
                callback = null
            }
        }

        Runner.service?.installTermExt(file.absolutePath, callback)
    }

    private fun onMessage(message: String?) {
        runOnUiThread {
            binding.text1.append(message + "\n")
            binding.scrollView.post {
                binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun showErrAndFinish(@StringRes resId: Int) {
        runOnUiThread {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
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