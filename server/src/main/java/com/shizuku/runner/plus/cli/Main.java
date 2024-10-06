package com.shizuku.runner.plus.cli;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.IActivityManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.Os;

import com.shizuku.runner.plus.server.BuildConfig;
import com.shizuku.runner.plus.server.IService;
import com.shizuku.runner.plus.server.Server;

public class Main {

    public static final String ARG_HELP = "help";
    public static final String ARG_LIST = "list";
    public static final String ARG_RUN = "run";
    public static final String ARG_DEL = "delete";
    public static final String ARG_EDIT = "edit";
    public static final String ARG_LIST_PROC = "listProc";
    public static final String ARG_KILL = "killProc";
    public static final String ARG_STOP_SERVER = "stopServer";
    public static final String selfName = "runnerD";
    private static Handler handler;
    private static String[] args;

    public static void help() {
        if (BuildConfig.DEBUG)
            System.out.println("Runner userspace cli (DEBUG)");
        else
            System.out.println("Runner userspace cli");
        System.out.printf("%5s: %s\n", "Usage", selfName);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_STOP_SERVER);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_LIST);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_LIST_PROC);
        System.out.printf("%5s: %s %-8s command_ID\n", "or", selfName, ARG_RUN);
        System.out.printf("%5s: %s %-8s command_ID\n", "or", selfName, ARG_DEL);
        System.out.printf("%5s: %s %-8s command_ID content\n", "or", selfName, ARG_EDIT);
        System.out.printf("%5s: %s %-8s PID\n", "or", selfName, ARG_KILL);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_HELP);
    }

    public static void main(String[] args) {
        Main.args = args;
        if (args.length == 0) {
            help();
            System.exit(1);
        } else {
            switch (args[0]) {
                case ARG_RUN: {

                    if (Looper.getMainLooper() == null) {
                        Looper.prepareMainLooper();
                    }

                    handler = new Handler(Looper.getMainLooper());

                    try {
                        requestForBinder(new BinderReceiver() {
                            @Override
                            public void onBinderReceived(IBinder serverBinder, IBinder appBinder) {
                                try {
                                    if (serverBinder.pingBinder()) {
                                        System.out.println(IService.Stub.asInterface(serverBinder).getuid());
                                    }
                                    if (appBinder.pingBinder()) {
                                        System.out.println(IApp.Stub.asInterface(appBinder).getuid());
                                    }
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                                System.exit(0);
                            }
                        });
                    } catch (Throwable tr) {
                        tr.printStackTrace(System.err);
                        System.err.flush();
                        System.exit(1);
                    }

                    handler.postDelayed(() -> {
                        System.exit(1);
                    }, 5000);

                    Looper.loop();
                    System.exit(0);
                }
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

    interface BinderReceiver {
        default void onBinderReceived(IBinder serverBinder, IBinder appBinder) {}
    }

    @SuppressLint("WrongConstant")
    private static void requestForBinder(BinderReceiver binderReceiver) throws RemoteException {
        try {
            Bundle data = new Bundle();
            data.putBinder("binder", new Binder() {

                @Override
                protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    if (code == 1) {
                        IBinder[] binders = null;
                        try {
                            binders = new IBinder[2];
                            data.readBinderArray(binders);
                        } catch (Throwable e) {
                            System.err.println("Server is not running!");
                            System.exit(1);
                        }
                        IBinder[] finalBinders = binders;
                        handler.post(() -> binderReceiver.onBinderReceived(finalBinders[1], finalBinders[0]));
                        return true;
                    } else if (code == 2) {
                        System.err.println("The user denied the request!");
                        System.exit(1);
                        return true;
                    }
                    return super.onTransact(code, data, reply, flags);
                }
            });

            IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            int uid = Os.getuid();

            Intent intent = Intent.createChooser(new Intent(Server.ACTION_REQUEST_BINDER)
                            .setPackage(Server.appPackageName)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                            .putExtra("data", data)
                            .putExtra("packageNames", pm.getPackagesForUid(uid))
                            .putExtra("uid", uid),
                    "Request binder");

            IActivityManager am = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
            String callingPackage = null;
            try {
                callingPackage = pm.getPackagesForUid(Os.getuid())[0];
            } catch (Throwable ignored) {
            }

            am.startActivityAsUser(null, callingPackage, intent, null, null, null, 0, 0, null, null, Os.getuid() / 100000);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
