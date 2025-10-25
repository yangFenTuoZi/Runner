package yangfentuozi.runner.app.ui.screens.main.term

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import yangfentuozi.runner.R
import yangfentuozi.runner.term.Term

/**
 * Terminal Screen - 使用 AndroidView 兼容层保持 term 模块的 XML 视图
 */
@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity ?: return
    
    // 为 Fragment 生成唯一的容器 ID
    val containerId = remember { R.id.terminal_fragment_container }

    DisposableEffect(Unit) {
        val fragmentManager = fragmentActivity.supportFragmentManager
        
        // 检查是否已经存在 Fragment
        var fragment = fragmentManager.findFragmentByTag("terminal_fragment") as? Term
        
        if (fragment == null) {
            // 创建新的 Fragment
            fragment = Term()
            fragmentManager.beginTransaction()
                .add(containerId, fragment, "terminal_fragment")
                .commitNow()
        }

        onDispose {
            // 不要在这里移除 Fragment，让 FragmentActivity 自己管理生命周期
        }
    }

    AndroidView(
        modifier = Modifier,
        factory = { context ->
            FragmentContainerView(context).apply {
                id = containerId
            }
        }
    )
}

