package yangfentuozi.runner.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Runner : Screen("runner")
    data object Terminal : Screen("terminal")
    data object Proc : Screen("proc")
    data object Settings : Screen("settings")
}

