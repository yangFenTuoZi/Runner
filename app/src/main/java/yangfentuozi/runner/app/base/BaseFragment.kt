package yangfentuozi.runner.app.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import yangfentuozi.runner.app.ui.activity.MainActivity

open class BaseFragment : Fragment() {
    protected lateinit var mMainActivity: MainActivity
    val appBar get() = mMainActivity.appBar
    val toolbar get() = mMainActivity.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMainActivity = (activity as? MainActivity)
            ?: throw RuntimeException("父Activity非MainActivity")
    }

    override fun onStart() {
        super.onStart()
        mMainActivity.toolbar.subtitle = null
    }

    fun runOnMainThread(action: Runnable) {
        mMainActivity.runOnMainThread(action)
    }
}