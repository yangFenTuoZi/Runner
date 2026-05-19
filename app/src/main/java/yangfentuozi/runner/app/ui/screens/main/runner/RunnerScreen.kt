package yangfentuozi.runner.app.ui.screens.main.runner

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.activity.ExecShortcutActivity
import yangfentuozi.runner.app.ui.components.ContentWithAutoHideFloatActionButton
import yangfentuozi.runner.app.ui.components.ExecDialog
import yangfentuozi.runner.app.ui.screens.main.runner.components.CommandItem
import yangfentuozi.runner.app.ui.screens.main.runner.components.EditCommandDialog
import yangfentuozi.runner.app.ui.theme.AppSpacing
import yangfentuozi.runner.app.ui.viewmodels.RunnerViewModel
import yangfentuozi.runner.shared.data.CommandInfo
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerScreen(
    viewModel: RunnerViewModel = viewModel()
) {
    val context = LocalContext.current
    val commands by viewModel.commands.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showAddDialog by remember { mutableIntStateOf(-1) }
    var showDeleteDialog by remember { mutableIntStateOf(-1) }
    var commandToExec by remember { mutableStateOf<CommandInfo?>(null) }
    var commandToEdit by remember { mutableStateOf<Pair<CommandInfo, Int>?>(null) }

    ContentWithAutoHideFloatActionButton(
        content = {
            if (isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                        items = commands,
                        key = { index, _ -> index }
                    ) { index, command ->
                        CommandItem(
                            command = command,
                            onRun = { commandToExec = command },
                            onEdit = { commandToEdit = Pair(command, index) },
                            onDelete = {
                                showDeleteDialog = index
                            },
                            onAddShortcut = {
                                val shortcutManager =
                                    context.getSystemService(ShortcutManager::class.java)
                                if (shortcutManager.isRequestPinShortcutSupported) {
                                    val label =
                                        if (command.name.isNullOrEmpty()) "Command" else command.name!!
                                    val shortcut = ShortcutInfo.Builder(
                                        context,
                                        UUID.randomUUID().toString()
                                    )
                                        .setShortLabel(label)
                                        .setLongLabel(label)
                                        .setIcon(
                                            Icon.createWithResource(
                                                context,
                                                R.mipmap.ic_launcher
                                            )
                                        )
                                        .setIntent(
                                            Intent(
                                                context,
                                                ExecShortcutActivity::class.java
                                            )
                                                .setAction(Intent.ACTION_VIEW)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                .putExtra("data", command.toBundle())
                                        )
                                        .build()
                                    shortcutManager.requestPinShortcut(shortcut, null)
                                }
                            }
                        )
                    }
                }
            }
        },
        onClickFAB = {
            showAddDialog = 0
        },
        contentFAB = {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
        }
    )

    if (showAddDialog != -1) {
        EditCommandDialog(
            command = null,
            onDismiss = { showAddDialog = -1 },
            onConfirm = { newCommand ->
                viewModel.addCommand(newCommand)
                showAddDialog = -1
            }
        )
    }

    if (showDeleteDialog != -1) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = -1 },
            title = { Text(stringResource(R.string.delete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCommand(showDeleteDialog)
                        showDeleteDialog = -1
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = -1 }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    commandToEdit?.let { (command, position) ->
        EditCommandDialog(
            command = command,
            onDismiss = { commandToEdit = null },
            onConfirm = { updatedCommand ->
                viewModel.updateCommand(updatedCommand, position)
                commandToEdit = null
            }
        )
    }

    commandToExec?.let { command ->
        ExecDialog(
            command = command,
            onDismiss = { commandToExec = null }
        )
    }
}

