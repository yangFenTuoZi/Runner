package yangFenTuoZi.runner.plus.base

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import com.google.android.material.color.DynamicColors
import rikka.material.app.MaterialActivity
import yangFenTuoZi.runner.plus.App
import yangFenTuoZi.runner.plus.utils.ThemeUtils

open class BaseActivity : MaterialActivity() {

    var isDialogShow: Boolean = false

    var isDark: Int = -1
    lateinit var mApp: App

    override fun onCreate(savedInstanceState: Bundle?) {
        mApp = application as App
        mApp.addActivity(this)

        // 设置主题
        isDark = if (mApp.isDark != -1) {
            mApp.isDark
        } else {
            val dark = if (ThemeUtils.isDark(this)) 1 else 0
            mApp.isDark = dark
            dark
        }
        setTheme(ThemeUtils.getTheme(isDark == 1))
        DynamicColors.applyToActivityIfAvailable(this, mApp.dynamicColorsOptions)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        mApp.removeActivity(this)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (mApp.isDark == -1) {
            mApp.isDark = isDark
        }
        if (mApp.isDark != isDark) {
            recreate()
        }
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        theme.applyStyle(rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true)
    }

    override fun onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars()

        // 设置状态栏导航栏透明
        val window = window
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }
}