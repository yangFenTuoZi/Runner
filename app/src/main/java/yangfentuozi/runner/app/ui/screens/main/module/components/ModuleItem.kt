package yangfentuozi.runner.app.ui.screens.main.module.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.components.BeautifulCard
import yangfentuozi.runner.app.ui.viewmodels.ModuleViewModel
import yangfentuozi.runner.shared.data.TermModuleInfo

@Composable
fun ModuleItem(
    moduleInfo: TermModuleInfo,
    viewModel: ModuleViewModel,
    onUninstall: () -> Unit
) {
    BeautifulCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {}
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = moduleInfo.moduleName ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 0.9f
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "版本: ${moduleInfo.versionName}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 0.9f
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "作者: ${moduleInfo.author}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 0.9f
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (moduleInfo.description?.isNotEmpty() ?: false) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = moduleInfo.description!!,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 分割线
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // 占位
            Row {}

            IconButton(
                onClick = onUninstall,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.remove),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
