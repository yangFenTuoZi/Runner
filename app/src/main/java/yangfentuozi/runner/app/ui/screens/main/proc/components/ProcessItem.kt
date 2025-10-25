package yangfentuozi.runner.app.ui.screens.main.proc.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.shared.data.ProcessInfo

@Composable
fun ProcessItem(
    process: ProcessInfo,
    onKill: () -> Unit
) {
    var showKillDialog by remember { mutableStateOf(false) }

    // 查找 nice name
    val niceName = remember(process) {
        process.args?.let{ args ->
            val index = args.indexOf("--nice-name")
            if (index != -1 && index + 1 < args.size) {
                args[index + 1]
            } else {
                null
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (niceName != null) {
                    Text(
                        text = niceName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = stringResource(R.string.pid_info, process.pid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showKillDialog = true }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_stop_circle_outline_24),
                    contentDescription = stringResource(R.string.kill)
                )
            }
        }
    }

    if (showKillDialog) {
        AlertDialog(
            onDismissRequest = { showKillDialog = false },
            title = { Text(stringResource(R.string.kill_process_ask)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showKillDialog = false
                        Thread {
                            onKill()
                        }.start()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
