package yangfentuozi.runner.app.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.app.ui.screens.InstallTermExtScreen
import yangfentuozi.runner.app.ui.theme.RunnerTheme

class InstallTermExtActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }

        setContent {
            RunnerTheme {
                InstallTermExtScreen(
                    uri = uri,
                    externalCacheDir = externalCacheDir,
                    onNavigateBack = { finish() },
                    onShowToast = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    },
                    onShowToastRes = { resId ->
                        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}