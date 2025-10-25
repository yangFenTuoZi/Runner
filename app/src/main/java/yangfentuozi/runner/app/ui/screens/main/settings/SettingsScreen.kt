package yangfentuozi.runner.app.ui.screens.main.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.color.DynamicColors
import yangfentuozi.runner.BuildConfig
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.data.BackupManager
import yangfentuozi.runner.app.ui.activity.envmanage.EnvManageActivity
import yangfentuozi.runner.app.ui.components.BeautifulCard
import yangfentuozi.runner.app.ui.theme.ThemeManager
import yangfentuozi.runner.app.ui.viewmodels.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    activity: ComponentActivity,
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

@Composable
private fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
    )
}

@Composable
private fun PreferenceItem(
    title: String,
    summary: String? = null,
    onClick: () -> Unit
) {
    BeautifulCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwitchPreferenceItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    BeautifulCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun DarkThemeDialog(
    onDismiss: () -> Unit,
    onSelect: (ThemeManager.ThemeMode) -> Unit
) {
    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    val options = listOf(
        ThemeManager.ThemeMode.SYSTEM to stringResource(R.string.follow_system),
        ThemeManager.ThemeMode.DARK to stringResource(R.string.dark),
        ThemeManager.ThemeMode.LIGHT to stringResource(R.string.light)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dark_theme)) },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentThemeMode == mode,
                            onClick = { onSelect(mode) }
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun BackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (BooleanArray) -> Unit
) {
    var appSettings by remember { mutableStateOf(true) }
    var dataDb by remember { mutableStateOf(true) }
    var termHome by remember { mutableStateOf(true) }
    var termUsr by remember { mutableStateOf(true) }

    val restricted = !Runner.pingServer()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_backup_data)) },
        text = {
            Column {
                CheckboxItem(
                    title = stringResource(R.string.app_settings),
                    checked = appSettings,
                    onCheckedChange = { appSettings = it },
                    enabled = true
                )
                CheckboxItem(
                    title = stringResource(R.string.data_db),
                    checked = dataDb,
                    onCheckedChange = { dataDb = it },
                    enabled = !restricted
                )
                CheckboxItem(
                    title = stringResource(R.string.term_home),
                    checked = termHome,
                    onCheckedChange = { termHome = it },
                    enabled = !restricted
                )
                CheckboxItem(
                    title = stringResource(R.string.term_usr),
                    checked = termUsr,
                    onCheckedChange = { termUsr = it },
                    enabled = !restricted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(booleanArrayOf(restricted, appSettings, dataDb, termHome, termUsr))
                }
            ) {
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

@Composable
private fun CheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.5f
            )
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        confirmButton = { },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {

                // 应用图标
                val drawable = context.applicationInfo.loadIcon(context.packageManager)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .drawBehind {
                            drawIntoCanvas { canvas ->
                                drawable?.let {
                                    it.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
                                    it.draw(canvas.nativeCanvas)
                                }
                            }
                        }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 文本信息列
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 应用名称
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal
                    )

                    // 版本信息
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 源码链接
                    val sourceCodeText = buildAnnotatedString {
                        val template = stringResource(R.string.about_view_source_code)
                        val parts = template.split("%1\$s")

                        append(parts.getOrNull(0) ?: "")

                        pushStringAnnotation(
                            tag = "URL",
                            annotation = "https://github.com/yangFenTuoZi/Runner"
                        )
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("GitHub")
                        }
                        pop()

                        append(parts.getOrNull(1) ?: "")
                    }

                    Text(
                        text = sourceCodeText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            sourceCodeText.getStringAnnotations(
                                tag = "URL",
                                start = 0,
                                end = sourceCodeText.length
                            ).firstOrNull()?.let { annotation ->
                                val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    )
}

