package yangFenTuoZi.runner.plus.cli;

import android.annotation.SuppressLint;
import android.app.IActivityManager;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.Os;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import yangFenTuoZi.runner.plus.info.Info;
import yangFenTuoZi.runner.plus.service.IService;
import yangFenTuoZi.runner.plus.service.Server;

public class Main {

//    @Parameter(names = {"-l", "--list"}, description = "list all cmds")
//    private boolean list;
//
//    @Parameter(names = {"-sr", "--showR"}, description = "show all running cmds")
//    private boolean showProc;
//
//    @Parameter(names = {"-c", "--exec"}, description = "exec command", converter = StringConverter.class)
//    private String exec = null;
//
//    @Parameter(names = {"-r", "--run"}, description = "run a cmd by id", converter = IntegerConverter.class)
//    private int run = -1;
//
//    @Parameter(names = {"-e", "--edit"}, description = "edit a cmd by id", converter = StringConverter.class)
//    private String edit = null;
//
//    @Parameter(names = {"-k", "--kill"}, description = "kill a process", converter = IntegerConverter.class)
//    private int kill = -1;
//
//    @Parameter(names = {"-s", "--stop"}, description = "stop server")
//    private boolean stopServer;
//
//    @Parameter(names = "--help")
//    private boolean help;
//
//    @Parameter(names = "--version", description = "show version")
//    private boolean v;

    public static final String selfName = "runner_cli";
    private static Handler handler;

    public static void main(String[] argv) {
        if (argv.length == 0) {
            help();
            System.exit(1);
        }

        switch (argv[0]) {
            case "--list", "-l": {
                waitForBinder(new BinderReceiver() {
                    @Override
                    public void onBinderReceived(IBinder serverBinder) {
                        BinderReceiver.super.onBinderReceived(serverBinder);
                        try {
                            CmdInfo[] cmdInfos = IService.Stub.asInterface(serverBinder).getAll();
                            JSONArray jsons = new JSONArray();
                            for (CmdInfo cmdInfo : cmdInfos) {
                                try {
                                    jsons.put(getJsonObject(cmdInfo));
                                } catch (Exception ignored) {
                                }
                            }
                            System.out.println(jsons.toString(4));
                        } catch (RemoteException e) {
                            System.err.println("Unable to call server");
                            e.printStackTrace(System.err);
                            System.exit(1);
                        } catch (JSONException ignored) {
                        }
                        System.exit(0);
                    }
                });
                break;
            }

            case "-sr", "--showR": {
                break;
            }

            case "-c", "--command": {
                if (argv.length == 1) {
                    help();
                    System.exit(1);
                }
                waitForBinder(new BinderReceiver() {
                    @Override
                    public void onBinderReceived(IBinder serverBinder) {
                        BinderReceiver.super.onBinderReceived(serverBinder);
                        try {
                            IService iService = IService.Stub.asInterface(serverBinder);
                            int port = getUsablePort(8400);
                            if (port == -1) {
                                System.err.println("No usable port, maybe the app doesn't have 'INTERNET' perm");
                                System.exit(255);
                            }
                            new Thread(() -> {
                                try {
                                    ServerSocket serverSocket = new ServerSocket(port);
                                    Socket socket = serverSocket.accept();
                                    try {
                                        System.out.print("PID: ");
                                        var in = socket.getInputStream();
                                        byte[] buffer = new byte[1024];
                                        int len;
                                        while ((len = in.read(buffer)) != -1) {
                                            System.out.write(buffer, 0, len);
                                        }
                                        in.close();
                                        socket.close();
                                    } catch (Exception ignored) {
                                    }
                                    serverSocket.close();
                                } catch (Exception e) {
                                    e.printStackTrace(System.err);
                                }
                            }).start();
                            System.exit(iService.execX(arraysToArgv(argv, 1, argv.length - 1), "CLI-TMP", port));
                        } catch (RemoteException e) {
                            System.err.println("Unable to call server");
                            e.printStackTrace(System.err);
                            System.exit(255);
                        }
                    }
                });
                break;
            }

            case "-r", "--run": {
                if (argv.length == 1) {
                    help();
                    System.exit(1);
                }
                waitForBinder(new BinderReceiver() {
                    @Override
                    public void onBinderReceived(IBinder serverBinder) {
                        BinderReceiver.super.onBinderReceived(serverBinder);
                        try {
                            IService iService = IService.Stub.asInterface(serverBinder);
                            CmdInfo cmdInfo = iService.query(Integer.parseInt(argv[1]));
                            if (cmdInfo == null) {
                                System.err.println("Unable to get this cmd");
                                System.exit(255);
                            }
                            assert cmdInfo != null;
                            int port = getUsablePort(8400);
                            if (port == -1) {
                                System.err.println("No usable port, maybe the app doesn't have 'INTERNET' perm");
                                System.exit(1);
                            }
                            new Thread(() -> {
                                try {
                                    ServerSocket serverSocket = new ServerSocket(port);
                                    Socket socket = serverSocket.accept();
                                    try {
                                        System.out.print("PID: ");
                                        var in = socket.getInputStream();
                                        byte[] buffer = new byte[1024];
                                        int len;
                                        while ((len = in.read(buffer)) != -1) {
                                            System.out.write(buffer, 0, len);
                                        }
                                        in.close();
                                        socket.close();
                                    } catch (Exception ignored) {
                                    }
                                    serverSocket.close();
                                } catch (Exception e) {
                                    e.printStackTrace(System.err);
                                }
                            }).start();
                            System.exit(iService.execX(cmdInfo.command, cmdInfo.name, port));
                        } catch (RemoteException e) {
                            System.err.println("Unable to call server");
                            e.printStackTrace(System.err);
                            System.exit(255);
                        }
                    }
                });
                break;
            }

            case "-d", "--delete": {
                if (argv.length == 1) {
                    help();
                    System.exit(1);
                }
                waitForBinder(new BinderReceiver() {
                    @Override
                    public void onBinderReceived(IBinder serverBinder) {
                        BinderReceiver.super.onBinderReceived(serverBinder);
                        try {
                            IService iService = IService.Stub.asInterface(serverBinder);
                            int id = Integer.parseInt(argv[1]);
                            iService.delete(id);
                            System.exit(iService.query(id) == null ? 0 : 1);
                        } catch (RemoteException e) {
                            System.err.println("Unable to call server");
                            e.printStackTrace(System.err);
                            System.exit(255);
                        }
                    }
                });
                break;
            }

            case "-e", "--edit": {
                break;
            }

            case "-k", "--kill": {
                if (argv.length == 1) {
                    help();
                    System.exit(1);
                }
                waitForBinder(new BinderReceiver() {
                    @Override
                    public void onBinderReceived(IBinder serverBinder) {
                        BinderReceiver.super.onBinderReceived(serverBinder);
                        try {
                            IService iService = IService.Stub.asInterface(serverBinder);
                            int port = getUsablePort(8400);
                            if (port == -1) {
                                System.err.println("No usable port, maybe the app doesn't have 'INTERNET' perm");
                                System.exit(255);
                            }
                            new Thread(() -> {
                                try {
                                    ServerSocket serverSocket = new ServerSocket(port);
                                    Socket socket = serverSocket.accept();
                                    try {
                                        System.out.print("PID: ");
                                        var in = socket.getInputStream();
                                        byte[] buffer = new byte[1024];
                                        int len;
                                        while ((len = in.read(buffer)) != -1) {
                                            System.out.write(buffer, 0, len);
                                        }
                                        in.close();
                                        socket.close();
                                    } catch (Exception ignored) {
                                    }
                                    serverSocket.close();
                                } catch (Exception e) {
                                    e.printStackTrace(System.err);
                                }
                            }).start();
                            System.exit(iService.execX("kill -9 " + arraysToArgv(argv, 1, argv.length - 1), "CLI-TMP", port));
                        } catch (RemoteException e) {
                            System.err.println("Unable to call server");
                            e.printStackTrace(System.err);
                            System.exit(255);
                        }
                    }
                });
                break;
            }

            case "-s", "--stop": {
                waitForBinder(new BinderReceiver() {
                    @Override
                    public void onBinderReceived(IBinder serverBinder) {
                        BinderReceiver.super.onBinderReceived(serverBinder);
                        try {
                            Socket socket = new Socket("localhost", Server.PORT);
                            OutputStream out = socket.getOutputStream();
                            out.write("stopServer".getBytes());
                            out.flush();
                            out.close();
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                            System.exit(1);
                        }
                        System.exit(0);
                    }
                });
                break;
            }

            case "-v", "--version": {
                System.out.print("version-name: ");
                System.out.println(Info.VERSION_NAME);
                System.out.print("version-code: ");
                System.out.println(Info.VERSION_CODE);
                System.exit(0);
                break;
            }

            case "--help", "-h": {
                help();
                System.exit(0);
                break;
            }

            default: {
                help();
                System.exit(1);
            }
        }
    }

    private static void help() {

    }

    private static String arraysToArgv(String[] arrays, int start, int end) {
        StringBuilder b = new StringBuilder();
        for (int i = start; ; i++) {
            b.append("\"").append(arrays[i].replaceAll("\"", "\\\"")).append("\"");
            if (i == end)
                break;
            b.append(" ");
        }
        return String.valueOf(b);
    }

    private static JSONObject getJsonObject(CmdInfo cmdInfo) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", cmdInfo.rowid);
        jsonObject.put("name", cmdInfo.name);
        jsonObject.put("command", cmdInfo.command);
        jsonObject.put("keepAlive", cmdInfo.keepAlive);
        jsonObject.put("useChid", cmdInfo.useChid);
        jsonObject.put("ids", cmdInfo.ids);
        return jsonObject;
    }

    private static void waitForBinder(BinderReceiver binderReceiver) {

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        handler = new Handler(Looper.getMainLooper());

        try {
            requestForBinder(binderReceiver);
        } catch (Throwable tr) {
            tr.printStackTrace(System.err);
            System.err.println("Server is not running");
            System.err.flush();
            System.exit(1);
        }

        handler.postDelayed(() -> {
            System.err.println("Server is not running");
            System.err.flush();
            System.exit(1);
        }, 5000);

        Looper.loop();
        System.exit(0);
    }

    interface BinderReceiver {
        default void onBinderReceived(IBinder serverBinder) {
            if (serverBinder == null || !serverBinder.pingBinder()) {
                System.err.println("Server is not running");
                System.err.flush();
                System.exit(1);
            }
        }
    }

    @SuppressLint("WrongConstant")
    private static void requestForBinder(BinderReceiver binderReceiver) {
        try {
            Bundle data = new Bundle();
            data.putBinder("binder", new Binder() {

                @Override
                protected boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
                    if (code == 1) {
                        IBinder binder = data.readStrongBinder();
                        handler.post(() -> binderReceiver.onBinderReceived(binder));
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
            IActivityManager am = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
            int uid = Os.getuid();

            Intent intent = Intent.createChooser(new Intent(Server.ACTION_REQUEST_BINDER)
                            .setPackage(Info.APPLICATION_ID)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                            .putExtra("data", data)
                            .putExtra("packageNames", pm.getPackagesForUid(uid))
                            .putExtra("uid", uid),
                    "Request binder");

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

    public static int getUsablePort(int port) {
        boolean flag = false;
        try {
            Socket socket = new Socket("localhost", port);
            flag = true;
            socket.close();
        } catch (IOException ignored) {
        }
        if (!flag && port == 65536)
            return -1;
        return flag ? getUsablePort(port + 1) : port;
    }
}
