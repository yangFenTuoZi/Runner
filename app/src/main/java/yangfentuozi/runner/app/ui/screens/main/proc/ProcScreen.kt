package yangfentuozi.runner.app.ui.screens.main.proc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.components.BlockWithAutoHideFloatActionButton
import yangfentuozi.runner.app.ui.screens.main.proc.components.ProcessItem
import yangfentuozi.runner.app.ui.viewmodels.ProcViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcScreen(
    viewModel: ProcViewModel = viewModel()
) {
    val processes by viewModel.processes.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showKillAllDialog by viewModel.showKillAllDialog.collectAsState()
    val showKillDialog by viewModel.showKillDialog.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // 监听生命周期事件
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.loadProcesses()
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

    BlockWithAutoHideFloatActionButton(
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
                        text = if (!Runner.pingServer()) {
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
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(processes) { process ->
                        ProcessItem(
                            process = process,
                            viewModel = viewModel
                        )
                    }
                }
            }
        },
        onClickFAB = {
            if (Runner.pingServer() && processes.isNotEmpty()) {
                viewModel.showKillAllDialog()
            }
        },
        contentFAB = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.kill_all_processes)
            )
        }
    )

    if (showKillAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideKillAllDialog() },
            title = { Text(stringResource(R.string.kill_all_processes)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hideKillAllDialog()
                        viewModel.killAllProcesses()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideKillAllDialog() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
    if (showKillDialog != -1) {
        AlertDialog(
            onDismissRequest = { viewModel.hideKillDialog() },
            title = { Text(stringResource(R.string.kill_process_ask)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hideKillDialog()
                        Thread {
                            viewModel.killProcess(showKillDialog)
                        }.start()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideKillDialog() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
