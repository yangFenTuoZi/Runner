package yangfentuozi.runner.app.ui.screens.main.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.components.ModernStatusCard

@Composable
fun ServiceStatusCard() {
    val isRunning = Runner.pingServer()

    ModernStatusCard(
        icon = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Error,
        title = stringResource(R.string.user_service),
        subtitle = "",
        statusText = stringResource(if (isRunning) R.string.running else R.string.stopped),
        isPositive = isRunning
    ) {
        if (isRunning) {
            val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(stringResource(R.string.version), "${Runner.serviceVersion}", contentColor)
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
