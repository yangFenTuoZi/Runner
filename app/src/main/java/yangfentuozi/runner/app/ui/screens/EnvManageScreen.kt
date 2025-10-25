package yangfentuozi.runner.app.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.theme.RunnerTheme
import yangfentuozi.runner.app.ui.viewmodels.EnvManageViewModel
import yangfentuozi.runner.shared.data.EnvInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvManageScreen(
    onNavigateBack: () -> Unit,
    viewModel: EnvManageViewModel = viewModel()
) {
    val envList by viewModel.envList.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val envToEdit by viewModel.envToEdit.collectAsState()
    val envToDelete by viewModel.envToDelete.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.env_manage)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
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
        } else if (envList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty),
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
                itemsIndexed(
                    items = envList,
                    key = { index, item -> item.key ?: index }
                ) { index, env ->
                    EnvItem(
                        env = env,
                        onEdit = { viewModel.showEditDialog(env) },
                        onDelete = { viewModel.showDeleteDialog(env) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        EditEnvDialog(
            env = null,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { key, value ->
                viewModel.addEnv(key, value)
                viewModel.hideAddDialog()
            }
        )
    }

    envToEdit?.let { env ->
        EditEnvDialog(
            env = env,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { key, value ->
                viewModel.updateEnv(env.key!!, env.value!!, key, value)
                viewModel.hideEditDialog()
            }
        )
    }

    envToDelete?.let { env ->
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("Delete ${env.key}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEnv(env.key!!)
                        viewModel.hideDeleteDialog()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnvItem(
    env: EnvInfo,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = { showMenu = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = env.key ?: "",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = env.value ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EditEnvDialog(
    env: EnvInfo?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var key by remember { mutableStateOf(env?.key ?: "") }
    var value by remember { mutableStateOf(env?.value ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.key)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = env == null // 只有新建时才能编辑 key
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.value)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onConfirm(key, value)
                    }
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

// 创建一个独立的 Activity 用于环境管理
class EnvManageActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunnerTheme {
                EnvManageScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

