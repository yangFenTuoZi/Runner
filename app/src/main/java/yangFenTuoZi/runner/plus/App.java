package yangFenTuoZi.runner.plus;

import android.app.Application;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.google.android.material.color.DynamicColors;

import yangFenTuoZi.runner.plus.receiver.OnServiceConnectListener;
import yangFenTuoZi.runner.plus.receiver.OnServiceDisconnectListener;
import yangFenTuoZi.runner.plus.server.IService;
import yangFenTuoZi.runner.plus.server.Server;

import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class App extends Application {
    public static IService iService;
    private static final List<OnServiceConnectListener> mConnectListeners;
    private static final List<OnServiceDisconnectListener> mDisconnectListeners;
    private static Timer timer, timer2;

    static {
        mConnectListeners = new ArrayList<>();
        mDisconnectListeners = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
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
}
