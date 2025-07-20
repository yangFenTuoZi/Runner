package yangfentuozi.runner.app.ui.activity

import android.app.Activity
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import yangfentuozi.runner.app.ui.dialog.ExecDialogBuilder
import yangfentuozi.runner.shared.data.CommandInfo

class ExecShortcutActivity: Activity() {

    val timeout: Long = 20_000L

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        val data = intent.getBundleExtra("data")
        if (data == null) {
            Toast.makeText(this, "data is empty!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ExecDialogBuilder(this, CommandInfo(data), timeout).apply {
            setOnDismissListener { finish() }
            show()
        }
    }
}