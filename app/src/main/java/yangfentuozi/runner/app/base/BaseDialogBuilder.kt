package yangfentuozi.runner.app.base

import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class BaseDialogBuilder @Throws(DialogShowingException::class) constructor(context: BaseActivity) : MaterialAlertDialogBuilder(context) {
    class DialogShowingException : Exception()

    private val mBaseActivity: BaseActivity = context
    private var mAlertDialog: AlertDialog? = null
    private var mOnDismissListener: DialogInterface.OnDismissListener? = null

    init {
        if (mBaseActivity.isDialogShowing) throw DialogShowingException()
        mBaseActivity.isDialogShowing = true
        super.setOnDismissListener { dialogInterface ->
            mBaseActivity.isDialogShowing = false
            mOnDismissListener?.onDismiss(dialogInterface)
        }
    }

    fun getAlertDialog(): AlertDialog? = mAlertDialog

    
    override fun create(): AlertDialog {
        return super.create().also { mAlertDialog = it }
    }

    
    override fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?): MaterialAlertDialogBuilder {
        mOnDismissListener = onDismissListener
        return this
    }

    fun runOnMainThread(action: Runnable) {
        mBaseActivity.runOnMainThread(action)
    }

    
    fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String {
        return mBaseActivity.getString(resId, *formatArgs)
    }

    
    fun getString(@StringRes resId: Int): String {
        return mBaseActivity.getString(resId)
    }
}