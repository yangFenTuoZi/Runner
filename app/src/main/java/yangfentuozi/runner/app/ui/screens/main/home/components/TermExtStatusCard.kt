package yangfentuozi.runner.app.ui.screens.main.home.components

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.components.ModernActionCard
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

    ModernActionCard(
        icon = Icons.Default.Terminal,
        title = stringResource(R.string.term_ext),
        subtitle = if (isInstalled) stringResource(R.string.installed)  + " | ${termExtVersion?.versionName} (${termExtVersion?.versionCode}) " else stringResource(R.string.not_installed),
        buttonText = stringResource(if (isInstalled) R.string.remove else R.string.install),
        onButtonClick = {
            if (!isInstalled) {
                onInstallTermExt.invoke()
            } else {
                onRemoveTermExt.invoke()
            }
        }
    )
}
