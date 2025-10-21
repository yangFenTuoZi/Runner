package yangfentuozi.runner.app.ui.screens

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
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.data.DataRepository
import yangfentuozi.runner.app.ui.activity.BridgeActivity
import yangfentuozi.runner.app.ui.activity.ExecShortcutActivity
import yangfentuozi.runner.app.ui.dialogs.EditCommandDialog
import yangfentuozi.runner.app.ui.dialogs.ExecDialog
import yangfentuozi.runner.shared.data.CommandInfo
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerScreen(activity: ComponentActivity) {
    val context = LocalContext.current
    val dataRepository = remember { DataRepository.getInstance(context) }
    var commands by remember { mutableStateOf(listOf<CommandInfo>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var commandToExec by remember { mutableStateOf<CommandInfo?>(null) }
    var commandToEdit by remember { mutableStateOf<Pair<CommandInfo, Int>?>(null) }

    val loadCommands = {
        isRefreshing = true
        commands = dataRepository.getAllCommands()
        isRefreshing = false
    }

    LaunchedEffect(Unit) {
        loadCommands()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
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
                    items = commands,
                    key = { index, _ -> index }
                ) { index, command ->
                    CommandItem(
                        command = command,
                        position = index,
                        onRun = { commandToExec = command },
                        onEdit = { commandToEdit = command to index },
                        onDelete = {
                            dataRepository.deleteCommand(index)
                            loadCommands()
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
                                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                                if (shortcutManager.isRequestPinShortcutSupported) {
                                    val label = if (command.name.isNullOrEmpty()) "Command" else command.name!!
                                    val shortcut = ShortcutInfo.Builder(context, UUID.randomUUID().toString())
                                        .setShortLabel(label)
                                        .setLongLabel(label)
                                        .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
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
    }

    if (showAddDialog) {
        EditCommandDialog(
            command = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newCommand ->
                dataRepository.addCommand(newCommand)
                loadCommands()
                showAddDialog = false
            }
        )
    }

    commandToEdit?.let { (command, position) ->
        EditCommandDialog(
            command = command,
            onDismiss = { commandToEdit = null },
            onConfirm = { updatedCommand ->
                dataRepository.updateCommand(updatedCommand, position)
                loadCommands()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommandItem(
    command: CommandInfo,
    position: Int,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyName: () -> Unit,
    onCopyCommand: () -> Unit,
    onAddShortcut: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isEmpty = command.command.isNullOrEmpty() && command.name.isNullOrEmpty()

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
                val nameText = if (command.name.isNullOrEmpty()) {
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("__NAME__")
                        }
                    }
                } else {
                    buildAnnotatedString { append(command.name) }
                }

                val commandText = if (command.command.isNullOrEmpty()) {
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("__CMD__")
                        }
                    }
                } else {
                    buildAnnotatedString { append(command.command) }
                }

                Text(
                    text = nameText,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = commandText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRun) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_play_arrow_baseline_24),
                    contentDescription = stringResource(R.string.exec_command)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_name)) },
                    onClick = {
                        onCopyName()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_command)) },
                    onClick = {
                        onCopyCommand()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_shortcut)) },
                    onClick = {
                        onAddShortcut()
                        showMenu = false
                    }
                )
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

private fun isColorOS(): Boolean {
    return SystemProperties.get("ro.build.version.oplusrom").isNullOrEmpty().not()
}

