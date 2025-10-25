package yangfentuozi.runner.app.ui.screens.main.runner.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.components.BeautifulCard
import yangfentuozi.runner.shared.data.CommandInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommandItem(
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

    BeautifulCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = { showMenu = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = commandText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
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