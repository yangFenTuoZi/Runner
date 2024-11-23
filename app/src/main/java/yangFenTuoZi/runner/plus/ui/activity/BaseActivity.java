package yangFenTuoZi.runner.plus.ui.activity;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;

import rikka.core.util.ResourceUtils;
import rikka.material.app.MaterialActivity;
import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.info.Info;

public class BaseActivity extends MaterialActivity {

    public short isDark = -1;
    public App mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mApp = (App) getApplication();
        //设置主题
        if (mApp.isDark != -1) {
            isDark = mApp.isDark;
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(Info.APPLICATION_ID + "_preferences", 0);
            String dark_theme = sharedPreferences.getString("dark_theme", "MODE_NIGHT_FOLLOW_SYSTEM");
            isDark = switch (dark_theme) {
                case "MODE_NIGHT_FOLLOW_SYSTEM" ->
                        ResourceUtils.isNightMode(getResources().getConfiguration()) ? (short) 1 : (short) 0;
                case "MODE_NIGHT_YES" -> 1;
                default -> 0;
            };
            mApp.isDark = isDark;
        }
        setTheme(isDark == 1 ? R.style.Theme : R.style.Theme_Light);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mApp.isDark == -1)
            mApp.isDark = isDark;
        if (mApp.isDark != isDark) {
            recreate();
        }
    }

    @Override
    public void onApplyUserThemeResource(@NonNull Resources.Theme theme, boolean isDecorView) {
        theme.applyStyle(rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true);
    }

    @Override
    public void onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars();

        //设置状态栏导航栏透明
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }
}
