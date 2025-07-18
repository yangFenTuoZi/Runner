package yangfentuozi.runner.app.ui.activity

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.SystemProperties
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.MenuItem
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.app.util.ThrowableUtil.toErrorDialog
import yangfentuozi.runner.databinding.ActivityCrashReportBinding
import java.io.FileOutputStream
import java.io.IOException

class CrashReportActivity : BaseActivity() {

    private lateinit var binding: ActivityCrashReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrashReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBar.setLiftable(true)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        val crashFile = intent.getStringExtra("crash_file")
        val crashInfo = intent.getStringExtra("crash_info")

        binding.crashFile.text = crashFile

        val crashInfoTextView = binding.crashInfo.apply {
            append(getBoldText("VERSION.RELEASE: "))
            append(Build.VERSION.RELEASE)
            append("\n")

            append(getBoldText("VERSION.SDK_INT: "))
            append(Build.VERSION.SDK_INT.toString())
            append("\n")

            append(getBoldText("BUILD_TYPE: "))
            append(Build.TYPE)
            append("\n")

            append(getBoldText("CPU_ABI: "))
            append(SystemProperties.get("ro.product.cpu.abi"))
            append("\n")

            append(getBoldText("CPU_SUPPORTED_ABIS: "))
            append(Build.SUPPORTED_ABIS.contentToString())
            append("\n\n$crashInfo")
        }

        try {
            FileOutputStream(crashFile).use { out ->
                out.write(crashInfoTextView.text.toString().toByteArray())
            }
        } catch (e: IOException) {
            e.toErrorDialog(this)
        }
    }

    private fun getBoldText(text: String): CharSequence {
        return SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mApp.finishApp()
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