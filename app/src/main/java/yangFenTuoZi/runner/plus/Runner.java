package yangFenTuoZi.runner.plus;


import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.reflect.Field;

import rikka.shizuku.Shizuku;
import yangFenTuoZi.runner.plus.service.IService;
import yangFenTuoZi.runner.plus.service.ServiceImpl;

public class Runner {

    public static IService service;
    public static IBinder binder;
    public static boolean shizukuPermission = false;
    public static boolean shizukuStatus = false;
    public static int shizukuUid;
    public static int shizukuApiVersion;
    public static int shizukuPatchVersion;
    public static int serviceVersion;

    private static final Shizuku.UserServiceArgs userServiceArgs =
            new Shizuku.UserServiceArgs(new ComponentName(BuildConfig.APPLICATION_ID, ServiceImpl.class.getName()))
                    .daemon(true)
                    .processNameSuffix("runner_server")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);

    private static final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder != null && iBinder.pingBinder()) {
                service = IService.Stub.asInterface(binder = iBinder);
                try {
                    serviceVersion = service.version();
                } catch (RemoteException e) {
                    serviceVersion = -1;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binder = null;
            service = null;
            serviceVersion = -1;
        }
    };

    private static final Shizuku.OnRequestPermissionResultListener onRequestPermissionResultListener = (requestCode, grantResult) -> {
        if (requestCode != 7890) return;
        shizukuPermission = grantResult == PackageManager.PERMISSION_GRANTED;
        shizukuStatus = Shizuku.pingBinder();
        tryBindService();
    };

    private static final Shizuku.OnBinderReceivedListener onBinderReceivedListener = () -> {
        shizukuStatus = true;
        shizukuUid = Shizuku.getUid();
        shizukuApiVersion = Shizuku.getVersion();
        try {
            Field serverPatchVersionField = Shizuku.class.getDeclaredField("serverPatchVersion");
            serverPatchVersionField.setAccessible(true);
            shizukuPatchVersion = serverPatchVersionField.getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            shizukuPatchVersion = 0;
        }
        if (shizukuPatchVersion < 0) shizukuPatchVersion = 0;
        tryBindService();
    };

    private static final Shizuku.OnBinderDeadListener onBinderDeadListener = () -> {
        shizukuStatus = false;
        serviceVersion = shizukuUid = shizukuApiVersion = shizukuPatchVersion = -1;
        binder = null;
        service = null;
    };

    public static void refreshStatus() {
        shizukuStatus = Shizuku.pingBinder();
        try {
            shizukuPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (RuntimeException e) {
            shizukuPermission = App.getInstance().checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED;
        }
        tryBindService();
    }

    public static void tryBindService() {
        if (shizukuStatus && shizukuPermission && !pingServer())
            Shizuku.bindUserService(userServiceArgs, serviceConnection);
    }

    public static void tryUnbindService(boolean remove) {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, remove);
    }

    public static void requestPermission() {
        Shizuku.requestPermission(7890);
    }

    public static boolean pingServer() {
        return binder != null && binder.pingBinder();
    }

    public static void init() {
        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener);
        Shizuku.addBinderReceivedListenerSticky(onBinderReceivedListener);
        Shizuku.addBinderDeadListener(onBinderDeadListener);
    }

    public static void remove() {
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener);
        Shizuku.removeBinderReceivedListener(onBinderReceivedListener);
        Shizuku.removeBinderDeadListener(onBinderDeadListener);
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, false);
    }
}