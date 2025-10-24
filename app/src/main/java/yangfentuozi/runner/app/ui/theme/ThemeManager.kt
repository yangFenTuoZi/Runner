package yangfentuozi.runner.app.ui.theme

import androidx.annotation.StringRes
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import yangfentuozi.runner.R
import yangfentuozi.runner.app.App

/**
 * 主题管理器 - 管理应用的主题模式
 * 支持三种模式:
 * - LIGHT: 永昼模式(总是浅色主题)
 * - DARK: 永夜模式(总是深色主题)  
 * - SYSTEM: 跟随系统(自动切换)
 */
object ThemeManager {
    
    enum class ThemeMode {
        LIGHT,      // 永昼
        DARK,       // 永夜
        SYSTEM      // 跟随系统
    }
    
    private const val PREF_KEY_THEME_MODE = "theme_mode_v2"
    
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    
    /**
     * 设置主题模式
     * @param mode 主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value != mode) {
            _themeMode.value = mode
            saveThemeMode(mode)
        }
    }
    
    /**
     * 从 SharedPreferences 加载主题模式
     */
    private fun loadThemeMode(): ThemeMode {
        val savedMode = App.preferences.getString(PREF_KEY_THEME_MODE, null)
        return when (savedMode) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> {
                // 兼容旧版本的设置
                val oldMode = App.preferences.getString("dark_theme", null)
                when (oldMode) {
                    "MODE_NIGHT_NO" -> ThemeMode.LIGHT
                    "MODE_NIGHT_YES" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            }
        }
    }
    
    /**
     * 保存主题模式到 SharedPreferences
     */
    private fun saveThemeMode(mode: ThemeMode) {
        App.preferences.edit {
            putString(PREF_KEY_THEME_MODE, mode.name)
        }
    }
    
    /**
     * 获取当前主题模式的显示名称
     */
    @StringRes
    fun getThemeModeName(mode: ThemeMode): Int {
        return when (mode) {
            ThemeMode.LIGHT -> R.string.light
            ThemeMode.DARK -> R.string.dark
            ThemeMode.SYSTEM -> R.string.follow_system
        }
    }
}

