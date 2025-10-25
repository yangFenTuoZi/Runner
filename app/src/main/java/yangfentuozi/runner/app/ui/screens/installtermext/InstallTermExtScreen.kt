package yangfentuozi.runner.app.ui.screens.installtermext

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.R
import yangfentuozi.runner.app.ui.viewmodels.InstallTermExtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallTermExtScreen(
    uri: Uri?,
    onNavigateBack: () -> Unit,
    onShowToastRes: (Int) -> Unit,
    viewModel: InstallTermExtViewModel = viewModel()) {

    val isInstalling by viewModel.isInstalling.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.install_term_ext)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isInstalling
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        yangfentuozi.runner.app.ui.screens.main.installtermext.InstallTermExtScreen(
            uri = uri,
            onShowToastRes = onShowToastRes,
            paddingValues = paddingValues,
            viewModel = viewModel,
            onInstallingStateChanged = null
        )
    }
}