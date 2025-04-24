package yangFenTuoZi.runner.plus.utils

import android.content.Context
import androidx.annotation.StyleRes
import rikka.core.util.ResourceUtils
import yangFenTuoZi.runner.plus.BuildConfig
import yangFenTuoZi.runner.plus.R

object ThemeUtils {
    @StyleRes
    fun getTheme(context: Context): Int {
        return getTheme(isDark(context))
    }

    @StyleRes
    fun getTheme(isDark: Boolean): Int {
        return if (isDark) R.style.Theme else R.style.Theme_Light
    }

    fun isDark(context: Context): Boolean {
        val sharedPreferences =
            context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", 0)
        val darkTheme: String =
            sharedPreferences.getString("dark_theme", "MODE_NIGHT_FOLLOW_SYSTEM")!!
        return when (darkTheme) {
            "MODE_NIGHT_FOLLOW_SYSTEM" -> ResourceUtils.isNightMode(
                context.resources.configuration
            )

            "MODE_NIGHT_YES" -> true
            else -> false
        }
    }
}