package yangfentuozi.runner.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.shared.data.CommandInfo

@Composable
fun EditCommandDialog(
    command: CommandInfo?,
    onDismiss: () -> Unit,
    onConfirm: (CommandInfo) -> Unit
) {
    var name by remember { mutableStateOf(command?.name ?: "") }
    var commandText by remember { mutableStateOf(command?.command ?: "") }
    var keepAlive by remember { mutableStateOf(command?.keepAlive ?: false) }

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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text(stringResource(R.string.command)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.keep_alive),
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                    )
                    Switch(
                        checked = keepAlive,
                        onCheckedChange = { keepAlive = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newCommand = CommandInfo().apply {
                        this.name = name
                        this.command = commandText
                        this.keepAlive = keepAlive
                    }
                    onConfirm(newCommand)
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

