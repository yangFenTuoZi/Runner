package yangfentuozi.runner.app.ui.screens.main.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.components.BeautifulCard
import yangfentuozi.runner.app.ui.viewmodels.HomeViewModel

@Composable
fun ServiceStatusCard(viewModel: HomeViewModel) {
    val isRunning = Runner.pingServer()
    val version = Runner.serviceVersion

    StatusCard(
        icon = if (isRunning) R.drawable.ic_check_circle_outline_24 else R.drawable.ic_error_outline_24,
        title = stringResource(if (isRunning) R.string.service_is_running else R.string.service_not_running),
        summary = if (isRunning) stringResource(R.string.service_version, version) else null,
        onClick = if (!isRunning) {
            { viewModel.tryBindService() }
        } else null
    )
}

@Composable
fun ShizukuStatusCard() {
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
fun StatusCard(
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