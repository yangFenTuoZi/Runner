package yangfentuozi.runner.app.base

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class BaseDialogBuilder @Throws(DialogShowingException::class) constructor(private val mContext: Context) : MaterialAlertDialogBuilder(mContext) {
    class DialogShowingException : Exception()

    interface IsDialogShowing {
        var isDialogShowing: Boolean
    }

    private var mAlertDialog: AlertDialog? = null
    private var mOnDismissListener: DialogInterface.OnDismissListener? = null
    private var isDialogShowing: Boolean
        get() = (mContext as? IsDialogShowing)?.isDialogShowing ?: false
        set(value) = (mContext as? IsDialogShowing)?.isDialogShowing = value

    init {
        if (isDialogShowing) throw DialogShowingException()
        isDialogShowing = true
        super.setOnDismissListener { dialogInterface ->
            isDialogShowing = false
            mOnDismissListener?.onDismiss(dialogInterface)
        }
    }

    val alertDialog: AlertDialog?
        get() = mAlertDialog

    override fun create(): AlertDialog {
        return super.create().also { mAlertDialog = it }
    }

    override fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?): MaterialAlertDialogBuilder {
        mOnDismissListener = onDismissListener
        return this
    }
}
