package yangfentuozi.runner.app.ui.screens.main.runner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemProperties
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.activity.BridgeActivity
import yangfentuozi.runner.app.ui.activity.ExecShortcutActivity
import yangfentuozi.runner.app.ui.components.BlockWithAutoHideFloatActionButton
import yangfentuozi.runner.app.ui.components.ExecDialog
import yangfentuozi.runner.app.ui.screens.main.runner.components.CommandItem
import yangfentuozi.runner.app.ui.screens.main.runner.components.EditCommandDialog
import yangfentuozi.runner.app.ui.viewmodels.RunnerViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerScreen(
    viewModel: RunnerViewModel = viewModel()
) {
    val context = LocalContext.current
    val commands by viewModel.commands.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val commandToExec by viewModel.commandToExec.collectAsState()
    val commandToEdit by viewModel.commandToEdit.collectAsState()

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

    BlockWithAutoHideFloatActionButton(
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
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(
                        items = commands,
                        key = { index, _ -> index }
                    ) { index, command ->
                        CommandItem(
                            command = command,
                            position = index,
                            onRun = { viewModel.showExecDialog(command) },
                            onEdit = { viewModel.showEditDialog(command, index) },
                            onDelete = {
                                viewModel.deleteCommand(index)
                            },
                            onCopyName = {
                                command.name?.let { name ->
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                        .setPrimaryClip(ClipData.newPlainText("c", name))
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.copied_info) + "\n" + name,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onCopyCommand = {
                                command.command?.let { cmd ->
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                        .setPrimaryClip(ClipData.newPlainText("c", cmd))
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.copied_info) + "\n" + cmd,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onAddShortcut = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                                                    if (isColorOS()) BridgeActivity::class.java else ExecShortcutActivity::class.java
                                                )
                                                    .setAction(Intent.ACTION_VIEW)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    .putExtra("data", command.toBundle())
                                            )
                                            .build()
                                        shortcutManager.requestPinShortcut(shortcut, null)
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Not supported on this Android version",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
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
        EditCommandDialog(
            command = null,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { newCommand ->
                viewModel.addCommand(newCommand)
                viewModel.hideAddDialog()
            }
        )
    }

    commandToEdit?.let { (command, position) ->
        EditCommandDialog(
            command = command,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { updatedCommand ->
                viewModel.updateCommand(updatedCommand, position)
                viewModel.hideEditDialog()
            }
        )
    }

    commandToExec?.let { command ->
        ExecDialog(
            command = command,
            onDismiss = { viewModel.hideExecDialog() }
        )
    }
}

private fun isColorOS(): Boolean {
    return SystemProperties.get("ro.build.version.oplusrom").isNullOrEmpty().not()
}

