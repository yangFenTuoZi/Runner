package yangfentuozi.runner.ui.dialog

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Build
import android.view.SurfaceControl
import android.view.SurfaceControl.Transaction
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import yangfentuozi.runner.base.BaseActivity
import yangfentuozi.runner.base.BaseDialogBuilder
import java.util.function.Consumer

class BlurBehindDialogBuilder(context: BaseActivity) : BaseDialogBuilder(context) {
    override fun create(): AlertDialog {
        val dialog = super.create()
        setupWindowBlurListener(dialog)
        return dialog
    }

    private fun setupWindowBlurListener(dialog: AlertDialog) {
        val window = dialog.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window!!.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val windowBlurEnabledListener = Consumer { enabled: Boolean? ->
                updateWindowForBlurs(
                    window, enabled!!
                )
            }
            window.decorView.addOnAttachStateChangeListener(
                object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        window.windowManager.addCrossWindowBlurEnabledListener(
                            windowBlurEnabledListener
                        )
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        window.windowManager.removeCrossWindowBlurEnabledListener(
                            windowBlurEnabledListener
                        )
                    }
                })
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            dialog.setOnShowListener(OnShowListener { d: DialogInterface? ->
                updateWindowForBlurs(
                    window!!, supportBlur
                )
            })
        }
    }

    private fun updateWindowForBlurs(window: Window, blursEnabled: Boolean) {
        val mDimAmountWithBlur = 0.1f
        val mDimAmountNoBlur = 0.32f
        window.setDimAmount(if (blursEnabled) mDimAmountWithBlur else mDimAmountNoBlur)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.attributes.blurBehindRadius = 20
            window.setAttributes(window.attributes)
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            if (blursEnabled) {
                val view = window.decorView
                val animator = ValueAnimator.ofInt(1, 53)
                animator.interpolator = DecelerateInterpolator()
                try {
                    val viewRootImpl = view.javaClass.getMethod("getViewRootImpl").invoke(view)
                    if (viewRootImpl == null) {
                        return
                    }
                    val surfaceControl = viewRootImpl.javaClass.getMethod("getSurfaceControl")
                        .invoke(viewRootImpl) as SurfaceControl?

                    @SuppressLint("BlockedPrivateApi") val setBackgroundBlurRadius =
                        Transaction::class.java.getDeclaredMethod(
                            "setBackgroundBlurRadius",
                            SurfaceControl::class.java,
                            Int::class.javaPrimitiveType
                        )
                    animator.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator? ->
                        try {
                            val transaction = Transaction()
                            val animatedValue = animation!!.getAnimatedValue()
                            if (animatedValue != null) {
                                setBackgroundBlurRadius.invoke(
                                    transaction,
                                    surfaceControl,
                                    animatedValue as Int
                                )
                            }
                            transaction.apply()
                        } catch (_: Throwable) {
                        }
                    })
                } catch (_: Throwable) {
                }
                view.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        animator.cancel()
                    }
                })
                animator.start()
            }
        }
    }

    companion object {
        // private static final boolean supportBlur = SystemProperties.getBoolean("ro.surface_flinger.supports_background_blur", false) && !SystemProperties.getBoolean("persist.sys.sf.disable_blurs", false);
        private fun getSystemPropertyBoolean(key: String?): Boolean {
            try {
                val systemProperties = Class.forName("android.os.SystemProperties")
                val getBoolean = systemProperties.getMethod(
                    "getBoolean",
                    String::class.java,
                    Boolean::class.javaPrimitiveType
                )
                var result = getBoolean.invoke(null, key, false) as Boolean?
                if (result == null) result = false
                return result
            } catch (_: Exception) {
                return false
            }
        }

        private val supportBlur =
            getSystemPropertyBoolean("ro.surface_flinger.supports_background_blur") &&
                    !getSystemPropertyBoolean("persist.sys.sf.disable_blurs")
    }
}