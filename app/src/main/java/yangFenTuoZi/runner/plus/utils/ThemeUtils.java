package yangFenTuoZi.runner.plus.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StyleRes;

import rikka.core.util.ResourceUtils;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.info.Info;

public class ThemeUtils {

    @StyleRes
    public static int getTheme(Context context) {
        return getTheme(isDark(context));
    }

    @StyleRes
    public static int getTheme(boolean isDark) {
        return isDark ? R.style.Theme : R.style.Theme_Light;
    }

    public static boolean isDark(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Info.APPLICATION_ID + "_preferences", 0);
        String dark_theme = sharedPreferences.getString("dark_theme", "MODE_NIGHT_FOLLOW_SYSTEM");
        return switch (dark_theme) {
            case "MODE_NIGHT_FOLLOW_SYSTEM" ->
                    ResourceUtils.isNightMode(context.getResources().getConfiguration());
            case "MODE_NIGHT_YES" -> true;
            default -> false;
        };
    }
}
