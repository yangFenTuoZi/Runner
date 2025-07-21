package yangfentuozi.runner.app.base

import android.content.res.Resources
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import rikka.material.app.MaterialActivity
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.util.ThemeUtil


open class BaseActivity() : MaterialActivity(),
    BaseDialogBuilder.IsDialogShowing {

    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val mUiThread: Thread = Thread.currentThread()
    override var isDialogShowing: Boolean = false

    lateinit var mApp: App

    override fun onCreate(savedInstanceState: Bundle?) {
        mApp = application as App
        mApp.addActivity(this)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        mApp.removeActivity(this)
        super.onDestroy()
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        if (!ThemeUtil.isSystemAccent) {
            theme.applyStyle(ThemeUtil.colorThemeStyleRes, true)
        }
        theme.applyStyle(ThemeUtil.getNightThemeStyleRes(this), true)
    }

    override fun computeUserThemeKey(): String? {
        return ThemeUtil.getNightTheme(this) + ThemeUtil.isSystemAccent
    }

    override fun onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars()
        // 设置状态栏导航栏透明
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT)
        )
    }

    fun runOnMainThread(action: Runnable) {
        if (Thread.currentThread() !== mUiThread) {
            mHandler.post(action)
        } else {
            action.run()
        }
    }
}
