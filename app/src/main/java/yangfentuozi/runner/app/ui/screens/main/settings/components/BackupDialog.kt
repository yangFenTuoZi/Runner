package yangfentuozi.runner.app.ui.screens.main.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (BooleanArray) -> Unit
) {
    var appSettings by remember { mutableStateOf(true) }
    var dataDb by remember { mutableStateOf(true) }
    var termHome by remember { mutableStateOf(true) }
    var termUsr by remember { mutableStateOf(true) }

    val restricted = !Runner.pingServer()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_backup_data)) },
        text = {
            Column {
                CheckboxItem(
                    title = stringResource(R.string.app_settings),
                    checked = appSettings,
                    onCheckedChange = { appSettings = it },
                    enabled = true
                )
                CheckboxItem(
                    title = stringResource(R.string.data_db),
                    checked = dataDb,
                    onCheckedChange = { dataDb = it },
                    enabled = !restricted
                )
                CheckboxItem(
                    title = stringResource(R.string.term_home),
                    checked = termHome,
                    onCheckedChange = { termHome = it },
                    enabled = !restricted
                )
                CheckboxItem(
                    title = stringResource(R.string.term_usr),
                    checked = termUsr,
                    onCheckedChange = { termUsr = it },
                    enabled = !restricted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(booleanArrayOf(restricted, appSettings, dataDb, termHome, termUsr))
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

@Composable
fun CheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.5f
            )
        )
    }
}