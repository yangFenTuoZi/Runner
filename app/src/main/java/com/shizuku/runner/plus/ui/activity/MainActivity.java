package com.shizuku.runner.plus.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.shizuku.runner.plus.BuildConfig;
import com.shizuku.runner.plus.IUserService;
import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.UserService;
import com.shizuku.runner.plus.databinding.ActivityMainBinding;
import com.shizuku.runner.plus.databinding.FragmentHomeBinding;
import com.shizuku.runner.plus.ui.fragment.HomeFragment;

import java.util.Objects;

import rikka.core.util.ResourceUtils;
import rikka.shizuku.Shizuku;


public class MainActivity extends BaseActivity {

    private boolean b, c;
    private TextView A, B, C, D, homeRootShell;
    private int m;
    public IUserService iUserService;
    public boolean shizukuServiceState = false;
    public boolean isHome;

    //shizuku权限监听
    private final Shizuku.OnRequestPermissionResultListener onRequestPermissionResultListener = (i, i1) -> check();

    //shizuku服务状态监听
    private final Shizuku.OnBinderReceivedListener onBinderReceivedListener = () -> {
        shizukuServiceState = true;
        if (isHome)
            runOnUiThread(() -> C.setText(getString(R.string.home_service_is_running)));
    };

    private final Shizuku.OnBinderDeadListener onBinderDeadListener = () -> {
        shizukuServiceState = false;
        iUserService = null;
        if (isHome)
            runOnUiThread(() -> C.setText(getString(R.string.home_service_is_not_running)));
    };

    //检查shizuku服务及权限状态
    public void check() {

        //本函数用于检查shizuku状态，b代表shizuku是否运行，c代表shizuku是否授权
        b = true;
        c = false;
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(0);
            else c = true;
        } catch (Exception e) {
            if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED)
                c = true;
            if (e.getClass() == IllegalStateException.class) {
                b = false;
                Toast.makeText(this, getString(R.string.home_shizuku_is_not_running), Toast.LENGTH_SHORT).show();
            }
        }
        if (A != null) {
            A.setText(b ? getString(R.string.home_shizuku_is_running) : getString(R.string.home_shizuku_is_not_running));
            A.setTextColor(b ? m : 0x77ff0000);
        }
        if (B != null) {
            B.setText(c ? getString(R.string.home_shizuku_has_been_authorized) : getString(R.string.home_shizuku_is_not_authorized));
            B.setTextColor(c ? m : 0x77ff0000);
        }
        if (b && c) {
            homeRootShell.setText(Shizuku.getUid() == 0 ? "Root" : "Shell");
            if (!shizukuServiceState || iUserService == null)
                Shizuku.bindUserService(userServiceArgs, serviceConnection);
        }
    }

    //shizuku初始化注册监听
    private void initShizuku() {
        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener);
        Shizuku.addBinderReceivedListenerSticky(onBinderReceivedListener);
        Shizuku.addBinderDeadListener(onBinderDeadListener);
    }

    //shizuku服务连接状态监听
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (isHome)
                runOnUiThread(() -> D.setText(getString(R.string.home_service_is_connected)));
            if (iBinder != null && iBinder.pingBinder()) {
                iUserService = IUserService.Stub.asInterface(iBinder);
                try {
                    iUserService.releaseFile(getApplicationInfo().packageName, getApplicationInfo().nativeLibraryDir, getApplicationInfo().sourceDir);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (isHome)
                runOnUiThread(() -> D.setText(getString(R.string.home_service_is_disconnected)));
            iUserService = null;
        }
    };

    //shizuku UserService服务参数
    private final Shizuku.UserServiceArgs userServiceArgs =
            new Shizuku.UserServiceArgs(new ComponentName(BuildConfig.APPLICATION_ID, UserService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("Runner_UserService")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Fragment fragment = Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main));
        NavController navController = ((NavHostFragment) fragment).getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);
        initShizuku();

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        int color;
        if (ResourceUtils.isNightMode(getResources().getConfiguration())) {
            color = ColorUtils.blendARGB(typedValue.data, Color.BLACK, 0.8f);
        } else {
            color = ColorUtils.blendARGB(typedValue.data, Color.WHITE, 0.9f);
        }
        binding.navView.setBackgroundColor(color);
        getWindow().setNavigationBarColor(color);
    }

    @Override
    protected void onStart() {
        super.onStart();
        check();
    }

    //在APP退出时，取消注册Shizuku监听
    @Override
    public void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener);
        Shizuku.removeBinderReceivedListener(onBinderReceivedListener);
        Shizuku.removeBinderDeadListener(onBinderDeadListener);
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true);
        super.onDestroy();
    }

    //获取HomeFragment，方便子控件事件调用这个类的方法以及shizuku监听
    public void setHomeFragment(HomeFragment homeFragment) {
        FragmentHomeBinding homeBinding = homeFragment.getBinding();
        A = homeBinding.homeA;
        B = homeBinding.homeB;
        C = homeBinding.homeC;
        D = homeBinding.homeD;
        homeRootShell = homeBinding.homeRootShell;
        m = A.getCurrentTextColor();
        homeBinding.homeCard.setOnClickListener((View view) -> check());
        check();
    }

    public static String getDataPath(Context mContext) {
        String packageName;
        try {
            packageName = mContext.getPackageName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (Objects.equals(packageName, ""))
            packageName = "com.shizuku.runner.plus";
        return "/data/local/tmp/" + packageName;
    }

}