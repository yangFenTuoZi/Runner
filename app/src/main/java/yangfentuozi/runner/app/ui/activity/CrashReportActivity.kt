package yangfentuozi.runner.app.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.app.ui.screens.crashreport.CrashReportScreen
import yangfentuozi.runner.app.ui.theme.RunnerTheme

class CrashReportActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashFile = intent.getStringExtra("crash_file")
        val crashInfo = intent.getStringExtra("crash_info")

        setContent {
            RunnerTheme {
                CrashReportScreen(
                    crashFile = crashFile,
                    crashInfo = crashInfo,
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mApp.finishApp()
    }
}