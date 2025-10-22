package yangfentuozi.runner.app.ui.screens.main.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.color.DynamicColors
import yangfentuozi.runner.R
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.data.BackupManager
import yangfentuozi.runner.app.ui.activity.envmanage.EnvManageActivity
import yangfentuozi.runner.app.util.ThemeUtil

@Composable
fun SettingsScreen(
    activity: ComponentActivity,
    onShowAbout: () -> Unit
) {
    val context = LocalContext.current
    var showDarkThemeDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    val blackDarkTheme = remember { 
        mutableStateOf(App.preferences.getBoolean("black_dark_theme", false)) 
    }
    val followSystemAccent = remember {
        mutableStateOf(App.preferences.getBoolean("follow_system_accent", true))
    }
    val forceKill = remember {
        mutableStateOf(App.preferences.getBoolean("force_kill", false))
    }
    val killChildProcesses = remember {
        mutableStateOf(App.preferences.getBoolean("kill_child_processes", false))
    }

    var lastBackupArgs by remember { mutableStateOf<BooleanArray?>(null) }

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
            showImportDialog = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // 主题设置
        item {
            PreferenceCategory(title = stringResource(R.string.theme))
        }

        item {
            PreferenceItem(
                title = stringResource(R.string.dark_theme),
                summary = getDarkThemeString(),
                onClick = { showDarkThemeDialog = true }
            )
        }

        if (DynamicColors.isDynamicColorAvailable()) {
            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.theme_color_system),
                    checked = followSystemAccent.value,
                    onCheckedChange = {
                        followSystemAccent.value = it
                        App.preferences.edit().putBoolean("follow_system_accent", it).apply()
                        activity.recreate()
                    }
                )
            }
        }

        if (!followSystemAccent.value || !DynamicColors.isDynamicColorAvailable()) {
            item {
                PreferenceItem(
                    title = stringResource(R.string.theme_color),
                    summary = getColorThemeString(),
                    onClick = { showColorThemeDialog = true }
                )
            }
        }

        item {
            SwitchPreferenceItem(
                title = stringResource(R.string.pure_black_dark_theme),
                summary = stringResource(R.string.pure_black_dark_theme_summary),
                checked = blackDarkTheme.value,
                onCheckedChange = {
                    blackDarkTheme.value = it
                    App.preferences.edit().putBoolean("black_dark_theme", it).apply()
                    activity.recreate()
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
                onClick = { showBackupDialog = true }
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
                checked = forceKill.value,
                onCheckedChange = {
                    forceKill.value = it
                    App.preferences.edit().putBoolean("force_kill", it).apply()
                }
            )
        }

        item {
            SwitchPreferenceItem(
                title = stringResource(R.string.kill_child_processes),
                summary = stringResource(R.string.kill_child_processes_summary),
                checked = killChildProcesses.value,
                onCheckedChange = {
                    killChildProcesses.value = it
                    App.preferences.edit().putBoolean("kill_child_processes", it).apply()
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
                onClick = onShowAbout
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
            onDismiss = { showDarkThemeDialog = false },
            onSelect = { mode ->
                App.preferences.edit().putString("dark_theme", mode).apply()
                AppCompatDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme(mode))
                showDarkThemeDialog = false
            }
        )
    }

    if (showColorThemeDialog) {
        ColorThemeDialog(
            onDismiss = { showColorThemeDialog = false },
            onSelect = { color ->
                App.preferences.edit().putString("theme_color", color).apply()
                activity.recreate()
                showColorThemeDialog = false
            }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            onConfirm = { args ->
                lastBackupArgs = args
                saveFileLauncher.launch("runner_backup_${System.currentTimeMillis()}.tar")
                showBackupDialog = false
            }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
    onSelect: (String) -> Unit
) {
    val current = App.preferences.getString("dark_theme", ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM)!!
    val options = listOf(
        ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM to stringResource(R.string.follow_system),
        ThemeUtil.MODE_NIGHT_YES to stringResource(R.string.dark),
        ThemeUtil.MODE_NIGHT_NO to stringResource(R.string.light)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dark_theme)) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == value,
                            onClick = { onSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
private fun ColorThemeDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val current = App.preferences.getString("theme_color", "COLOR_BLUE")!!
    val colors = listOf(
        "MATERIAL_RED" to stringResource(R.string.color_red),
        "MATERIAL_PINK" to stringResource(R.string.color_pink),
        "MATERIAL_PURPLE" to stringResource(R.string.color_purple),
        "MATERIAL_DEEP_PURPLE" to stringResource(R.string.color_deep_purple),
        "MATERIAL_INDIGO" to stringResource(R.string.color_indigo),
        "MATERIAL_BLUE" to stringResource(R.string.color_blue),
        "MATERIAL_LIGHT_BLUE" to stringResource(R.string.color_light_blue),
        "MATERIAL_CYAN" to stringResource(R.string.color_cyan),
        "MATERIAL_TEAL" to stringResource(R.string.color_teal),
        "MATERIAL_GREEN" to stringResource(R.string.color_green),
        "MATERIAL_LIGHT_GREEN" to stringResource(R.string.color_light_green),
        "MATERIAL_LIME" to stringResource(R.string.color_lime),
        "MATERIAL_YELLOW" to stringResource(R.string.color_yellow),
        "MATERIAL_AMBER" to stringResource(R.string.color_amber),
        "MATERIAL_ORANGE" to stringResource(R.string.color_orange),
        "MATERIAL_DEEP_ORANGE" to stringResource(R.string.color_deep_orange),
        "MATERIAL_BROWN" to stringResource(R.string.color_brown),
        "MATERIAL_BLUE_GREY" to stringResource(R.string.color_blue_grey)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_color)) },
        text = {
            LazyColumn {
                items(colors.size) { index ->
                    val (value, label) = colors[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == value,
                            onClick = { onSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun getDarkThemeString(): String {
    return when (App.preferences.getString("dark_theme", ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM)) {
        ThemeUtil.MODE_NIGHT_YES -> stringResource(R.string.dark)
        ThemeUtil.MODE_NIGHT_NO -> stringResource(R.string.light)
        else -> stringResource(R.string.follow_system)
    }
}

@Composable
private fun getColorThemeString(): String {
    val colorTheme = App.preferences.getString("theme_color", "COLOR_BLUE")!!
    return when (colorTheme) {
        "MATERIAL_RED" -> stringResource(R.string.color_red)
        "MATERIAL_PINK" -> stringResource(R.string.color_pink)
        "MATERIAL_PURPLE" -> stringResource(R.string.color_purple)
        "MATERIAL_DEEP_PURPLE" -> stringResource(R.string.color_deep_purple)
        "MATERIAL_INDIGO" -> stringResource(R.string.color_indigo)
        "MATERIAL_BLUE" -> stringResource(R.string.color_blue)
        "MATERIAL_LIGHT_BLUE" -> stringResource(R.string.color_light_blue)
        "MATERIAL_CYAN" -> stringResource(R.string.color_cyan)
        "MATERIAL_TEAL" -> stringResource(R.string.color_teal)
        "MATERIAL_GREEN" -> stringResource(R.string.color_green)
        "MATERIAL_LIGHT_GREEN" -> stringResource(R.string.color_light_green)
        "MATERIAL_LIME" -> stringResource(R.string.color_lime)
        "MATERIAL_YELLOW" -> stringResource(R.string.color_yellow)
        "MATERIAL_AMBER" -> stringResource(R.string.color_amber)
        "MATERIAL_ORANGE" -> stringResource(R.string.color_orange)
        "MATERIAL_DEEP_ORANGE" -> stringResource(R.string.color_deep_orange)
        "MATERIAL_BROWN" -> stringResource(R.string.color_brown)
        "MATERIAL_BLUE_GREY" -> stringResource(R.string.color_blue_grey)
        else -> stringResource(R.string.color_blue)
    }
}

