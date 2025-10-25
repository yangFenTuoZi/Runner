package yangfentuozi.runner.app.ui.screens.main.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.color.DynamicColors
import yangfentuozi.runner.R
import yangfentuozi.runner.app.data.BackupManager
import yangfentuozi.runner.app.ui.activity.envmanage.EnvManageActivity
import yangfentuozi.runner.app.ui.screens.main.settings.components.AboutDialog
import yangfentuozi.runner.app.ui.screens.main.settings.components.BackupDialog
import yangfentuozi.runner.app.ui.screens.main.settings.components.DarkThemeDialog
import yangfentuozi.runner.app.ui.screens.main.settings.components.PreferenceCategory
import yangfentuozi.runner.app.ui.screens.main.settings.components.PreferenceItem
import yangfentuozi.runner.app.ui.screens.main.settings.components.SwitchPreferenceItem
import yangfentuozi.runner.app.ui.theme.ThemeManager
import yangfentuozi.runner.app.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val showDarkThemeDialog by viewModel.showDarkThemeDialog.collectAsState()
    val showBackupDialog by viewModel.showBackupDialog.collectAsState()
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()
    val blackDarkTheme by viewModel.blackDarkTheme.collectAsState()
    val followSystemAccent by viewModel.followSystemAccent.collectAsState()
    val forceKill by viewModel.forceKill.collectAsState()
    val killChildProcesses by viewModel.killChildProcesses.collectAsState()
    val lastBackupArgs by viewModel.lastBackupArgs.collectAsState()

    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-tar")
    ) { uri ->
        uri?.let {
            lastBackupArgs?.let { args ->
                BackupManager.backup(
                    context,
                    it,
                    backupAppSettings = args[1],
                    backupDataDb = args[2],
                    backupTermHome = args[3],
                    backupTermUsr = args[4]
                )
                Toast.makeText(context, R.string.backup_started, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // TODO 导入功能
//            showImportDialog = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        // 主题设置
        item {
            PreferenceCategory(title = stringResource(R.string.theme))
        }

        item {
            val currentThemeMode by ThemeManager.themeMode.collectAsState()
            PreferenceItem(
                title = stringResource(R.string.dark_theme),
                summary = stringResource(ThemeManager.getThemeModeName(currentThemeMode)),
                onClick = { viewModel.showDarkThemeDialog() }
            )
        }

        if (DynamicColors.isDynamicColorAvailable()) {
            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.theme_color_system),
                    checked = followSystemAccent,
                    onCheckedChange = {
                        viewModel.setFollowSystemAccent(it)
                        // 不需要 recreate,主题会自动更新
                    }
                )
            }
        }

        // TODO 颜色选择
//        if (!followSystemAccent.value || !DynamicColors.isDynamicColorAvailable()) {
//        }

        item {
            SwitchPreferenceItem(
                title = stringResource(R.string.pure_black_dark_theme),
                summary = stringResource(R.string.pure_black_dark_theme_summary),
                checked = blackDarkTheme,
                onCheckedChange = {
                    viewModel.setBlackDarkTheme(it)
                    // 不需要 recreate,主题会自动更新
                }
            )
        }

        // 备份设置
        item {
            PreferenceCategory(title = stringResource(R.string.backup))
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.export_data),
                summary = stringResource(R.string.export_data_summary),
                onClick = { viewModel.showBackupDialog() }
            )
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.import_data),
                summary = stringResource(R.string.import_data_summary),
                onClick = {
                    pickFileLauncher.launch(arrayOf("application/x-tar"))
                }
            )
        }

        // 环境设置
        item {
            PreferenceItem(
                title = stringResource(R.string.env_manage),
                summary = stringResource(R.string.env_manage_summary),
                onClick = {
                    context.startActivity(Intent(context, EnvManageActivity::class.java))
                }
            )
        }

        // 进程设置
        item {
            PreferenceCategory(title = stringResource(R.string.title_proc))
        }

        item {
            SwitchPreferenceItem(
                title = stringResource(R.string.force_kill),
                summary = stringResource(R.string.force_kill_summary),
                checked = forceKill,
                onCheckedChange = {
                    viewModel.setForceKill(it)
                }
            )
        }

        item {
            SwitchPreferenceItem(
                title = stringResource(R.string.kill_child_processes),
                summary = stringResource(R.string.kill_child_processes_summary),
                checked = killChildProcesses,
                onCheckedChange = {
                    viewModel.setKillChildProcesses(it)
                }
            )
        }

        // 关于
        item {
            PreferenceCategory(title = stringResource(R.string.about))
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.about),
                onClick = { viewModel.showAboutDialog() }
            )
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.help),
                onClick = {
                    Toast.makeText(context, "没做", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (showDarkThemeDialog) {
        DarkThemeDialog(
            onDismiss = { viewModel.hideDarkThemeDialog() },
            onSelect = { mode ->
                ThemeManager.setThemeMode(mode)
                viewModel.hideDarkThemeDialog()
            }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { viewModel.hideBackupDialog() },
            onConfirm = { args ->
                viewModel.setLastBackupArgs(args)
                saveFileLauncher.launch("runner_backup_${System.currentTimeMillis()}.tar")
                viewModel.hideBackupDialog()
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { viewModel.hideAboutDialog() }
        )
    }
}
