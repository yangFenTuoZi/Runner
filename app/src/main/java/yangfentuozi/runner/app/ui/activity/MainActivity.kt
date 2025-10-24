package yangfentuozi.runner.app.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.app.ui.MainScreen
import yangfentuozi.runner.app.ui.theme.RunnerTheme

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RunnerTheme {
                MainScreen(
                    activity = this
                )
            }
        }
    }
}
