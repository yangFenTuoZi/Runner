package yangfentuozi.runner.app.ui.screens.main.envmanage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.screens.main.envmanage.components.EditEnvDialog
import yangfentuozi.runner.app.ui.screens.main.envmanage.components.EnvItem
import yangfentuozi.runner.app.ui.viewmodels.EnvManageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvManageScreen(
    viewModel: EnvManageViewModel = viewModel()
) {
    val envList by viewModel.envList.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val envToEdit by viewModel.envToEdit.collectAsState()
    val envToDelete by viewModel.envToDelete.collectAsState()

    if (isRefreshing) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (envList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
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
