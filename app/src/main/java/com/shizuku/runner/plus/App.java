package com.shizuku.runner.plus;

import android.app.Application;
import android.os.IBinder;
import android.os.RemoteException;
import android.system.Os;

import androidx.annotation.NonNull;

import com.google.android.material.color.DynamicColors;
import com.shizuku.runner.plus.cli.IApp;
import com.shizuku.runner.plus.receiver.OnServiceConnectListener;
import com.shizuku.runner.plus.receiver.OnServiceDisconnectListener;
import com.shizuku.runner.plus.server.IService;
import com.shizuku.runner.plus.server.Server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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
        if (App.pingServer())
            onServiceConnectListener.onServiceConnect(iService);
    }

    public static void addOnServiceConnectListenerX(@NonNull OnServiceConnectListener onServiceConnectListener) {
        mConnectListeners.add(onServiceConnectListener);
    }

    public static void removeOnServiceConnectListener(@NonNull OnServiceConnectListener onServiceConnectListener) {
        mConnectListeners.remove(onServiceConnectListener);
    }

    public static void addOnServiceDisconnectListener(@NonNull OnServiceDisconnectListener onServiceDisconnectListener) {
        mDisconnectListeners.add(onServiceDisconnectListener);
        if (!App.pingServer())
            onServiceDisconnectListener.onServiceDisconnect();
    }

    public static void addOnServiceDisconnectListenerX(@NonNull OnServiceDisconnectListener onServiceDisconnectListener) {
        mDisconnectListeners.add(onServiceDisconnectListener);
    }

    public static void removeOnServiceDisconnectListener(@NonNull OnServiceDisconnectListener onServiceDisconnectListener) {
        mDisconnectListeners.remove(onServiceDisconnectListener);
    }
}
