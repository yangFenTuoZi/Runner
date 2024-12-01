package yangFenTuoZi.runner.plus.ui.activity;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;

import com.google.android.material.color.DynamicColors;

import rikka.material.app.MaterialActivity;
import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.utils.ThemeUtils;

public class BaseActivity extends MaterialActivity {

    public boolean isDialogShow = false;

    public int isDark = -1;
    public App mApp;

    public void setupToolbar(Toolbar toolbar, View tipsView, int title) {
        setupToolbar(toolbar, tipsView, getString(title), -1);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, int title, int menu) {
        setupToolbar(toolbar, tipsView, getString(title), menu);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, String title, int menu) {
        toolbar.setTitle(title);
        toolbar.setTooltipText(title);
        if (tipsView != null) tipsView.setTooltipText(title);
        if (menu != -1) {
            toolbar.inflateMenu(menu);
            if (this instanceof MenuProvider self) {
                self.onPrepareMenu(toolbar.getMenu());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mApp = (App) getApplication();
        mApp.addActivity(this);
        //设置主题
        if (mApp.isDark != -1) {
            isDark = mApp.isDark;
        } else {
            isDark = ThemeUtils.isDark(this) ? 1 : 0;
            mApp.isDark = isDark;
        }
        setTheme(ThemeUtils.getTheme(isDark == 1));
        DynamicColors.applyToActivityIfAvailable(this, mApp.getDynamicColorsOptions());
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        mApp.removeActivity(this);
        super.onDestroy();
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
