package rikka.rish;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.util.Log;

import yangfentuozi.runner.server.IRishService;

public class RishConfig {

    private static final String TAG = "RISHConfig";

    private static IRishService service;
    private static String libraryPath;

    static IRishService getService() {
        return service;
    }

    public static void setLibraryPath(String path) {
        libraryPath = path;
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private static void loadLibrary() {
        if (libraryPath == null) {
            System.loadLibrary("rish");
        } else {
            System.load(libraryPath + "/librish.so");
        }
    }

    public static void init() {
        Log.d(TAG, "init (server)");
        loadLibrary();
    }

    public static void init(IBinder binder) {
        Log.d(TAG, "init (client) " + binder);
        RishConfig.service = IRishService.Stub.asInterface(binder);
        loadLibrary();
    }
}
