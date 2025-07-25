package rikka.rish;

import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.system.Os;

import java.util.Objects;

import rikka.hidden.compat.ActivityManagerApis;
import stub.dalvik.system.VMRuntimeHidden;
import yangfentuozi.runner.BuildConfig;
import yangfentuozi.runner.server.callback.IRequestBinderCallback;

public class Rish {

    private static void startShell() {
        try {
            RishTerminal terminal = new RishTerminal(args);
            terminal.start();
            int exitCode = terminal.waitFor();
            System.exit(exitCode);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String[] args;
    private static Handler handler;

    private static final Binder receiverBinder = new IRequestBinderCallback.Stub() {

        @Override
        public void onCallback(IBinder binder, String sourceDir) throws RemoteException {
            if (binder != null) {
                handler.post(() -> onBinderReceived(binder, sourceDir));
            } else {
                System.err.println("Server is not running");
                System.err.flush();
                System.exit(1);
            }
        }
    };

    private static void requestForBinder() throws RemoteException {
        Bundle data = new Bundle();
        data.putBinder("binder", receiverBinder);

        Intent intent = new Intent("yangfentuozi.runner.intent.action.REQUEST_BINDER")
                .setPackage(BuildConfig.APPLICATION_ID)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra("data", data);

        // broadcastIntent will fail on Android 8.x
        //  com.android.server.am.ActivityManagerService.isInstantApp(ActivityManagerService.java:18547)
        //  com.android.server.am.ActivityManagerService.broadcastIntentLocked(ActivityManagerService.java:18972)
        //  com.android.server.am.ActivityManagerService.broadcastIntent(ActivityManagerService.java:19703)
        //
        try {
            ActivityManagerApis.broadcastIntent(intent, Os.getuid() / 100000);
        } catch (Throwable e) {
            if ((Build.VERSION.SDK_INT != Build.VERSION_CODES.O && Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1)
                    || !Objects.equals(e.getMessage(), "Calling application did not provide package name")) {
                throw e;
            }

            System.err.println("broadcastIntent fails on Android 8.0 or 8.1");
            System.err.flush();
        }
    }

    private static void onBinderReceived(IBinder binder, String sourceDir) {
        var base = sourceDir.substring(0, sourceDir.lastIndexOf('/'));
        String librarySearchPath = base + "/lib/" + VMRuntimeHidden.getRuntime().vmInstructionSet();
        RishConfig.setLibraryPath(librarySearchPath);
        RishConfig.init(binder);
        startShell();
    }

    public static void main(String[] argv) {
        args = argv;

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        handler = new Handler(Looper.getMainLooper());

        try {
            requestForBinder();
        } catch (Throwable tr) {
            tr.printStackTrace(System.err);
            System.err.flush();
            System.exit(1);
        }

        handler.postDelayed(() -> {
            System.err.println("Request timeout");
            System.err.flush();
            System.exit(1);
        }, 5000);

        Looper.loop();
        System.exit(0);
    }
}
