package yangfentuozi.runner.app.ui.screens.main.home

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.activity.InstallTermExtActivity
import yangfentuozi.runner.app.ui.components.BeautifulCard
import yangfentuozi.runner.shared.data.TermExtVersion

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    // 监听 Runner 状态变化
    LaunchedEffect(Unit) {
        Runner.refreshStatus()
    }

    DisposableEffect(Unit) {
        val shizukuPermissionListener = Runner.ShizukuPermissionListener {
            refreshTrigger++
        }
        val shizukuStatusListener = Runner.ShizukuStatusListener {
            refreshTrigger++
        }
        val serviceStatusListener = Runner.ServiceStatusListener {
            refreshTrigger++
        }

        Runner.addShizukuPermissionListener(shizukuPermissionListener)
        Runner.addShizukuStatusListener(shizukuStatusListener)
        Runner.addServiceStatusListener(serviceStatusListener)

        onDispose {
            Runner.removeShizukuPermissionListener(shizukuPermissionListener)
            Runner.removeShizukuStatusListener(shizukuStatusListener)
            Runner.removeServiceStatusListener(serviceStatusListener)
        }
    }

    // 文件选择器用于安装 Term Ext
    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val mimeType = context.contentResolver.getType(uri)
                if (mimeType == "application/zip") {
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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        pickFileLauncher.launch(Intent.createChooser(intent, context.getString(R.string.pick_term_ext)))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item(key = "service_status_$refreshTrigger") {
            ServiceStatusCard()
        }
        item(key = "shizuku_status_$refreshTrigger") {
            ShizukuStatusCard()
        }
        if (!Runner.shizukuPermission) {
            item(key = "grant_perm_$refreshTrigger") {
                GrantShizukuPermCard()
            }
        }
        if (Runner.pingServer()) {
            item(key = "term_ext_$refreshTrigger") {
                TermExtStatusCard(onInstallTermExt)
            }
        }
    }
}

@Composable
private fun ServiceStatusCard() {
    val isRunning = Runner.pingServer()
    val version = Runner.serviceVersion

    StatusCard(
        icon = if (isRunning) R.drawable.ic_check_circle_outline_24 else R.drawable.ic_error_outline_24,
        title = stringResource(if (isRunning) R.string.service_is_running else R.string.service_not_running),
        summary = if (isRunning) stringResource(R.string.service_version, version) else null,
        onClick = if (!isRunning) {
            {
                Thread {
                    Runner.tryBindService()
                }.start()
            }
        } else null
    )
}

@Composable
private fun ShizukuStatusCard() {
    val isRunning = Runner.shizukuStatus
    val isRoot = Runner.shizukuUid == 0
    val apiVersion = Runner.shizukuApiVersion
    val patchVersion = Runner.shizukuPatchVersion

    val user = if (isRoot) "root" else "adb"
    
    StatusCard(
        icon = if (isRunning) R.drawable.ic_check_circle_outline_24 else R.drawable.ic_error_outline_24,
        title = stringResource(if (isRunning) R.string.shizuku_is_running else R.string.shizuku_not_running),
        summary = if (isRunning) stringResource(R.string.shizuku_version, user, "$apiVersion.$patchVersion") else null
    )
}

@Composable
private fun GrantShizukuPermCard() {
    BeautifulCard (
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Runner.requestPermission()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_error_outline_24),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.grant_shizuku_permission),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.grant_shizuku_permission_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TermExtStatusCard(onInstallTermExt: () -> Unit) {
    var termExtVersion by remember { mutableStateOf<TermExtVersion?>(null) }

    LaunchedEffect(Unit) {
        try {
            termExtVersion = Runner.service?.termExtVersion
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val isInstalled = termExtVersion != null
    val versionText = termExtVersion?.versionName ?: ""

    BeautifulCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isInstalled) {
                    Modifier.clickable { onInstallTermExt() }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    if (isInstalled) R.drawable.ic_check_circle_outline_24 else R.drawable.ic_error_outline_24
                ),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isInstalled) R.string.term_ext_installed else R.string.term_ext_not_installed),
                    style = MaterialTheme.typography.titleMedium
                )
                if (isInstalled && versionText.isNotEmpty()) {
                    Text(
                        text = versionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isInstalled) {
                IconButton(onClick = onInstallTermExt) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.install_term_ext)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: Int,
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null
) {
    BeautifulCard (
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (summary != null && summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

