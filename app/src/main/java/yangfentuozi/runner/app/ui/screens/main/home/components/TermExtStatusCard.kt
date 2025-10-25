package yangfentuozi.runner.app.ui.screens.main.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.components.BeautifulCard
import yangfentuozi.runner.app.ui.viewmodels.HomeViewModel

@Composable
fun TermExtStatusCard(
    onInstallTermExt: () -> Unit,
    viewModel: HomeViewModel
) {
    val termExtVersion by viewModel.termExtVersion.collectAsState()

    val isInstalled = (termExtVersion?.versionCode ?: -1) != -1
    val versionText = termExtVersion?.versionName ?: ""

    BeautifulCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isInstalled) {
                    Modifier.clickable { onInstallTermExt() }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    if (isInstalled) R.drawable.ic_check_circle_outline_24 else R.drawable.ic_error_outline_24
                ),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isInstalled) R.string.term_ext_installed else R.string.term_ext_not_installed),
                    style = MaterialTheme.typography.titleMedium
                )
                if (isInstalled && versionText.isNotEmpty()) {
                    Text(
                        text = versionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isInstalled) {
                IconButton(onClick = onInstallTermExt) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.install_term_ext)
                    )
                }
            }
        }
    }
}
