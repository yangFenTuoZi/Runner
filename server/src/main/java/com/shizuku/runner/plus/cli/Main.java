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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static final String ARG_HELP = "help";
    public static final String ARG_LIST = "list";
    public static final String ARG_LIST_PROC = "listProc";
    public static final String ARG_EXEC = "exec";
    public static final String ARG_RUN = "run";
    public static final String ARG_DEL = "delete";
    public static final String ARG_EDIT = "edit";
    public static final String ARG_KILL = "kill";
    public static final String ARG_STOP_SERVER = "stopServer";
    public static final String selfName = "runner_cli";
    private static Handler handler;
    private static String[] args;

    public static void help() {
        if (BuildConfig.DEBUG)
            System.out.println("Runner userspace cli (DEBUG)");
        else
            System.out.println("Runner userspace cli");
        System.out.printf("%5s: %s\n", "Usage", selfName);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_HELP);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_STOP_SERVER);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_LIST);
        System.out.printf("%5s: %s %-8s\n", "or", selfName, ARG_LIST_PROC);
        System.out.printf("%5s: %s %-8s cmd\n", "or", selfName, ARG_EXEC);
        System.out.printf("%5s: %s %-8s cmdID\n", "or", selfName, ARG_RUN);
        System.out.printf("%5s: %s %-8s cmdID\n", "or", selfName, ARG_DEL);
        System.out.printf("%5s: %s %-8s cmdID name(string) cmd(string) keepAlive(boolean) useChid(boolean) ids(string)\n", "or", selfName, ARG_EDIT);
        System.out.printf("%5s: %s %-8s PID\n", "or", selfName, ARG_KILL);
    }

    public static void main(String[] args) {
        Main.args = args;
        if (args.length == 0) {
            help();
            System.exit(1);
        } else {
            switch (args[0]) {
                case ARG_LIST: {
                    a(new BinderReceiver() {
                        @Override
                        public void onBinderReceived(IBinder serverBinder, IBinder appBinder) {
                            if (appBinder == null || ! appBinder.pingBinder()) {
                                System.err.println("App binder is not alive");
                                System.exit(1);
                            } else {
                                try {
                                    JSONObject json = new JSONObject();
                                    cmdInfo[] cmdInfos = IApp.Stub.asInterface(appBinder).getAllCmds();
                                    JSONArray jsons = new JSONArray();
                                    int i = 0;
                                    for (cmdInfo cmdInfo : cmdInfos) {
                                        try {
                                            jsons.put(getJsonObject(cmdInfo));
                                        } catch (Exception ignored) {
                                        }
                                        i++;
                                    }
                                    json.put("cmdInfo", jsons);
                                    System.out.println(json.toString(4));
                                } catch (RemoteException e) {
                                    System.err.println("Unable to invoke getAllCmds()");
                                } catch (JSONException ignored) {
                                }
                            }
                            System.exit(0);
                        }
                    });
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

    private static JSONObject getJsonObject(cmdInfo cmdInfo) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", cmdInfo.id);
        jsonObject.put("name", cmdInfo.name);
        jsonObject.put("command", cmdInfo.command);
        jsonObject.put("keepAlive", cmdInfo.keepAlive);
        jsonObject.put("useChid", cmdInfo.useChid);
        jsonObject.put("ids", cmdInfo.ids);
        return jsonObject;
    }

    private static void a(BinderReceiver binderReceiver) {

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        handler = new Handler(Looper.getMainLooper());

        try {
            requestForBinder(binderReceiver);
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

    interface BinderReceiver {
        default void onBinderReceived(IBinder serverBinder, IBinder appBinder) {
        }
    }

    @SuppressLint("WrongConstant")
    private static void requestForBinder(BinderReceiver binderReceiver) {
        try {
            Bundle data = new Bundle();
            data.putBinder("binder", new Binder() {

                @Override
                protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    if (code == 1) {
                        IBinder[] binders = new IBinder[2];
                        data.readBinderArray(binders);
                        handler.post(() -> binderReceiver.onBinderReceived(binders[1], binders[0]));
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
