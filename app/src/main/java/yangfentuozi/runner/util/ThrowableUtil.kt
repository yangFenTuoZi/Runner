package yangfentuozi.runner.util

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import yangfentuozi.runner.R

object ThrowableUtil {

    fun Throwable.getStackTraceString() : String {
        return Log.getStackTraceString(this)
    }

    fun Throwable.toErrorDialog(context: Activity) {
        getStackTraceString().toErrorDialog(context)
    }

    fun CharSequence.toErrorDialog(context: Activity) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            createDialog(context, this)
        } else {
            context.runOnUiThread { createDialog(context, this) }
        }
    }

    private fun createDialog(context: Context, errorMsg: CharSequence) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.error)
            .setMessage(errorMsg)
            .show()
    }
}