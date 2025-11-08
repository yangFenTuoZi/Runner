package yangfentuozi.runner.app.ui.screens.main.home.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.theme.AppShape
import yangfentuozi.runner.app.ui.viewmodels.HomeViewModel

@SuppressLint("DefaultLocale")
@Composable
fun TermExtStatusCard(
    onInstallTermExt: () -> Unit,
    onRemoveTermExt: () -> Unit,
    viewModel: HomeViewModel
) {
    val termExtVersion by viewModel.termExtVersion.collectAsState()
    val isInstalled = (termExtVersion?.versionCode ?: -1) != -1

    val iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer
    val iconTint = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.shapes.cardLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        enabled = true,
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AppShape.shapes.iconSmall)
                        .background(color = iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.term_ext),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isInstalled) stringResource(R.string.installed) + " | ${termExtVersion?.versionName} (${termExtVersion?.versionCode}) " else stringResource(
                            R.string.not_installed
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                if (isInstalled) {
                    OutlinedButton(
                        onClick = onRemoveTermExt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = AppShape.shapes.buttonMedium,
                    ) {
                        Text(
                            text = stringResource(R.string.remove),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                }

                Button(
                    onClick = onInstallTermExt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = AppShape.shapes.buttonMedium
                ) {
                    Text(
                        text = stringResource(R.string.install),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
