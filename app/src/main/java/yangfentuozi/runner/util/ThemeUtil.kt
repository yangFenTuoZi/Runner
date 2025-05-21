package yangfentuozi.runner.util

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import rikka.core.util.ResourceUtils
import yangfentuozi.runner.App
import yangfentuozi.runner.R


object ThemeUtil {
    private val colorThemeMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()

    const val MODE_NIGHT_FOLLOW_SYSTEM: String = "MODE_NIGHT_FOLLOW_SYSTEM"
    const val MODE_NIGHT_NO: String = "MODE_NIGHT_NO"
    const val MODE_NIGHT_YES: String = "MODE_NIGHT_YES"

    init {
        colorThemeMap.put("MATERIAL_RED", R.style.ThemeOverlay_MaterialRed)
        colorThemeMap.put("MATERIAL_PINK", R.style.ThemeOverlay_MaterialPink)
        colorThemeMap.put("MATERIAL_PURPLE", R.style.ThemeOverlay_MaterialPurple)
        colorThemeMap.put("MATERIAL_DEEP_PURPLE", R.style.ThemeOverlay_MaterialDeepPurple)
        colorThemeMap.put("MATERIAL_INDIGO", R.style.ThemeOverlay_MaterialIndigo)
        colorThemeMap.put("MATERIAL_BLUE", R.style.ThemeOverlay_MaterialBlue)
        colorThemeMap.put("MATERIAL_LIGHT_BLUE", R.style.ThemeOverlay_MaterialLightBlue)
        colorThemeMap.put("MATERIAL_CYAN", R.style.ThemeOverlay_MaterialCyan)
        colorThemeMap.put("MATERIAL_TEAL", R.style.ThemeOverlay_MaterialTeal)
        colorThemeMap.put("MATERIAL_GREEN", R.style.ThemeOverlay_MaterialGreen)
        colorThemeMap.put("MATERIAL_LIGHT_GREEN", R.style.ThemeOverlay_MaterialLightGreen)
        colorThemeMap.put("MATERIAL_LIME", R.style.ThemeOverlay_MaterialLime)
        colorThemeMap.put("MATERIAL_YELLOW", R.style.ThemeOverlay_MaterialYellow)
        colorThemeMap.put("MATERIAL_AMBER", R.style.ThemeOverlay_MaterialAmber)
        colorThemeMap.put("MATERIAL_ORANGE", R.style.ThemeOverlay_MaterialOrange)
        colorThemeMap.put("MATERIAL_DEEP_ORANGE", R.style.ThemeOverlay_MaterialDeepOrange)
        colorThemeMap.put("MATERIAL_BROWN", R.style.ThemeOverlay_MaterialBrown)
        colorThemeMap.put("MATERIAL_BLUE_GREY", R.style.ThemeOverlay_MaterialBlueGrey)
    }

    private const val THEME_DEFAULT = "DEFAULT"
    private const val THEME_BLACK = "BLACK"

    private val isBlackNightTheme: Boolean
        get() = App.preferences.getBoolean("black_dark_theme", false)

    val isSystemAccent: Boolean
        get() = DynamicColors.isDynamicColorAvailable() && App.preferences.getBoolean(
            "follow_system_accent",
            true
        )

    fun getNightTheme(context: Context): String {
        if (isBlackNightTheme
            && ResourceUtils.isNightMode(context.resources.configuration)
        ) return THEME_BLACK

        return THEME_DEFAULT
    }

    @StyleRes
    fun getNightThemeStyleRes(context: Context): Int {
        return when (getNightTheme(context)) {
            THEME_BLACK -> R.style.ThemeOverlay_Black
            THEME_DEFAULT -> R.style.ThemeOverlay
            else -> R.style.ThemeOverlay
        }
    }

    val colorTheme: String
        get() {
            if (isSystemAccent) {
                return "SYSTEM"
            }
            return App.preferences.getString("theme_color", "COLOR_BLUE")!!
        }

    @get:StyleRes
    val colorThemeStyleRes: Int
        get() {
            val theme = colorThemeMap[colorTheme]
            if (theme == null) {
                return R.style.ThemeOverlay_MaterialBlue
            }
            return theme
        }

    fun getDarkTheme(mode: String): Int {
        return when (mode) {
            MODE_NIGHT_FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_YES
            MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    val darkTheme: Int
        get() = getDarkTheme(
            App.preferences.getString(
                "dark_theme",
                MODE_NIGHT_FOLLOW_SYSTEM
            )!!
        )
}