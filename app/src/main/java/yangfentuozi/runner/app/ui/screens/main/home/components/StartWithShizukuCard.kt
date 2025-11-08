package yangfentuozi.runner.app.ui.screens.main.home.components

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.components.ModernActionCard
import yangfentuozi.runner.app.ui.viewmodels.HomeViewModel

@Composable
fun StartWithShizukuCard(viewModel: HomeViewModel) {
    val context = LocalContext.current

    ModernActionCard(
        icon = Icons.Default.Star,
        title = stringResource(R.string.start_with_shizuku),
        subtitle = "",
        buttonText = stringResource(R.string.start),
        onButtonClick = {
            // 如果Shizuku未运行，提示用户
            if (!Runner.shizukuStatus) {
                Toast.makeText(context, R.string.shizuku_not_running, Toast.LENGTH_LONG).show()
                return@ModernActionCard
            }

            // 如果没有权限，请求权限
            if (!Runner.shizukuPermission) {
                Toast.makeText(context, "正在请求 Shizuku 权限...", Toast.LENGTH_SHORT).show()
                return@ModernActionCard
            }

            // 有权限后启动
            // TODO 通过 Shizuku 启动
            viewModel.tryBindService()
        }
    )
}