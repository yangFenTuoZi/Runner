package yangfentuozi.runner.app.ui.screens.main

import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Service
import yangfentuozi.runner.app.ui.screens.main.home.HomeScreen
import yangfentuozi.runner.app.ui.screens.main.proc.ProcScreen
import yangfentuozi.runner.app.ui.screens.main.runner.RunnerScreen
import yangfentuozi.runner.app.ui.screens.main.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var showStopServerDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (currentRoute) {
                        Screen.Home.route -> stringResource(R.string.title_home)
                        Screen.Runner.route -> stringResource(R.string.title_runner)
                        Screen.Terminal.route -> stringResource(R.string.title_terminal)
                        Screen.Proc.route -> stringResource(R.string.title_proc)
                        Screen.Settings.route -> stringResource(R.string.title_settings)
                        else -> stringResource(R.string.app_name)
                    }
                    Text(title)
                },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = {
                        if (Service.pingServer()) {
                            showStopServerDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.StopCircle,
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
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                    // 其他页面使用导航
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

private data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, Icons.Outlined.Home, R.string.title_home),
    BottomNavItem(Screen.Runner.route, Icons.Outlined.PlayArrow, R.string.title_runner),
    BottomNavItem(Screen.Terminal.route, Icons.Outlined.Terminal, R.string.title_terminal),
    BottomNavItem(Screen.Proc.route, Icons.Outlined.Layers, R.string.title_proc),
    BottomNavItem(Screen.Settings.route, Icons.Outlined.Settings, R.string.title_settings)
)

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Runner : Screen("runner")
    data object Terminal : Screen("terminal")
    data object Proc : Screen("proc")
    data object Settings : Screen("settings")
}

interface HideAllDialogs {
    fun hideAllDialogs()
}