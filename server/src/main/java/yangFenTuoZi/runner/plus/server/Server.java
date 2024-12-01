package yangFenTuoZi.runner.plus.server;

import static yangFenTuoZi.runner.plus.server.DatabaseHelper.TABLE_NAME;
import static yangFenTuoZi.runner.plus.server.Logger.getStackTraceString;

import android.annotation.SuppressLint;
import android.app.IActivityManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.ddm.DdmHandleAppName;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.Os;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import yangFenTuoZi.runner.plus.cli.CmdInfo;
import yangFenTuoZi.runner.plus.info.Info;

public class Server {
    public static final String TAG = "runner_server";
    public static final String DATA_PATH = "/data/local/tmp/runner";
    public static final String USR_PATH = DATA_PATH + "/usr";
    public static final String LOG_PATH = DATA_PATH + "/logs";
    public static final int PORT = 13432;
    public static final String ACTION_SERVER_RUNNING = "runner.plus.intent.action.SERVER_RUNNING";
    public static final String ACTION_SERVER_STOPPED = "runner.plus.intent.action.SERVER_STOPPED";
    public static final String ACTION_REQUEST_BINDER = "runner.plus.intent.action.REQUEST_BINDER";
    private IPackageManager packageManager;
    private IActivityManager activityManager;
    public Logger Log;
    public boolean isStop = false;
    public String appPath;
    public FakeContext mContext = FakeContext.get();
    public SQLiteDatabase database;
    public DatabaseHelper databaseHelper;

    public static void main(String[] args) {
        DdmHandleAppName.setAppName("runner_server", Os.getuid());
        int uid = Os.getuid();
        if (uid != 2000 && uid != 0) {
            System.err.printf("Insufficient permission! Need to be launched by root or shell, but your uid is %d.\n", uid);
            System.exit(255);
        }
        new Server(args);
    }

    @SuppressLint("WrongConstant")
    private Server(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isStop)
                onStop();
        }));
        try {
            onStart(args);
        } catch (Throwable e) {
            Log.e(getStackTraceString(e));
        }
    }

    public void onStart(String[] args) {
        Log = new Logger(TAG, new File(LOG_PATH));
        if (args.length != 0 && args[0].equals("restart")) {
            Log.i("Server Restart.");
        } else {
            Log.i("Server Start.");
        }

        packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));

        try {
            appPath = getAppPath();
        } catch (Throwable e) {
            Log.e("Unable to get app file path!\n" + getStackTraceString(e));
            exit(1);
        }

        init();
        listenAppChange();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            Log.i("Socket server start.");
        } catch (Exception e) {
            Log.w("Unable to create socket server, it is possible that the previous server was not terminated!");
            Log.i("Try to stop the previous server.");
            try {
                Socket socket = new Socket("localhost", PORT);
                OutputStream out = socket.getOutputStream();
                out.write("stopServer".getBytes());
                out.close();
                socket.close();
            } catch (IOException ioE) {
                Log.e("Unable to stop the previous server!\n" + getStackTraceString(ioE));
                exit(1);
            }
            try {
                Thread.sleep(1000);
                serverSocket = new ServerSocket(PORT);
                Log.i("Socket server start.");
            } catch (Exception e1) {
                Log.e("Still can't create socket server!\n" + getStackTraceString(e));
                exit(1);
            }
        }

        databaseHelper = new DatabaseHelper(mContext);
        database = databaseHelper.getWritableDatabase();

        while (!isStop) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e(getStackTraceString(e));
                continue;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                var in = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                bos.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                Log.e("Unable to read socket reply.\n" + getStackTraceString(e));
            }
            String[] msg = bos.toString().split("\n");
            String action = msg[0];
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
                        Log.e("Unable to stop the socket server!\n" + getStackTraceString(e));
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
            applicationInfo = packageManager.getApplicationInfo(Info.APPLICATION_ID, 0L, 0);
        } else {
            applicationInfo = packageManager.getApplicationInfo(Info.APPLICATION_ID, 0, 0);
        }
        return applicationInfo.sourceDir;
    }

    private void init() {
        File PREFIX = new File(USR_PATH);
        ZipFile app = null;
        try {
            app = new ZipFile(appPath);
        } catch (IOException e) {
            Log.e("Read app file error!\n" + getStackTraceString(e));
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
                Log.e("Unable to read 'ver.txt' in the app file!\n" + getStackTraceString(e));
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
                            file = new File(USR_PATH, zipEntry.getName().split("/", 3)[2]);
                        else
                            file = new File(USR_PATH, zipEntry.getName().split("/", 4)[3]);
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
                        Log.e("Unable to unzip file: %s\n %s", zipEntry.getName(), getStackTraceString(e));
                    }
                }
            }
        }

        exec(new String[]{
                "chmod -R 777 " + DATA_PATH
        }, "Set perms");

        if (exec(new String[]{USR_PATH + "/bin/busybox --install -s " + USR_PATH + "/bin/applets"}) != 0) {
            Log.e("Unable to install busybox!");
        } else {
            Log.i("Installed busybox.");
        }
    }

    public boolean sendBinderToAppByStickyBroadcast() {
        try {
            BinderContainer binderContainer = new BinderContainer(createBinder());

            @SuppressLint("WrongConstant") Intent intent = new Intent(Server.ACTION_SERVER_RUNNING)
                    .setPackage(Info.APPLICATION_ID)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .putExtra("binder", binderContainer);

            if (activityManager == null || !activityManager.asBinder().pingBinder())
                activityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
            activityManager.broadcastIntent(null, intent, null, null, 0, null, null,
                    null, -1, null, true, false, 0);
        } catch (Throwable e) {
            Log.e(getStackTraceString(e));
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
        isStop = true;
        if (database != null) database.close();
        if (databaseHelper != null) databaseHelper.close();
        Log.i("Server Stop.\n");
        Log.close();
    }

    public void exit(int status) {
        onStop();
        System.exit(status);
    }

    private IBinder createBinder() {
        //生成binder
        return new IService.Stub() {
            @Override
            public String[] version() {
                return new String[]{Info.VERSION_NAME, String.valueOf(Info.VERSION_CODE)};
            }

            @Override
            public CmdInfo[] getAllCmds() {
                return Server.this.getAll();
            }

            @Override
            public CmdInfo getCmdByID(int id) {
                return Server.this.query(id);
            }

            @Override
            public void delete(int id) {
                Log.i("Delete by id: " + id);
                Server.this.delete(id);
            }

            @Override
            public void edit(CmdInfo cmdInfo) {
                if (Server.this.query(cmdInfo.id) == null) {
                    Log.i("Create %s", cmdInfo.toString());
                    Server.this.insert(cmdInfo);
                } else {
                    Log.i("Edit %s", cmdInfo.toString());
                    Server.this.update(cmdInfo);
                }
            }

            @Override
            public int execX(String cmd, String name, int port) {
                try {
                    Process p = Runtime.getRuntime().exec(USR_PATH + "/bin/bash");
                    OutputStream out = p.getOutputStream();
                    out.write(("exec 8<>/dev/tcp/127.0.0.1/" + port + "\n").getBytes());
                    out.flush();
                    out.write(("exec -a \"RUNNER-proc:" + name.replaceAll(" ", "") + "\" \"" + USR_PATH + "/bin/bash\" >&8 2>&1\n").getBytes());
                    out.flush();
                    out.write("echo $$\n".getBytes());
                    out.flush();
                    out.write((". " + USR_PATH + "/etc/profile\n").getBytes());
                    out.flush();
                    out.write((cmd + "\n").getBytes());
                    out.flush();
                    out.close();
                    p.waitFor();
                    return p.exitValue();
                } catch (Exception e) {
                    Log.e(getStackTraceString(e));
                    return -1;
                }
            }

            @Override
            public String exec(String cmd) {
                try {
                    Process process = Runtime.getRuntime().exec(USR_PATH + "/bin/bash");
                    OutputStream out = process.getOutputStream();
                    out.write((". " + USR_PATH + "/etc/profile\n").getBytes());
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
                    Log.e(getStackTraceString(e));
                    return null;
                }
            }

            @Override
            public String readDatabase(int port) {
                try {
                    File DB_file = new File(DatabaseHelper.DB_NAME);
                    Socket socket = new Socket("localhost", port);
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(DB_file));
                    BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                    int len;
                    byte[] b = new byte[1024];
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while ((len = in.read(b)) != -1) {
                        out.write(b, 0, len);
                        bos.write(b, 0, len);
                    }
                    in.close();
                    out.close();
                    return getSHA256(bos.toByteArray());
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public boolean writeDatabase(int port, String sha256) {
                try {
                    File DB_file = new File(DatabaseHelper.DB_NAME);
                    File DB_file_ = new File(DatabaseHelper.DB_NAME + "_");
                    Socket socket = new Socket("localhost", port);
                    BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(DB_file_));
                    int len;
                    byte[] b = new byte[1024];
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while ((len = in.read(b)) != -1) {
                        out.write(b, 0, len);
                        bos.write(b, 0, len);
                    }
                    in.close();
                    out.close();
                    String _sha256 = getSHA256(bos.toByteArray());
                    if (_sha256 == null) return true;
                    if (sha256.toLowerCase().replaceAll(" ", "").equals(_sha256.toLowerCase().replaceAll(" ", ""))) {
                        DB_file.delete();
                        DB_file_.renameTo(DB_file);
                    }
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        };
    }

    public static String getSHA256(byte[] bytes) {
        MessageDigest messageDigest;
        String encodeStr;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bytes);
            encodeStr = byte2Hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return encodeStr;
    }

    private static String byte2Hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String temp;
        for (byte aByte : bytes) {
            temp = Integer.toHexString(aByte & 0xFF);
            if (temp.length() == 1) {
                sb.append("0");
            }
            sb.append(temp);
        }
        return sb.toString();
    }

    public void insert(CmdInfo cmdInfo) {
        ContentValues values = new ContentValues();
        values.put("id", cmdInfo.id);
        values.put("name", new String(Base64.getEncoder().encode(cmdInfo.name.getBytes())));
        values.put("command", new String(Base64.getEncoder().encode(cmdInfo.command.getBytes())));
        values.put("keepAlive", cmdInfo.keepAlive ? 1 : 0);
        values.put("useChid", cmdInfo.useChid ? 1 : 0);
        values.put("ids", cmdInfo.ids);
        database.insert(TABLE_NAME, null, values);
    }

    public void update(CmdInfo cmdInfo) {
        ContentValues values = new ContentValues();
        values.put("id", cmdInfo.id);
        values.put("name", new String(Base64.getEncoder().encode(cmdInfo.name.getBytes())));
        values.put("command", new String(Base64.getEncoder().encode(cmdInfo.command.getBytes())));
        values.put("keepAlive", cmdInfo.keepAlive ? 1 : 0);
        values.put("useChid", cmdInfo.useChid ? 1 : 0);
        values.put("ids", cmdInfo.ids);
        database.update(TABLE_NAME, values, "id=?", new String[]{String.valueOf(cmdInfo.id)});
    }

    public CmdInfo query(int id) {
        return query(String.valueOf(id));
    }

    public CmdInfo[] query(int[] ids) {
        String[] args = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            args[i] = String.valueOf(ids[i]);
        }
        return query(args);
    }

    public CmdInfo query(String id) {
        CmdInfo[] cmdInfos = query(new String[]{id});
        return cmdInfos.length == 0 ? null : cmdInfos[0];
    }

    @SuppressLint({"Range", "Recycle"})
    public CmdInfo[] query(String[] ids) {
        Cursor cursor = database.query(TABLE_NAME, null, "id=?", ids, null, null, null);
        CmdInfo[] result = new CmdInfo[cursor.getCount()];
        if (cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.move(i);
                CmdInfo cmdInfo = new CmdInfo();
                cmdInfo.id = cursor.getInt(cursor.getColumnIndex("id"));
                cmdInfo.name = new String(Base64.getDecoder().decode(cursor.getString(cursor.getColumnIndex("name")).getBytes()));
                cmdInfo.command = new String(Base64.getDecoder().decode(cursor.getString(cursor.getColumnIndex("command")).getBytes()));
                cmdInfo.keepAlive = cursor.getInt(cursor.getColumnIndex("keepAlive")) == 1;
                cmdInfo.useChid = cursor.getInt(cursor.getColumnIndex("useChid")) == 1;
                cmdInfo.ids = cursor.getString(cursor.getColumnIndex("ids"));
                result[i] = cmdInfo;
            }
        }
        return result;
    }

    @SuppressLint("Range")
    public CmdInfo[] getAll() {
        Cursor cursor = database.query(TABLE_NAME, null, null, null, null, null, null);
        CmdInfo[] result = new CmdInfo[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            CmdInfo cmdInfo = new CmdInfo();
            cmdInfo.id = cursor.getInt(cursor.getColumnIndex("id"));
            cmdInfo.name = new String(Base64.getDecoder().decode(cursor.getString(cursor.getColumnIndex("name")).getBytes()));
            cmdInfo.command = new String(Base64.getDecoder().decode(cursor.getString(cursor.getColumnIndex("command")).getBytes()));
            cmdInfo.keepAlive = cursor.getInt(cursor.getColumnIndex("keepAlive")) == 1;
            cmdInfo.useChid = cursor.getInt(cursor.getColumnIndex("useChid")) == 1;
            cmdInfo.ids = cursor.getString(cursor.getColumnIndex("ids"));
            result[i] = cmdInfo;
            i++;
        }
        cursor.close();
        return result;
    }

    public void delete(CmdInfo cmdInfo) {
        delete(cmdInfo.id);
    }

    public void delete(int id) {
        delete(String.valueOf(id));
    }

    public void delete(int[] ids) {
        String[] args = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            args[i] = String.valueOf(ids[i]);
        }
        delete(args);
    }

    public void delete(String id) {
        delete(new String[]{id});
    }

    public void delete(String[] ids) {
        database.delete(TABLE_NAME, "id=?", ids);
    }
}
