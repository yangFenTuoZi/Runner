package com.shizuku.runner.plus.server;

import android.app.IActivityManager;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.shizuku.runner.plus.server.BuildConfig;

import java.util.Objects;

public class UserCli {
    public static final String ARG_HELP = "help";
    public static final String ARG_LIST = "list";
    public static final String ARG_RUN = "run";
    public static final String ARG_DEL = "delete";
    public static final String ARG_EDIT = "edit";
    public static final String ARG_LIST_PROC = "listProc";
    public static final String ARG_KILL = "killProc";
    public static final String ARG_KILL_SERVER = "killServer";
    public static final String selfName = "runnerD";
    private static Handler handler;
    private static String[] args;

    public static void help() {
        if (BuildConfig.DEBUG)
            System.out.println("Runner userspace cli (DEBUG)");
        else
            System.out.println("Runner userspace cli");
        System.out.printf("""
                        
                        verName: %s
                        verCode: %d
                        
                        """);
        System.out.printf("%5s: %s\n", "Usage", selfName);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_KILL_SERVER);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_LIST);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_LIST_PROC);
        System.out.printf("%5s: %s %-8s command_ID\n", "or", selfName, ARG_RUN);
        System.out.printf("%5s: %s %-8s command_ID\n", "or", selfName, ARG_DEL);
        System.out.printf("%5s: %s %-8s command_ID content\n", "or", selfName, ARG_EDIT);
        System.out.printf("%5s: %s %-8s PID\n", "or", selfName, ARG_KILL);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_HELP);
    }

    public static void main(String[] args) {
        UserCli.args = args;
        if (args.length == 0) {
            help();
            System.exit(1);
        } else {
            switch (args[0]) {
                case ARG_HELP: {
                    help();
                    System.exit(0);
                }
                default: {
                    help();
                    System.exit(1);
                }
            }
        }
    }

    private static final Binder receiverBinder = new Binder() {

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1) {
                IBinder binder = data.readStrongBinder();

                if (binder != null) {
                    handler.post(() -> onBinderReceived(binder));
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
        try {
            Bundle data = new Bundle();
            data.putBinder("binder", receiverBinder);

            Intent intent = new Intent("runner.plus.intent.action.REQUEST_BINDER")
                    .setPackage("com.shizuku.runner.plus")
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .putExtra("data", data);

            IActivityManager am = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));

            try {
                // 调用 broadcastIntent 方法，发送粘性广播
                am.broadcastIntent(
                        null,
                        intent,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        -1,
                        null,
                        true,
                        false,
                        0
                );
            } catch (Throwable e) {
                if ((Build.VERSION.SDK_INT != Build.VERSION_CODES.O && Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1)
                        || !Objects.equals(e.getMessage(), "Calling application did not provide package name")) {
                    throw e;
                }

                System.err.println("broadcastIntent fails on Android 8.0 or 8.1, fallback to startActivity");
                System.err.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void onBinderReceived(IBinder binder) {

    }
}
