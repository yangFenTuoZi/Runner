package yangfentuozi.runner.app.ui.screens.main.home.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import yangfentuozi.runner.R

@Composable
fun RemoveTermExtConfirmDialog(onDismissRequest: () -> Unit, onConfirmRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.warning))
        },
        text = {
            Text(stringResource(R.string.will_remove_term_ext))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirmRequest()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}