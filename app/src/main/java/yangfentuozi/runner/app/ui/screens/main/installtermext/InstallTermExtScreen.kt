package yangfentuozi.runner.app.ui.screens.main.installtermext

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.app.ui.theme.monoFontFamily
import yangfentuozi.runner.app.ui.viewmodels.InstallTermExtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallTermExtScreen(
    uri: Uri?,
    onShowToastRes: (Int) -> Unit,
    viewModel: InstallTermExtViewModel = viewModel(),
    paddingValues: PaddingValues?,
    onInstallingStateChanged: ((Boolean) -> Unit)? = null
) {
    val output by viewModel.output.collectAsState()
    val isInstalling by viewModel.isInstalling.collectAsState()
    val scrollState = rememberScrollState()

    // 在安装进行时屏蔽返回操作
    BackHandler(enabled = isInstalling) {
        // 安装进行中时不处理返回操作
    }

    // 同步安装状态到父级
    LaunchedEffect(isInstalling) {
        onInstallingStateChanged?.invoke(isInstalling)
    }

    // 滚动到底部
    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // 处理安装逻辑
    LaunchedEffect(uri) {
        if (uri != null) {
            viewModel.startInstallation(uri, onShowToastRes)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanup()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (paddingValues != null) {
                    Modifier.padding(paddingValues)
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = output,
                    fontFamily = monoFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
