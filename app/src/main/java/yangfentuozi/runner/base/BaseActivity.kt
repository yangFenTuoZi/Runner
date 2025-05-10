package yangfentuozi.runner.base

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import rikka.material.app.MaterialActivity
import yangfentuozi.runner.App
import yangfentuozi.runner.R
import yangfentuozi.runner.util.ThemeUtil


open class BaseActivity : MaterialActivity() {

    var isDialogShowing: Boolean = false

    lateinit var mApp: App

    override fun onCreate(savedInstanceState: Bundle?) {
        mApp = application as App
        mApp.addActivity(this)
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        mApp.removeActivity(this)
        super.onDestroy()
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        if (!ThemeUtil.isSystemAccent) {
            theme.applyStyle(ThemeUtil.colorThemeStyleRes, true);
        }
        theme.applyStyle(ThemeUtil.getNightThemeStyleRes(this), true);
        theme.applyStyle(rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true)
    }

    override fun computeUserThemeKey(): String? {
        return ThemeUtil.colorTheme + ThemeUtil.getNightTheme(this)
    }

    override fun onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars()

        // 设置状态栏导航栏透明
        val window = window
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }
}