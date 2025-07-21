package yangfentuozi.runner.app.ui.activity

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import yangfentuozi.runner.app.base.BaseDialogBuilder
import yangfentuozi.runner.app.ui.dialog.ExecDialogFragment
import yangfentuozi.runner.shared.data.CommandInfo

class ExecShortcutActivity : AppCompatActivity(), BaseDialogBuilder.IsDialogShowing {

    override var isDialogShowing: Boolean
        get() = false
        set(value) {}
    val timeout: Long = 5_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.getParcelableExtra<PersistableBundle>("data")
        if (data == null) {
            Toast.makeText(this, "data is empty!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ExecDialogFragment(CommandInfo(data), waitServiceTimeout = timeout).apply {
            setOnDismissListener { finish() }
            show(supportFragmentManager, null)
        }
    }
}