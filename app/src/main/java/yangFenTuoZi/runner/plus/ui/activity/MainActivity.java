package yangFenTuoZi.runner.plus.ui.activity;

import static yangFenTuoZi.runner.plus.utils.ExceptionUtils.throwableToDialog;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;

import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import rikka.material.app.LocaleDelegate;
import yangFenTuoZi.runner.plus.BuildConfig;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.Runner;
import yangFenTuoZi.runner.plus.base.BaseActivity;
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder;
import yangFenTuoZi.runner.plus.databinding.ActivityMainBinding;
import yangFenTuoZi.runner.plus.databinding.DialogAboutBinding;
import yangFenTuoZi.runner.plus.ui.dialog.BlurBehindDialogBuilder;


public class MainActivity extends BaseActivity {

    private AppBarLayout mAppBar;
    private MaterialToolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_terminal, R.id.navigation_processes, R.id.navigation_settings).build();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = ((NavHostFragment) fragment).getNavController();
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        binding.appBar.setLiftable(true);
        binding.toolbar.inflateMenu(R.menu.menu_home);
        if (this instanceof MenuProvider self) {
            self.onPrepareMenu(binding.toolbar.getMenu());
        }
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_stop_server) {
                if (!Runner.pingServer()) return true;
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.confirm_stop_server)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> new Thread(() -> {
                            try {
                                Runner.tryUnbindService(true);
                            } catch (Exception e) {
                                throwableToDialog(this, e);
                            }
                        }).start())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            } else if (item.getItemId() == R.id.menu_about) {
                showAbout();
            }
            return true;
        });

        mToolbar = binding.toolbar;
        mAppBar = binding.appBar;
    }

    private void showAbout() {
        DialogAboutBinding binding = DialogAboutBinding.inflate(getLayoutInflater(), null, false);
        binding.designAboutTitle.setText(R.string.app_name);
        binding.designAboutInfo.setMovementMethod(LinkMovementMethod.getInstance());
        binding.designAboutInfo.setText(HtmlCompat.fromHtml(getString(
                R.string.about_view_source_code,
                "<b><a href=\"https://github.com/yangFenTuoZi/Runner\">GitHub</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
        binding.designAboutVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        try {
            new BlurBehindDialogBuilder(this)
                    .setView(binding.getRoot()).show();
        } catch (BaseDialogBuilder.DialogShowException ignored) {
        }
    }

    public AppBarLayout getAppBar() {
        return mAppBar;
    }

    public MaterialToolbar getToolbar() {
        return mToolbar;
    }
}