package yangfentuozi.runner.app.ui.screens.main.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.color.DynamicColors
import yangfentuozi.runner.R
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.data.BackupManager
import yangfentuozi.runner.app.ui.screens.main.settings.components.AboutDialog
import yangfentuozi.runner.app.ui.screens.main.settings.components.BackupDialog
import yangfentuozi.runner.app.ui.screens.main.settings.components.DarkThemeDialog
import yangfentuozi.runner.app.ui.screens.main.settings.components.PreferenceCategory
import yangfentuozi.runner.app.ui.screens.main.settings.components.PreferenceItem
import yangfentuozi.runner.app.ui.screens.main.settings.components.SwitchPreferenceItem
import yangfentuozi.runner.app.ui.theme.AppSpacing
import yangfentuozi.runner.app.ui.theme.ThemeManager
import yangfentuozi.runner.app.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateToEnvManage: () -> Unit,
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

    val lifecycleOwner = LocalLifecycleOwner.current

    // 监听生命周期事件
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                }

                Lifecycle.Event.ON_STOP -> {
                    viewModel.hideAllDialogs()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            Toast.makeText(context, R.string.restore_started, Toast.LENGTH_SHORT).show()
            BackupManager.restore(context = context, uri = uri) {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, R.string.restore_completed, Toast.LENGTH_SHORT).show()
                }
                (context.applicationContext as? App)?.finishApp()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            top = AppSpacing.topBarContentSpacing,
            bottom = AppSpacing.screenBottomPadding,
            start = AppSpacing.screenHorizontalPadding,
            end = AppSpacing.screenHorizontalPadding
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.cardSpacing)
    ) {
        // 主题设置
        item {
            PreferenceCategory(title = stringResource(R.string.theme))
        }

        item {
            val currentThemeMode by ThemeManager.themeMode.collectAsState()
            PreferenceItem(
                title = stringResource(R.string.dark_theme),
                subtitle = stringResource(ThemeManager.getThemeModeName(currentThemeMode)),
                onClick = { viewModel.showDarkThemeDialog() },
                icon = Icons.Default.DarkMode,
                showArrow = false
            )
        }

        item {
            SwitchPreferenceItem(
                title = stringResource(R.string.pure_black_dark_theme),
                subtitle = stringResource(R.string.pure_black_dark_theme_summary),
                checked = blackDarkTheme,
                onCheckedChange = {
                    viewModel.setBlackDarkTheme(it)
                    // 不需要 recreate,主题会自动更新
                },
                icon = Icons.Default.InvertColors
            )
        }

        if (DynamicColors.isDynamicColorAvailable()) {
            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.theme_color_system),
                    subtitle = "",
                    checked = followSystemAccent,
                    onCheckedChange = {
                        viewModel.setFollowSystemAccent(it)
                        // 不需要 recreate,主题会自动更新
                    },
                    icon = Icons.Default.ColorLens
                )
            }
        }

        // TODO 颜色选择
//        if (!followSystemAccent.value || !DynamicColors.isDynamicColorAvailable()) {
//        }

        // 备份设置
        item {
            PreferenceCategory(title = stringResource(R.string.backup))
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.export_data),
                subtitle = stringResource(R.string.export_data_summary),
                onClick = { viewModel.showBackupDialog() },
                icon = Icons.Default.Backup,
                showArrow = true
            )
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.import_data),
                subtitle = stringResource(R.string.import_data_summary),
                onClick = { pickFileLauncher.launch(arrayOf("application/x-tar")) },
                icon = Icons.Default.Restore,
                showArrow = true
            )
        }

        // 环境设置
        item {
            PreferenceCategory(title = stringResource(R.string.runtime))
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.env_manage),
                subtitle = stringResource(R.string.env_manage_summary),
                onClick = onNavigateToEnvManage,
                icon = Icons.AutoMirrored.Filled.Article,
                showArrow = true
            )
        }

        // 进程设置
        item {
            PreferenceCategory(title = stringResource(R.string.title_proc))
        }

        item {
            SwitchPreferenceItem(
                title = stringResource(R.string.force_kill),
                subtitle = stringResource(R.string.force_kill_summary),
                checked = forceKill,
                onCheckedChange = {
                    viewModel.setForceKill(it)
                },
                icon = Icons.Default.StopCircle
            )
        }

        item {
            SwitchPreferenceItem(
                title = stringResource(R.string.kill_child_processes),
                subtitle = stringResource(R.string.kill_child_processes_summary),
                checked = killChildProcesses,
                onCheckedChange = {
                    viewModel.setKillChildProcesses(it)
                },
                icon = Icons.Default.LayersClear
            )
        }

        // 关于
        item {
            PreferenceCategory(title = stringResource(R.string.about))
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.about),
                subtitle = "",
                onClick = { viewModel.showAboutDialog() },
                icon = Icons.Default.Info,
                showArrow = false
            )
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.help),
                subtitle = "",
                onClick = {
                    Toast.makeText(context, "没做", Toast.LENGTH_SHORT).show()
                },
                icon = Icons.AutoMirrored.Filled.Help,
                showArrow = false
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
