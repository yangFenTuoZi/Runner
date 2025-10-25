package yangfentuozi.runner.app.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.app.ui.screens.installtermext.InstallTermExtScreen
import yangfentuozi.runner.app.ui.theme.RunnerTheme
import yangfentuozi.runner.app.ui.viewmodels.InstallTermExtViewModel

class InstallTermExtActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }

        setContent {
            val viewModel: InstallTermExtViewModel = viewModel()
            val isInstalling by viewModel.isInstalling.collectAsState()

            // 在安装进行时屏蔽返回操作
            BackHandler(enabled = isInstalling) {
                // 安装进行中时不处理返回操作
            }

            RunnerTheme {
                InstallTermExtScreen(
                    uri = uri,
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onShowToastRes = { resId ->
                        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}