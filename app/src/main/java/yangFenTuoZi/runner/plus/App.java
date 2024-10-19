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

public class App extends Application {
    public static IService iService;
    public static IBinder binder;
    private static final List<OnServiceConnectListener> mConnectListeners;
    private static final List<OnServiceDisconnectListener> mDisconnectListeners;
    private Thread t1;
    private boolean stopListen = false;

    static {
        mConnectListeners = new ArrayList<>();
        mDisconnectListeners = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        t1 = new Thread(() -> {
            try {
                Thread.sleep(1000);
                while (!pingServer() && !stopListen) {
                    Socket socket = new Socket("localhost", Server.PORT);
                    new Thread(() -> {
                        stopListen = true;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        stopListen = false;
                    }).start();
                    OutputStream out = socket.getOutputStream();
                    out.write("sendBinderToApp\n".getBytes());
                    out.close();
                    socket.close();
                }
            } catch (Exception ignored) {
            }
        });
        t1.start();
    }

    @Override
    public void onTerminate() {
        t1.interrupt();
        super.onTerminate();
    }

    public static boolean pingServer() {
        return binder != null && binder.pingBinder();
    }

    public static void onServerReceive(IBinder binder) {
        App.binder = binder;
        App.iService = IService.Stub.asInterface(binder);
        if (App.pingServer()) {
            for (OnServiceConnectListener listener : mConnectListeners) {
                listener.onServiceConnect(iService);
            }
        } else {
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
