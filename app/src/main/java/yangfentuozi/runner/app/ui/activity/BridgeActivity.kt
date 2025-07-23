package yangfentuozi.runner.app.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle

class BridgeActivity : Activity() {
    companion object {
        @SuppressLint("DiscouragedPrivateApi")
        fun Intent.setExtras(extras: Bundle?) {
            try {
                val mExtrasField = Intent::class.java.getDeclaredField("mExtras")
                mExtrasField.isAccessible = true
                mExtrasField.set(this, extras)
            } catch (_: NoSuchFieldException) {
            } catch (_: IllegalAccessException) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
        startActivity(Intent().setClass(this, ExecShortcutActivity::class.java).apply {
            setExtras(intent.extras)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}