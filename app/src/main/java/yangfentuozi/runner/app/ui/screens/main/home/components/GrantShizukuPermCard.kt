package yangfentuozi.runner.app.ui.screens.main.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.components.ModernActionCard
import yangfentuozi.runner.app.ui.viewmodels.HomeViewModel

@Composable
fun GrantShizukuPermCard(viewModel: HomeViewModel) {
    ModernActionCard(
        icon = Icons.Default.Error,
        title = stringResource(R.string.grant_shizuku_permission),
        subtitle = stringResource(R.string.grant_shizuku_permission_summary),
        buttonText = stringResource(R.string.grant),
        onButtonClick = {
            viewModel.requestPermission()
        }
    )
}
