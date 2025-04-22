package yangFenTuoZi.runner.plus.utils

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.util.Log.getStackTraceString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import yangFenTuoZi.runner.plus.R

object ExceptionUtils {

    fun Throwable.toErrorDialog(context: Activity) {
        getStackTraceString(this).toErrorDialog(context)
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