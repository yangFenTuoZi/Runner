package yangfentuozi.runner.app.ui.screens.main.envmanage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.components.ContentWithAutoHideFloatActionButton
import yangfentuozi.runner.app.ui.screens.main.envmanage.components.EditEnvDialog
import yangfentuozi.runner.app.ui.screens.main.envmanage.components.EnvItem
import yangfentuozi.runner.app.ui.theme.AppSpacing
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

    val lifecycleOwner = LocalLifecycleOwner.current

    // 监听生命周期事件
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
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


    ContentWithAutoHideFloatActionButton(
        content = {
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
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = AppSpacing.topBarContentSpacing,
                        bottom = AppSpacing.screenBottomPadding,
                        start = AppSpacing.screenHorizontalPadding,
                        end = AppSpacing.screenHorizontalPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.cardSpacing)
                ) {
                    itemsIndexed(
                        items = envList,
                        key = { index, item -> item.key ?: index }
                    ) { _, env ->
                        EnvItem(
                            env = env,
                            onEdit = { viewModel.showEditDialog(env) },
                            onDelete = { viewModel.showDeleteDialog(env) },
                            onToggle = { viewModel.updateEnv(env.key!!, !env.enabled) }
                        )
                    }
                }
            }
        },
        onClickFAB = {
            viewModel.showAddDialog()
        },
        contentFAB = {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
        }
    )

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
            onConfirm = { _, value ->
                viewModel.updateEnv(env.key!!, value)
                viewModel.hideEditDialog()
            }
        )
    }

    envToDelete?.let { env ->
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("${env.key}") },
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
