package yangfentuozi.runner.app.ui.activity.envmanage

import android.os.Bundle
import androidx.activity.compose.setContent
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.app.ui.screens.envmanage.EnvManageScreen
import yangfentuozi.runner.app.ui.theme.RunnerTheme

class EnvManageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunnerTheme {
                EnvManageScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}