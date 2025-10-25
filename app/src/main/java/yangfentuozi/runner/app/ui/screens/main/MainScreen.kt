package yangfentuozi.runner.app.ui.screens.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.screens.main.home.HomeScreen
import yangfentuozi.runner.app.ui.screens.main.proc.ProcScreen
import yangfentuozi.runner.app.ui.screens.main.runner.RunnerScreen
import yangfentuozi.runner.app.ui.screens.main.settings.SettingsScreen
import yangfentuozi.runner.app.ui.screens.main.term.TerminalScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    activity: ComponentActivity
) {
    val navController = rememberNavController()
    var showStopServerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val title = when (currentDestination?.route) {
                        Screen.Home.route -> stringResource(R.string.title_home)
                        Screen.Runner.route -> stringResource(R.string.title_runner)
                        Screen.Terminal.route -> stringResource(R.string.title_terminal)
                        Screen.Proc.route -> stringResource(R.string.title_proc)
                        Screen.Settings.route -> stringResource(R.string.title_settings)
                        else -> stringResource(R.string.app_name)
                    }
                    Text(title)
                },
                actions = {
                    IconButton(onClick = {
                        if (Runner.pingServer()) {
                            showStopServerDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_stop_circle_outline_24),
                            contentDescription = stringResource(R.string.stop_server)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(item.iconRes),
                                contentDescription = stringResource(item.labelRes)
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Runner.route) {
                RunnerScreen()
            }
            composable(Screen.Terminal.route) {
                TerminalScreen()
            }
            composable(Screen.Proc.route) {
                ProcScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
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
                        Runner.tryUnbindService(true)
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

private data class BottomNavItem(
    val route: String,
    val iconRes: Int,
    val labelRes: Int
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, R.drawable.ic_home_outline_24, R.string.title_home),
    BottomNavItem(Screen.Runner.route, R.drawable.ic_play_arrow_outline_24, R.string.title_runner),
    BottomNavItem(Screen.Terminal.route, R.drawable.ic_terminal_24, R.string.title_terminal),
    BottomNavItem(Screen.Proc.route, R.drawable.ic_layers_outline_24, R.string.title_proc),
    BottomNavItem(Screen.Settings.route, R.drawable.ic_settings_outline_24, R.string.title_settings)
)

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Runner : Screen("runner")
    data object Terminal : Screen("terminal")
    data object Proc : Screen("proc")
    data object Settings : Screen("settings")
}
