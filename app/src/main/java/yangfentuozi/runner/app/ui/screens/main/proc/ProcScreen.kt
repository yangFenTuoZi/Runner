package yangfentuozi.runner.app.ui.screens.main.proc

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.viewmodels.ProcViewModel
import yangfentuozi.runner.shared.data.ProcessInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcScreen(
    activity: ComponentActivity,
    viewModel: ProcViewModel = viewModel()
) {
    val processes by viewModel.processes.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showKillAllDialog by viewModel.showKillAllDialog.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Runner.pingServer() && processes.isNotEmpty()) {
                        viewModel.showKillAllDialog()
                    }
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.kill_all_processes))
            }
        }
    ) { paddingValues ->
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (processes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(processes) { process ->
                    ProcessItem(
                        process = process,
                        onKill = {
                            viewModel.killProcess(process.pid)
                        }
                    )
                }
            }
        }
    }

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
}

@Composable
private fun ProcessItem(
    process: ProcessInfo,
    onKill: () -> Unit
) {
    var showKillDialog by remember { mutableStateOf(false) }

    // 查找 nice name
    val niceName = remember(process) {
        var niceNameFlag = false
        var result: String? = null
        process.args?.forEach { arg ->
            if (niceNameFlag) {
                result = arg
                return@forEach
            }
            if (arg == "--nice-name") niceNameFlag = true
        }
        result
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

