package yangfentuozi.runner.app.ui.screens.main.proc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.Service
import yangfentuozi.runner.app.ui.components.ContentWithAutoHideFloatActionButton
import yangfentuozi.runner.app.ui.screens.main.proc.components.ProcessItem
import yangfentuozi.runner.app.ui.screens.main.settings.components.CheckboxItem
import yangfentuozi.runner.app.ui.theme.AppSpacing
import yangfentuozi.runner.app.ui.viewmodels.ProcViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcScreen(
    viewModel: ProcViewModel = viewModel()
) {
    val processes by viewModel.processes.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showKillDialog by remember { mutableIntStateOf(-1) }

    ContentWithAutoHideFloatActionButton(
        content = {
            if (isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (processes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!Service.pingService()) {
                            stringResource(R.string.service_not_running)
                        } else {
                            stringResource(R.string.no_processes)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = AppSpacing.topBarContentSpacing,
                        bottom = AppSpacing.screenBottomPadding,
                        start = AppSpacing.screenHorizontalPadding,
                        end = AppSpacing.screenHorizontalPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.cardSpacing)
                ) {
                    items(processes) { process ->
                        ProcessItem(
                            process = process,
                            showKillDialog = { showKillDialog = it }
                        )
                    }
                }
            }
        },
        onClickFAB = {
            if (Service.pingService() && processes.isNotEmpty()) {
                showKillDialog = -1010
            }
        },
        contentFAB = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.kill_all_processes)
            )
        }
    )

    if (showKillDialog != -1) {
        var forceKill by remember {
            mutableStateOf(
                App.preferences.getBoolean(
                    "force_kill",
                    false
                )
            )
        }
        var killChildren by remember {
            mutableStateOf(
                App.preferences.getBoolean(
                    "kill_child_processes",
                    false
                )
            )
        }

        AlertDialog(
            onDismissRequest = { showKillDialog = -1 },
            title = { Text(stringResource(if (showKillDialog == -1010) R.string.kill_all_processes_ask else R.string.kill_process_ask)) },
            text = {
                Column {
                    CheckboxItem(
                        title = stringResource(R.string.force_kill),
                        checked = forceKill,
                        onCheckedChange = { forceKill = it },
                        enabled = true
                    )
                    CheckboxItem(
                        title = stringResource(R.string.kill_child_processes),
                        checked = killChildren,
                        onCheckedChange = { killChildren = it },
                        enabled = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showKillDialog = -1
                        Thread {
                            viewModel.killProcess(
                                pid = showKillDialog,
                                forceKill = forceKill,
                                killChildren = killChildren
                            )
                        }.start()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillDialog = -1 }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
