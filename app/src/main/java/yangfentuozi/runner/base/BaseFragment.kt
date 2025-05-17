package yangfentuozi.runner.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import yangfentuozi.runner.ui.activity.MainActivity

open class BaseFragment : Fragment() {
    protected lateinit var mContext: MainActivity
    val appBar get() = mContext.appBar
    val toolbar get() = mContext.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = (activity as? MainActivity)
            ?: throw RuntimeException("父Activity非MainActivity")
    }

    override fun onStart() {
        super.onStart()
        mContext.toolbar.subtitle = null
    }

    fun runOnUiThread(action: Runnable) {
        mContext.runOnUiThread(action)
    }
}