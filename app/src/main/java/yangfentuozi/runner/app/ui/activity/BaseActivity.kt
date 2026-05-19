package yangfentuozi.runner.app.ui.activity

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import yangfentuozi.runner.app.App

open class BaseActivity : ComponentActivity() {

    lateinit var mApp: App

    override fun onCreate(savedInstanceState: Bundle?) {
        mApp = application as App
        mApp.addActivity(this)
        super.onCreate(savedInstanceState)
        // 设置状态栏导航栏透明
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
    }

    override fun onDestroy() {
        mApp.removeActivity(this)
        super.onDestroy()
    }
}