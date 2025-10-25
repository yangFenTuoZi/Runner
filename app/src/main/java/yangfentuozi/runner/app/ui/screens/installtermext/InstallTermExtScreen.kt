package yangfentuozi.runner.app.ui.screens.installtermext

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.theme.monoFontFamily
import yangfentuozi.runner.app.ui.viewmodels.InstallTermExtViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallTermExtScreen(
    uri: Uri?,
    externalCacheDir: File?,
    onNavigateBack: () -> Unit,
    onShowToast: (String) -> Unit,
    onShowToastRes: (Int) -> Unit,
    viewModel: InstallTermExtViewModel = viewModel()
) {
    val output by viewModel.output.collectAsState()
    val isInstalling by viewModel.isInstalling.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val scrollState = rememberScrollState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.install_term_ext)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isInstalling
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
}
