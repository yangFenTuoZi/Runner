package yangFenTuoZi.runner.plus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import yangFenTuoZi.runner.plus.receiver.OnServiceConnectListener;
import yangFenTuoZi.runner.plus.receiver.OnServiceDisconnectListener;
import yangFenTuoZi.runner.plus.server.IService;
import yangFenTuoZi.runner.plus.server.Logger;
import yangFenTuoZi.runner.plus.server.Server;
import yangFenTuoZi.runner.plus.ui.activity.CrashReportActivity;
import yangFenTuoZi.runner.plus.utils.ThemeUtils;

public class App extends Application implements Thread.UncaughtExceptionHandler {
    public static IService iService;
    private static final List<OnServiceConnectListener> mConnectListeners;
    private static final List<OnServiceDisconnectListener> mDisconnectListeners;
    private static Timer timer, timer2;

    private final DynamicColorsOptions mDynamicColorsOptions = new DynamicColorsOptions.Builder().build();
    private final List<Activity> activities = new LinkedList<>();
    public int isDark = -1;

    static {
        mConnectListeners = new LinkedList<>();
        mDisconnectListeners = new LinkedList<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (pingServer())
                    return;
                try {
                    Socket socket = new Socket("localhost", Server.PORT);
                    OutputStream out = socket.getOutputStream();
                    out.write("sendBinderToApp\n".getBytes());
                    out.close();
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }, 0L, 1000L);

        boolean isDark = ThemeUtils.isDark(this);
        this.isDark = isDark ? 1 : 0;
        setTheme(ThemeUtils.getTheme(isDark));
        DynamicColors.applyToActivitiesIfAvailable(this, mDynamicColorsOptions);
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    public void removeActivity(Activity activity) {
        activities.remove(activity);
    }

    public void finishApp() {
        for (Activity activity : activities)
            activity.finish();
        activities.clear();
    }

    public DynamicColorsOptions getDynamicColorsOptions() {
        return mDynamicColorsOptions;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (timer2 != null)
            timer2.cancel();
        if (timer != null)
            timer.cancel();
    }

    public static boolean pingServer() {
        return iService != null && iService.asBinder().pingBinder();
    }

    public static void onServerReceive(IBinder binder) {
        App.iService = IService.Stub.asInterface(binder);
        Log.i("App", pingServer() ? "Server Connect" : "Server Disconnect");
        if (timer2 != null)
            timer2.cancel();
        if (timer != null)
            timer.cancel();
        if (App.pingServer()) {
            timer2 = new Timer();
            timer2.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!pingServer()) {
                        onServerReceive(null);
                    }
                }
            }, 0L, 1000L);
            for (OnServiceConnectListener listener : mConnectListeners) {
                listener.onServiceConnect(iService);
            }
        } else {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Socket socket = new Socket("localhost", Server.PORT);
                        OutputStream out = socket.getOutputStream();
                        out.write("sendBinderToApp\n".getBytes());
                        out.close();
                        socket.close();
                    } catch (Exception ignored) {
                    }
                }
            }, 0L, 1000L);
            for (OnServiceDisconnectListener listener : mDisconnectListeners) {
                listener.onServiceDisconnect();
            }
        }
    }

    public static void addOnServiceConnectListener(@NonNull OnServiceConnectListener onServiceConnectListener) {
        mConnectListeners.add(onServiceConnectListener);
    }

    public static void removeOnServiceConnectListener(@NonNull OnServiceConnectListener onServiceConnectListener) {
        mConnectListeners.remove(onServiceConnectListener);
    }

    public static void addOnServiceDisconnectListener(@NonNull OnServiceDisconnectListener onServiceDisconnectListener) {
        mDisconnectListeners.add(onServiceDisconnectListener);
    }

    public static void removeOnServiceDisconnectListener(@NonNull OnServiceDisconnectListener onServiceDisconnectListener) {
        mDisconnectListeners.remove(onServiceDisconnectListener);
    }

    @SuppressLint("WrongConstant")
    private void crashHandler(@NonNull Thread t, @NonNull Throwable e) {

        String fileName = "runnerCrash-" + System.currentTimeMillis() + ".log";
        File file;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file = new File(dir, fileName);
        } else {
            file = getExternalFilesDir(fileName);
        }
        assert file != null;

        startActivity(new Intent(this, CrashReportActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("crash_info", Logger.getStackTraceString(e))
                .putExtra("crash_file", file.getAbsolutePath()));
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        new Thread(() -> {
            Looper.prepare();
            crashHandler(t, e);
            Looper.loop();
        }).start();
    }
}
