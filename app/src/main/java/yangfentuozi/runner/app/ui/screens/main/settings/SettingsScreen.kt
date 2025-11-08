package yangfentuozi.runner.app.ui.screens.main.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import yangfentuozi.runner.app.ui.theme.AppShape
import yangfentuozi.runner.app.ui.theme.AppSpacing
import yangfentuozi.runner.app.ui.theme.ThemeManager
import yangfentuozi.runner.app.ui.viewmodels.SettingsViewModel
import kotlin.math.roundToInt

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
                subtitle = "",
                onClick = {},
                icon = Icons.Default.DarkMode,
                showArrow = false,
                contentColumn = {
                    Box(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        ThemeSelectorWithAnimation(
                            currentMode = currentThemeMode,
                            onModeChange = { mode ->
                                ThemeManager.setThemeMode(mode)
                            }
                        )
                    }
                }
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

@Composable
private fun ThemeSelectorWithAnimation(
    currentMode: ThemeManager.ThemeMode,
    onModeChange: (ThemeManager.ThemeMode) -> Unit
) {
    val density = LocalDensity.current
    val themeCount = ThemeManager.ThemeMode.entries.size
    val currentIndex = ThemeManager.ThemeMode.entries.indexOf(currentMode)

    var innerWidth by remember { mutableStateOf(0) }

    val spacing = 4.dp
    val spacingPx = with(density) { spacing.toPx() }

    val animatedIndex by animateFloatAsState(
        targetValue = currentIndex.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "theme_index"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = AppShape.shapes.cardMedium
            )
            .padding(6.dp)
    ) {
        // 计算内部可用宽度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    innerWidth = size.width
                },
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            ThemeManager.ThemeMode.entries.forEach { _ ->
                Spacer(modifier = Modifier
                    .weight(1f)
                    .height(40.dp))
            }
        }

        if (innerWidth > 0) {
            val itemWidth = (innerWidth - spacingPx * (themeCount - 1)) / themeCount
            val offsetX = animatedIndex * (itemWidth + spacingPx)

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .width(with(density) { itemWidth.toDp() })
                    .height(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = AppShape.shapes.iconSmall
                    )
            )
        }

        // 文字选项
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            ThemeManager.ThemeMode.entries.forEach { mode ->
                val isSelected = currentMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            onClick = { onModeChange(mode) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(ThemeManager.getThemeModeName(mode)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}
