package yangfentuozi.runner.app.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Service
import yangfentuozi.runner.app.ui.screens.main.home.HomeScreen
import yangfentuozi.runner.app.ui.screens.main.proc.ProcScreen
import yangfentuozi.runner.app.ui.screens.main.runner.RunnerScreen
import yangfentuozi.runner.app.ui.screens.main.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = backStack.last()

    var isServiceConnected by remember { mutableStateOf(Service.pingService()) }

    LaunchedEffect(Unit) {
        Service.addServiceStatusListener {
            isServiceConnected = it
        }
    }

    fun navigateBack() {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun navigateTo(screen: Screen) {
        if (current != screen) {
            navigateBack()
            backStack += screen
        }
    }

    var showStopServerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(current.labelRes))
                },
                navigationIcon = {},
                actions = {
                    if (isServiceConnected) {
                        IconButton(onClick = {
                            showStopServerDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.StopCircle,
                                contentDescription = stringResource(R.string.stop_server)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
        }
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding)
        ) {
            val items = listOf(
                Screen.Home,
                Screen.Runner,
                Screen.Proc,
                Screen.Terminal,
                Screen.Settings
            )
            NavigationSuiteScaffold(navigationSuiteItems = {
                items.forEach { item ->
                    item(
                        selected = current == item,
                        onClick = { navigateTo(item) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(item.labelRes),
                            )
                        },
                        label = {
                            Text(stringResource(item.labelRes))
                        }
                    )
                }
            }) {
                NavDisplay(
                    backStack = backStack,
                    onBack = ::navigateBack,
                    entryProvider = { key ->
                        when (key) {
                            Screen.Home -> NavEntry(key) {
                                HomeScreen()
                            }

                            Screen.Runner -> NavEntry(key) {
                                RunnerScreen()
                            }

                            Screen.Terminal -> NavEntry(key) {
                                TODO()
                            }

                            Screen.Proc -> NavEntry(key) {
                                ProcScreen()
                            }

                            Screen.Settings -> NavEntry(key) {
                                SettingsScreen()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showStopServerDialog) {
        StopServerDialog(
            onDismiss = { showStopServerDialog = false },
            onConfirm = {
                showStopServerDialog = false
                Thread {
                    try {
                        Service.tryUnbindService(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        )
    }
}

@Composable
private fun StopServerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.warning)) },
        text = { Text(stringResource(R.string.confirm_stop_server)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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

sealed class Screen(
    val labelRes: Int,
    val icon: ImageVector
) {
    data object Home : Screen(R.string.title_home, Icons.Outlined.Home)
    data object Runner : Screen(R.string.title_runner, Icons.Outlined.PlayArrow)
    data object Terminal : Screen(R.string.title_terminal, Icons.Outlined.Terminal)
    data object Proc : Screen(R.string.title_proc, Icons.Outlined.Layers)
    data object Settings : Screen(R.string.title_settings, Icons.Outlined.Settings)
}
