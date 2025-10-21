package yangfentuozi.runner.app.ui.activity

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import yangfentuozi.runner.app.ui.dialogs.ExecDialog
import yangfentuozi.runner.app.ui.theme.RunnerTheme
import yangfentuozi.runner.shared.data.CommandInfo

class ExecShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.getParcelableExtra<PersistableBundle>("data")
        if (data == null) {
            Toast.makeText(this, "data is empty!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val commandInfo = CommandInfo(data)
        
        setContent {
            RunnerTheme {
                ExecDialog(
                    command = commandInfo,
                    onDismiss = { finish() }
                )
            }
        }
    }
}