package yangfentuozi.runner.app.ui.screens.main.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.activity.InstallTermExtActivity
import yangfentuozi.runner.app.ui.screens.main.home.components.GrantShizukuPermCard
import yangfentuozi.runner.app.ui.screens.main.home.components.RemoveTermExtConfirmDialog
import yangfentuozi.runner.app.ui.screens.main.home.components.ServiceStatusCard
import yangfentuozi.runner.app.ui.screens.main.home.components.ShizukuStatusCard
import yangfentuozi.runner.app.ui.screens.main.home.components.TermExtStatusCard
import yangfentuozi.runner.app.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToInstallTermExt: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val refreshTrigger by viewModel.refreshTrigger.collectAsState()
    val showRemoveTermExtConfirmDialog by viewModel.showRemoveTermExtConfirmDialog.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // 监听生命周期事件
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.triggerRefresh()
                    viewModel.loadTermExtVersion()
                }

                Lifecycle.Event.ON_STOP -> {
                    viewModel.hideAllDialogs()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 文件选择器用于安装 Term Ext
    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType == "application/zip") {
                if (onNavigateToInstallTermExt != null) {
                    // 使用导航方式
                    onNavigateToInstallTermExt(uri)
                } else {
                    // 回退到 Activity 方式（用于独立页面）
                    val installIntent = Intent(context, InstallTermExtActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        setDataAndType(uri, "application/zip")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(installIntent)
                }
            }
        }
    }

    val onInstallTermExt = {
        pickFileLauncher.launch("application/zip")
    }

    val onRemoveTermExt = {
        viewModel.showRemoveTermExtConfirmDialog()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item(key = "service_status_$refreshTrigger") {
            ServiceStatusCard(viewModel)
        }
        item(key = "shizuku_status_$refreshTrigger") {
            ShizukuStatusCard()
        }
        if (!Runner.shizukuPermission) {
            item(key = "grant_perm_$refreshTrigger") {
                GrantShizukuPermCard(viewModel)
            }
        }
        if (Runner.pingServer()) {
            item(key = "term_ext_$refreshTrigger") {
                TermExtStatusCard(
                    onInstallTermExt = onInstallTermExt,
                    onRemoveTermExt = onRemoveTermExt,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showRemoveTermExtConfirmDialog) {
        RemoveTermExtConfirmDialog(
            onDismissRequest = {
                viewModel.hideRemoveTermExtConfirmDialog()
            },
            onConfirmRequest = {
                viewModel.removeTermExt()
            }
        )
    }
}

