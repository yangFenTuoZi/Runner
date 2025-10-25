package yangfentuozi.runner.app.ui.viewmodels

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import yangfentuozi.runner.app.App

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _showDarkThemeDialog = MutableStateFlow(false)
    val showDarkThemeDialog: StateFlow<Boolean> = _showDarkThemeDialog.asStateFlow()

    private val _showBackupDialog = MutableStateFlow(false)
    val showBackupDialog: StateFlow<Boolean> = _showBackupDialog.asStateFlow()

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    private val _blackDarkTheme = MutableStateFlow(App.preferences.getBoolean("black_dark_theme", false))
    val blackDarkTheme: StateFlow<Boolean> = _blackDarkTheme.asStateFlow()

    private val _followSystemAccent = MutableStateFlow(App.preferences.getBoolean("follow_system_accent", true))
    val followSystemAccent: StateFlow<Boolean> = _followSystemAccent.asStateFlow()

    private val _forceKill = MutableStateFlow(App.preferences.getBoolean("force_kill", false))
    val forceKill: StateFlow<Boolean> = _forceKill.asStateFlow()

    private val _killChildProcesses = MutableStateFlow(App.preferences.getBoolean("kill_child_processes", false))
    val killChildProcesses: StateFlow<Boolean> = _killChildProcesses.asStateFlow()

    private val _lastBackupArgs = MutableStateFlow<BooleanArray?>(null)
    val lastBackupArgs: StateFlow<BooleanArray?> = _lastBackupArgs.asStateFlow()

    fun showDarkThemeDialog() {
        _showDarkThemeDialog.value = true
    }

    fun hideDarkThemeDialog() {
        _showDarkThemeDialog.value = false
    }

    fun showBackupDialog() {
        _showBackupDialog.value = true
    }

    fun hideBackupDialog() {
        _showBackupDialog.value = false
    }

    fun showAboutDialog() {
        _showAboutDialog.value = true
    }

    fun hideAboutDialog() {
        _showAboutDialog.value = false
    }

    fun setBlackDarkTheme(value: Boolean) {
        _blackDarkTheme.value = value
        App.preferences.edit { putBoolean("black_dark_theme", value) }
    }

    fun setFollowSystemAccent(value: Boolean) {
        _followSystemAccent.value = value
        App.preferences.edit { putBoolean("follow_system_accent", value) }
    }

    fun setForceKill(value: Boolean) {
        _forceKill.value = value
        App.preferences.edit { putBoolean("force_kill", value) }
    }

    fun setKillChildProcesses(value: Boolean) {
        _killChildProcesses.value = value
        App.preferences.edit { putBoolean("kill_child_processes", value) }
    }

    fun setLastBackupArgs(args: BooleanArray) {
        _lastBackupArgs.value = args
    }
}

