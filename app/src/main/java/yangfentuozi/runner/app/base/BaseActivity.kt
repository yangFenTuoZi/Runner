package yangfentuozi.runner.app.base

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import yangfentuozi.runner.app.App


open class BaseActivity() : ComponentActivity(),
    BaseDialogBuilder.IsDialogShowing {
    override var isDialogShowing: Boolean = false

    lateinit var mApp: App

    override fun onCreate(savedInstanceState: Bundle?) {
        mApp = application as App
        mApp.addActivity(this)
        super.onCreate(savedInstanceState)
        // 设置状态栏导航栏透明
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT)
        )
    }

    override fun onDestroy() {
        mApp.removeActivity(this)
        super.onDestroy()
    }
}
