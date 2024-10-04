package com.shizuku.runner.plus.server;

import android.annotation.SuppressLint;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.ddm.DdmHandleAppName;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.Os;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Server {
    public static final String TAG = "runner_server";
    public static final String usr_path = "/data/local/tmp/runner/usr";
    public static final String appPackageName = "com.shizuku.runner.plus";
    public static final int PORT = 13432;
    public static final String ACTION_SERVER_RUNNING = "runner.plus.intent.action.SERVER_RUNNING";
    public static final String ACTION_SERVER_STOPPED = "runner.plus.intent.action.SERVER_STOPPED";
    private IPackageManager packageManager;
    private IActivityManager activityManager;
    private Intent intent;
    public Logger Log;
    public boolean isStop = false;
    public String appPath;

    public static void main(String[] args) {
        DdmHandleAppName.setAppName("runner_server", Os.getuid());
        int uid = Os.getuid();
        if (uid != 2000 && uid != 0) {
            System.err.printf("Insufficient permission! Need to be launched by root or shell, but your uid is %d.\n", uid);
            System.exit(255);
        }
        new Server(args);
    }

    private Server(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isStop)
                onStop();
        }));
        try {
            onStart(args);
        } catch (Throwable e) {
            Log.e(e.toString());
        }
    }

    public void onStart(String[] args) {
        Log = new Logger(TAG, new File(usr_path, "../logs"));
        if (args.length != 0 && args[0].equals("restart")) {
            Log.i("Server Restart.");
        } else {
            Log.i("Server Start.");
        }

        packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));

        try {
            appPath = getAppPath();
        } catch (Throwable e) {
            Log.e("Unable to get app file path!\n" + e.getMessage());
            exit(1);
        }

        init();
        listenAppChange();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            Log.i("Socket server start.");
        } catch (Exception e) {
            Log.e("Unable to create socket server, it is possible that the previous server was not terminated!");
            Log.i("Try to stop the previous server.");
            try {
                Socket socket = new Socket("localhost", PORT);
                OutputStream out = socket.getOutputStream();
                out.write("stopServer\n".getBytes());
                out.close();
                socket.close();
            } catch (IOException ioE) {
                Log.e("Unable to stop the previous server, may be that other apps are occupying the port!");
                exit(1);
            }
            try {
                Thread.sleep(1000);
                serverSocket = new ServerSocket(PORT);
                Log.i("Socket server start.");
            } catch (Exception e1) {
                Log.e("Still can't create socket server!");
                exit(1);
            }
        }
        while (!isStop) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e(e.toString());
                continue;
            }
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String inline;
                while ((inline = bufferedReader.readLine()) != null) {
                    sb.append(inline);
                    sb.append("\n");
                }
                bufferedReader.close();
                socket.close();
            } catch (IOException e) {
                Log.e("Unable to read socket reply.\n" + e.toString());
                continue;
            }
            String action;
            if (sb.toString().isEmpty())
                action = "";
            else
                action = sb.toString().substring(0, sb.length() - 1);
            switch (action) {
                case "sendBinderToApp": {
                    if (sendBinderToAppByStickyBroadcast()) {
                        Log.i("Send binder by broadcast.");
                    } else {
                        Log.e("Failed to send binder by broadcast!");
                    }
                    break;
                }
                case "stopServer": {
                    isStop = true;
                    try {
                        serverSocket.close();
                        Log.i("Socket server stop.");
                    } catch (IOException e) {
                        Log.e("Unable to stop the socket server!\n" + e.toString());
                    }
                    exit(0);
                    break;
                }
                default: {
                    if (action != null && !action.isEmpty()) {
                        Log.w("Unsupported action: " + action);
                    }
                    break;
                }
            }
        }
    }

    private void listenAppChange() {
        File app = new File(appPath);
        final long[] lastTime = new long[1];
        lastTime[0] = app.lastModified();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!app.exists()) {
                    onAppChange();
                    timer.cancel();
                } else {
                    if (lastTime[0] != app.lastModified()) {
                        onAppChange();
                        timer.cancel();
                    }
                }
            }
        }, 0L, 1000L);
    }

    private void onAppChange() {
        new Thread(() -> {
            try {
                appPath = getAppPath();
                if (appPath == null || appPath.isEmpty()) {
                    Log.w("App is uninstalled!");
                    exit(1);
                } else {
                    try {
                        String file = System.getenv("appPath_file");
                        if (file != null && !file.isEmpty()) {
                            File file1 = new File(file);
                            if (!file1.exists())
                                file1.createNewFile();
                            OutputStream out = new FileOutputStream(file1);
                            out.write(appPath.getBytes());
                            out.flush();
                            out.close();
                        }
                    } catch (Exception ignored) {
                    }
                    exit(10);
                }
            } catch (Exception e) {
                Log.w("App is uninstalled!");
                exit(1);
            }
        }).start();
    }

    private String getAppPath() throws RemoteException {
        ApplicationInfo applicationInfo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationInfo = packageManager.getApplicationInfo(appPackageName, 0L, 0);
        } else {
            applicationInfo = packageManager.getApplicationInfo(appPackageName, 0, 0);
        }
        return applicationInfo.sourceDir;
    }

    private void init() {
        File PREFIX = new File(usr_path);
        ZipFile app = null;
        try {
            app = new ZipFile(appPath);
        } catch (IOException e) {
            Log.e("Read app file error!\n" + e);
        }

        if (!PREFIX.exists())
            PREFIX.mkdirs();

        File HOME = new File(PREFIX, "home");
        if (!HOME.exists())
            HOME.mkdirs();

        File TMPDIR = new File(PREFIX, "tmp");
        if (!TMPDIR.exists())
            TMPDIR.mkdirs();

        File ETC = new File(PREFIX, "etc");
        if (!ETC.exists())
            ETC.mkdirs();

        File PROFILE_D = new File(ETC, "profile.d");
        if (!PROFILE_D.exists())
            PROFILE_D.mkdirs();

        File BIN = new File(PREFIX, "bin");
        if (!BIN.exists())
            BIN.mkdirs();

        File APPLETS = new File(BIN, "applets");
        if (!APPLETS.exists())
            APPLETS.mkdirs();

        File LIB = new File(PREFIX, "lib");
        if (!LIB.exists())
            LIB.mkdirs();

        File ver = new File(PREFIX, "ver.txt");
        if (app == null)
            return;
        if (!ver.exists()) {
            releaseFiles(app);
        } else {
            BufferedReader bufferedReader;
            try {
                bufferedReader = new BufferedReader(new FileReader(ver));
            } catch (FileNotFoundException e) {
                releaseFiles(app);
                return;
            }
            StringBuilder sb = new StringBuilder();
            String inline;
            try {
                while ((inline = bufferedReader.readLine()) != null) {
                    sb.append(inline);
                    sb.append("\n");
                }
                bufferedReader.close();
            } catch (IOException ignored) {
            }
            int local_verCode;
            try {
                local_verCode = Integer.parseInt(sb.toString().replaceAll("\n", ""));
            } catch (NumberFormatException e) {
                releaseFiles(app);
                return;
            }

            try {
                bufferedReader = new BufferedReader(new InputStreamReader(app.getInputStream(app.getEntry("assets/usr/ver.txt"))));
            } catch (IOException e) {
                Log.e("Unable to read 'ver.txt' in the app file!\n" + e);
                return;
            }
            sb = new StringBuilder();
            try {
                while ((inline = bufferedReader.readLine()) != null) {
                    sb.append(inline);
                    sb.append("\n");
                }
                bufferedReader.close();
            } catch (IOException ignored) {
            }
            int app_verCode = Integer.parseInt(sb.toString().replaceAll("\n", ""));

            if (app_verCode > local_verCode)
                releaseFiles(app);
        }
    }

    private void releaseFiles(ZipFile app) {
        Log.i("Release term files.");
        Enumeration<? extends ZipEntry> entries = app.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.getName().matches("^assets/usr/.*")) {
                if (zipEntry.getName().matches("^assets/usr/common/.*") || zipEntry.getName().matches("^assets/usr/" + Build.SUPPORTED_ABIS[0] + "/.*") || "assets/usr/ver.txt".equals(zipEntry.getName())) {
                    try {
                        File file;
                        if ("assets/usr/ver.txt".equals(zipEntry.getName()))
                            file = new File(usr_path, zipEntry.getName().split("/", 3)[2]);
                        else
                            file = new File(usr_path, zipEntry.getName().split("/", 4)[3]);
                        if (!Objects.requireNonNull(file.getParentFile()).exists())
                            file.getParentFile().mkdirs();
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        BufferedInputStream in;
                        BufferedOutputStream out;
                        in = new BufferedInputStream(app.getInputStream(zipEntry));
                        out = new BufferedOutputStream(new FileOutputStream(file));
                        int len;
                        byte[] b = new byte[1024];
                        while ((len = in.read(b)) != -1) {
                            out.write(b, 0, len);
                        }
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        Log.e("Unable to unzip file: %s\n %s", zipEntry.getName(), e.toString());
                    }
                }
            }
        }

        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.OTHERS_READ);
        chmod_R(new File(usr_path + "/.."), perms);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        chmod_R(new File(usr_path + "/bin"), perms);

        if (exec(new String[]{usr_path + "/bin/busybox --install -s " + usr_path + "/bin/applets"}) != 0) {
            Log.e("Unable to install busybox!");
        } else {
            Log.i("Installed busybox.");
        }
    }

    private void chmod_R(File file, Set<PosixFilePermission> perms) {
        if (!file.exists()) return;
        try {
            Files.setPosixFilePermissions(file.toPath(), perms);
        } catch (IOException e) {
            Log.e("Unable to set file permission: %s\n%s", file, e.toString());
        }
        if (file.isFile())
            return;
        File[] childFiles = file.listFiles();
        if (childFiles == null)
            return;
        for (File childFile : childFiles) {
            chmod_R(childFile, perms);
        }
    }

    public boolean sendBinderToAppByStickyBroadcast() {
        try {
            //生成binder
            IBinder binder = new IService.Stub() {
                @Override
                public void destroy() throws RemoteException {
                    System.exit(0);
                }

                @Override
                public void exit() throws RemoteException {
                    destroy();
                }

                @Override
                public int getuid() throws RemoteException {
                    return Os.getuid();
                }

                @Override
                public int execX(String cmd, String name, String pipe, int port) throws RemoteException {
                    try {
                        Runtime.getRuntime().exec(new String[]{usr_path + "/bin/busybox", "mkfifo", pipe}).waitFor();
                        Socket socket = new Socket("localhost", port);
                        Process p = Runtime.getRuntime().exec(usr_path + "/bin/bash");
                        OutputStream out = p.getOutputStream();
                        out.write((". " + usr_path + "/etc/profile\n").getBytes());
                        out.write(("exec -a RUNNER-bash:" + name + " " + usr_path + "/bin/bash >> " + pipe + " 2>> " + pipe + "\n").getBytes());
                        out.flush();
                        out.write(("echo $$\n").getBytes());
                        out.flush();
                        out.write((cmd + "\n").getBytes());
                        out.flush();
                        out.write(("exit\n").getBytes());
                        out.write(("exitValue=$?\n").getBytes());
                        out.write(("rm -f " + pipe + "\n").getBytes());
                        out.write(("exit $exitValue\n").getBytes());
                        out.flush();
                        out.close();
                        try {
                            // 建立socket连接
                            OutputStream os = socket.getOutputStream();

                            // 实时读取并返回存储shell执行结果的管道
                            BufferedReader bufferedReader = new BufferedReader(new FileReader(pipe));
                            String inline;
                            while ((inline = bufferedReader.readLine()) != null) {
                                os.write(inline.getBytes());
                                os.write("\n".getBytes());
                                os.flush();
                            }

                            // 读完了要关掉
                            os.close();
                            bufferedReader.close();
                            socket.shutdownOutput();
                            socket.close();
                        } catch (Exception e) {
                            Log.e(e.toString());
                        }
                        p.waitFor();
                        return p.exitValue();
                    } catch (Exception e) {
                        Log.e(e.toString());
                        return -1;
                    }
                }

                @Override
                public String exec(String cmd) throws RemoteException {
                    try {
                        Process process = Runtime.getRuntime().exec(usr_path + "/bin/bash");
                        OutputStream out = process.getOutputStream();
                        out.write((". " + usr_path + "/etc/profile\n").getBytes());
                        out.flush();
                        out.write((cmd + "\n").getBytes());
                        out.flush();
                        out.write(("exit\n").getBytes());
                        out.flush();
                        out.close();
                        StringBuilder sb = new StringBuilder();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String inline;
                        while ((inline = bufferedReader.readLine()) != null) {
                            sb.append(inline);
                            sb.append("\n");
                        }
                        bufferedReader.close();
                        process.waitFor();
                        return sb.toString();
                    } catch (Exception e) {
                        Log.e(e.toString());
                        return null;
                    }
                }

                @Override
                public void deleteFreePIPE() throws RemoteException {
                    try {
                        Process process = Runtime.getRuntime().exec(usr_path + "/bin/bash");
                        OutputStream out = process.getOutputStream();
                        out.write((". " + usr_path + "/etc/profile\n").getBytes());
                        out.flush();
                        out.write(("sh " + usr_path + "/etc/profile d_p\n").getBytes());
                        out.flush();
                        out.write(("exit\n").getBytes());
                        out.flush();
                        out.close();
                        process.waitFor();
                    } catch (Exception e) {
                        Log.e(e.toString());
                    }
                }
            };
            BinderContainer binderContainer = new BinderContainer(binder);

            intent = new Intent(Server.ACTION_SERVER_RUNNING)
                    .setPackage(Server.appPackageName)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .putExtra("binder", binderContainer);

            activityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
            activityManager.broadcastIntent(null, intent, null, null, 0, null, null,
                    null, -1, null, true, true, 0);
        } catch (Throwable e) {
            Log.e(e.toString());
            return false;
        }
        return true;
    }

    @SuppressLint({"DiscouragedPrivateApi", "SoonBlockedPrivateApi"})
    public boolean removeStickyBroadcast() {
        if (intent == null || activityManager == null) return true;
        try {
            Method unbroadcastIntentMethod = IActivityManager.class.getDeclaredMethod("unbroadcastIntent", IApplicationThread.class, Intent.class, int.class);
            unbroadcastIntentMethod.invoke(activityManager, null, intent, -1);
            intent = null;
            activityManager = null;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private int exec(String[] cmds) {
        return exec(cmds, null);
    }

    private int exec(String[] cmds, String TAG) {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            OutputStream out = p.getOutputStream();
            for (String cmd : cmds) {
                out.write((cmd + "\n").getBytes());
                out.flush();
            }
            out.write("exit\n".getBytes());
            out.flush();
            out.close();
            new Thread(() -> {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String inline;
                    while ((inline = bufferedReader.readLine()) != null) {
                        if (TAG == null || TAG.isEmpty())
                            Log.i(inline + "\n");
                        else
                            Log.i(TAG + ": " + inline + "\n");
                    }
                    bufferedReader.close();
                } catch (Exception ignored) {
                }
            }).start();
            new Thread(() -> {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    String inline;
                    while ((inline = bufferedReader.readLine()) != null) {
                        if (TAG == null || TAG.isEmpty())
                            Log.e(inline + "\n");
                        else
                            Log.e(TAG + ": " + inline + "\n");
                    }
                    bufferedReader.close();
                } catch (Exception ignored) {
                }
            }).start();
            p.waitFor();
            return p.exitValue();
        } catch (Exception e) {
            return -1;
        }
    }

    public void onStop() {
        try {
            intent = new Intent(Server.ACTION_SERVER_STOPPED)
                    .setPackage(Server.appPackageName)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            activityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
            activityManager.broadcastIntent(null, intent, null, null, 0, null, null,
                    null, -1, null, true, false, 0);
        } catch (Throwable e) {
            Log.e("Unable to broadcast 'server stopped'!\n"+ e);
        }
        isStop = true;
        Log.i("Server Stop.\n");
        Log.close();
    }

    public void exit(int status) {
        if (removeStickyBroadcast()) {
            Log.i("Removed broadcast.");
            onStop();
            System.exit(status);
        } else {
            Log.e("Failed to remove broadcast!");
            onStop();
            System.exit(1);
        }
    }
}
