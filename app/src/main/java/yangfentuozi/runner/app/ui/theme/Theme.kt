package yangfentuozi.runner.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.android.material.color.DynamicColors
import yangfentuozi.runner.app.App

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2196F3),
    secondary = androidx.compose.ui.graphics.Color(0xFF03A9F4),
    tertiary = androidx.compose.ui.graphics.Color(0xFF00BCD4)
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2196F3),
    secondary = androidx.compose.ui.graphics.Color(0xFF03A9F4),
    tertiary = androidx.compose.ui.graphics.Color(0xFF00BCD4)
)

private val BlackDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2196F3),
    secondary = androidx.compose.ui.graphics.Color(0xFF03A9F4),
    tertiary = androidx.compose.ui.graphics.Color(0xFF00BCD4),
    background = androidx.compose.ui.graphics.Color.Black,
    surface = androidx.compose.ui.graphics.Color.Black
)

@Composable
fun RunnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = DynamicColors.isDynamicColorAvailable() && 
        App.preferences.getBoolean("follow_system_accent", true),
    blackDarkTheme: Boolean = App.preferences.getBoolean("black_dark_theme", false),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                if (blackDarkTheme) {
                    // 动态颜色与黑色主题的组合
                    dynamicDarkColorScheme(context).copy(
                        background = androidx.compose.ui.graphics.Color.Black,
                        surface = androidx.compose.ui.graphics.Color.Black
                    )
                } else {
                    dynamicDarkColorScheme(context)
                }
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> {
            if (blackDarkTheme) {
                BlackDarkColorScheme
            } else {
                DarkColorScheme
            }
        }
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

