package yangfentuozi.runner.app.ui.screens.main.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.theme.ThemeManager

@Composable
fun DarkThemeDialog(
    onDismiss: () -> Unit,
    onSelect: (ThemeManager.ThemeMode) -> Unit
) {
    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    val options = listOf(
        ThemeManager.ThemeMode.SYSTEM to stringResource(R.string.follow_system),
        ThemeManager.ThemeMode.DARK to stringResource(R.string.dark),
        ThemeManager.ThemeMode.LIGHT to stringResource(R.string.light)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dark_theme)) },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentThemeMode == mode,
                            onClick = { onSelect(mode) }
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}