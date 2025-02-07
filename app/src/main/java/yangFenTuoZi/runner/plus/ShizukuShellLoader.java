package yangFenTuoZi.runner.plus;

import android.annotation.SuppressLint;
import android.app.IActivityManager;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import yangFenTuoZi.runner.plus.info.Info;
import yangFenTuoZi.runner.plus.server.Server;

import java.io.File;

import dalvik.system.DexClassLoader;

public class ShizukuShellLoader {

    private static String[] args;
    private static String callingPackage;
    private static Handler handler;

    private static final Binder receiverBinder = new Binder() {

        @Override
        protected boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1) {
                IBinder binder = data.readStrongBinder();

                String sourceDir = data.readString();
                if (binder != null) {
                    handler.post(() -> onBinderReceived(binder, sourceDir));
                } else {
                    System.err.println("Server is not running");
                    System.err.flush();
                    System.exit(1);
                }
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    };

    private static void requestForBinder() throws RemoteException {
        Bundle data = new Bundle();
        data.putBinder("binder", receiverBinder);

        @SuppressLint("WrongConstant") Intent intent = new Intent("rikka.shizuku.intent.action.REQUEST_BINDER")
                .setPackage("moe.shizuku.privileged.api")
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra("data", data);

        IBinder amBinder = ServiceManager.getService("activity");
        IActivityManager am;
        am = IActivityManager.Stub.asInterface(amBinder);

        am.broadcastIntent(null, intent, null, null, 0, null, null,
                null, -1, null, true, false, 0);
    }

    private static void onBinderReceived(IBinder binder, String sourceDir) {
        String librarySearchPath = sourceDir + "!/lib/" + Build.SUPPORTED_ABIS[0];
        String systemLibrarySearchPath = System.getProperty("java.library.path");
        if (!TextUtils.isEmpty(systemLibrarySearchPath)) {
            librarySearchPath += File.pathSeparatorChar + systemLibrarySearchPath;
        }
        String optimizedDirectory = ".";
        File optimizedDirectoryFile = new File(optimizedDirectory);
        if (!optimizedDirectoryFile.exists() || !optimizedDirectoryFile.isDirectory()  || !optimizedDirectoryFile.canWrite() || !optimizedDirectoryFile.canExecute()) {
            optimizedDirectory = Server.USR_PATH + "/tmp";
            optimizedDirectoryFile = new File(optimizedDirectory);
            optimizedDirectoryFile.mkdirs();
            try {
                Os.chmod(optimizedDirectory, 00777);
            } catch (ErrnoException ignored) {
            }
        }

        try {
            DexClassLoader classLoader = new DexClassLoader(sourceDir, optimizedDirectory, librarySearchPath, ClassLoader.getSystemClassLoader());
            Class<?> cls = classLoader.loadClass("moe.shizuku.manager.shell.Shell");
            cls.getDeclaredMethod("main", String[].class, String.class, IBinder.class, Handler.class)
                    .invoke(null, args, callingPackage, binder, handler);

        } catch (ClassNotFoundException tr) {
            System.err.println("Class not found");
            System.err.println("Make sure you have Shizuku v12.0.0 or above installed");
            System.err.flush();
            System.exit(1);
        } catch (Throwable tr) {
            tr.printStackTrace(System.err);
            System.err.flush();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        ShizukuShellLoader.args = args;

        String packageName = Info.APPLICATION_ID;

        ShizukuShellLoader.callingPackage = packageName;

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

        handler.postDelayed(() -> abort(
                String.format(
                        "Request timeout. The connection between the current app (%1$s) and Shizuku app may be blocked by your system. " +
                                "Please disable all battery optimization features for both current app (%1$s) and Shizuku app.",
                        packageName)
        ), 10000);

        Looper.loop();
        System.exit(0);
    }

    private static void abort(String message) {
        System.err.println(message);
        System.err.flush();
        System.exit(1);
    }
}