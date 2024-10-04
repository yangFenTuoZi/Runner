package com.shizuku.runner.plus;

import android.app.Application;

import androidx.annotation.NonNull;

import com.google.android.material.color.DynamicColors;
import com.shizuku.runner.plus.receiver.OnServiceConnectListener;
import com.shizuku.runner.plus.receiver.OnServiceDisconnectListener;
import com.shizuku.runner.plus.server.IService;

import java.util.ArrayList;
import java.util.List;

public class App extends Application {
    public static IService iService;
    private static final List<OnServiceConnectListener> mConnectListeners;
    private static final List<OnServiceDisconnectListener> mDisconnectListeners;

    static {
        mConnectListeners = new ArrayList<>();
        mDisconnectListeners = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }

    public static void onServerReceive(IService iService) {
        App.iService = iService;
        if (iService == null){
            for (OnServiceDisconnectListener listener : mDisconnectListeners) {
                listener.onServiceDisconnect();
            }
        } else {
            for (OnServiceConnectListener listener : mConnectListeners) {
                listener.onServiceConnect(iService);
            }
        }
    }

    public static void addOnServiceConnectListener(@NonNull OnServiceConnectListener onServiceConnectListener) {
        mConnectListeners.add(onServiceConnectListener);
        if (iService != null)
            onServiceConnectListener.onServiceConnect(iService);
    }

    public static void removeOnServiceConnectListener(@NonNull OnServiceConnectListener onServiceConnectListener) {
        mConnectListeners.remove(onServiceConnectListener);
    }

    public static void addOnServiceDisconnectListener(@NonNull OnServiceDisconnectListener onServiceDisconnectListener) {
        mDisconnectListeners.add(onServiceDisconnectListener);
        if (iService == null)
            onServiceDisconnectListener.onServiceDisconnect();
    }

    public static void removeOnServiceDisconnectListener(@NonNull OnServiceDisconnectListener onServiceDisconnectListener) {
        mDisconnectListeners.remove(onServiceDisconnectListener);
    }
}
