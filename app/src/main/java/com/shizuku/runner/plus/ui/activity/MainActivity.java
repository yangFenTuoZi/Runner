package com.shizuku.runner.plus.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

import rikka.shizuku.Shizuku;


public class MainActivity extends BaseActivity {

    private boolean b, c;
    private TextView A, B, C, D, homeRootShell;
    private int m;
    private ActivityMainBinding binding;
    private HomeFragment homeFragment;
    protected mHandler mHandler = new mHandler(this);
    public IUserService iUserService;
    private boolean shizukuServiceState = false;
    public static String packageName;

    public static class mHandler extends Handler {
        private final MainActivity mainActivity;

        public mHandler(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0)
                mainActivity.C.setText((String) msg.obj);
            else
                mainActivity.D.setText((String) msg.obj);
        }
    }

    @Override
    public void onDestroy() {
        //在APP退出时，取消注册Shizuku监听
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener);
        Shizuku.removeBinderReceivedListener(onBinderReceivedListener);
        Shizuku.removeBinderDeadListener(onBinderDeadListener);
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true);
        super.onDestroy();
    }

    private final Shizuku.OnRequestPermissionResultListener onRequestPermissionResultListener = (i, i1) -> check();

    private final Shizuku.OnBinderReceivedListener onBinderReceivedListener = () -> {
        shizukuServiceState = true;
        Message msg = new Message();
        msg.what = 0;
        msg.obj = getString(R.string.home_service_is_running);
        mHandler.sendMessage(msg);
    };

    private final Shizuku.OnBinderDeadListener onBinderDeadListener = () -> {
        shizukuServiceState = false;
        iUserService = null;
        Message msg = new Message();
        msg.what = 0;
        msg.obj = getString(R.string.home_service_is_not_running);
        mHandler.sendMessage(msg);
    };

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
        A.setText(b ? getString(R.string.home_shizuku_is_running) : getString(R.string.home_shizuku_is_not_running));
        A.setTextColor(b ? m : 0x77ff0000);
        B.setText(c ? getString(R.string.home_shizuku_has_been_authorized) : getString(R.string.home_shizuku_is_not_authorized));
        B.setTextColor(c ? m : 0x77ff0000);
        if (b && c) {
            homeRootShell.setText(Shizuku.getUid() == 0 ? "Root" : "Shell");
            if (!shizukuServiceState || iUserService == null)
                Shizuku.bindUserService(userServiceArgs, serviceConnection);
        }
    }

    private void initShizuku() {
        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener);
        Shizuku.addBinderReceivedListenerSticky(onBinderReceivedListener);
        Shizuku.addBinderDeadListener(onBinderDeadListener);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Message msg = new Message();
            msg.what = 1;
            msg.obj = getString(R.string.home_service_is_connected);
            mHandler.sendMessage(msg);
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
            Message msg = new Message();
            msg.what = 1;
            msg.obj = getString(R.string.home_service_is_disconnected);
            mHandler.sendMessage(msg);
            iUserService = null;
        }
    };

    private final Shizuku.UserServiceArgs userServiceArgs =
            new Shizuku.UserServiceArgs(new ComponentName(BuildConfig.APPLICATION_ID, UserService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("adb_service")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Fragment fragment = Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main));
        NavController navController = ((NavHostFragment) fragment).getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);
        initShizuku();
        packageName = getApplicationInfo().packageName;
    }

    @Override
    protected void onResume() {
        super.onResume();
        check();

    }
    public void setHomeFragment(HomeFragment homeFragment) {
        this.homeFragment = homeFragment;
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

    public void startBinManager(View view) {
        Intent intent = new Intent(this, FileMangerActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("path", getDataPath(this) + "/bin");
        intent.putExtra("bundle", bundle);
        startActivity(intent);
    }

    public void startLibManager(View view) {
        Intent intent = new Intent(this, FileMangerActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("path", getDataPath(this) + "/lib");
        intent.putExtra("bundle", bundle);
        startActivity(intent);
    }


}