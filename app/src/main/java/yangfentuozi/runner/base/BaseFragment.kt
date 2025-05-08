package yangfentuozi.runner.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import yangfentuozi.runner.ui.activity.MainActivity

open class BaseFragment : Fragment() {
    protected lateinit var mContext: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = (activity as? MainActivity)
            ?: throw RuntimeException("父Activity非MainActivity")
    }

    override fun onStart() {
        super.onStart()
        mContext.toolbar.subtitle = null
    }

    fun getAppBar(): AppBarLayout = mContext.appBar

    fun getToolbar(): MaterialToolbar = mContext.toolbar

    fun runOnUiThread(action: Runnable) {
        mContext.runOnUiThread(action)
    }
}