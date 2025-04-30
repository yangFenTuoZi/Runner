package yangFenTuoZi.runner.plus.service;

import static android.util.Log.getStackTraceString;

import android.database.sqlite.SQLiteDatabase;
import android.ddm.DdmHandleAppName;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import yangFenTuoZi.runner.plus.BuildConfig;
import yangFenTuoZi.runner.plus.service.callback.ExecResultCallback;
import yangFenTuoZi.runner.plus.service.callback.IExecResultCallback;
import yangFenTuoZi.runner.plus.service.callback.IInstallTermExtCallback;
import yangFenTuoZi.runner.plus.service.callback.InstallTermExtCallback;
import yangFenTuoZi.runner.plus.service.data.CommandDao;
import yangFenTuoZi.runner.plus.service.data.CommandDbHelper;
import yangFenTuoZi.runner.plus.service.data.CommandInfo;
import yangFenTuoZi.runner.plus.service.data.TermExtVersion;
import yangFenTuoZi.runner.plus.service.fakecontext.FakeContext;

public class ServiceImpl extends IService.Stub {

    public static final String TAG = "runner_server";
    public static final String DATA_PATH = "/data/local/tmp/runner";
    public static final String USR_PATH = DATA_PATH + "/usr";
    public final Handler mHandler;
    private final CommandDbHelper dbHelper = new CommandDbHelper(FakeContext.get());
    private final CommandDao commandDao;

    public ServiceImpl() {
        DdmHandleAppName.setAppName(TAG, Os.getuid());
        Log.i(TAG, "start");
        mHandler = new Handler();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        commandDao = new CommandDao(db);
    }

    @Override
    public void destroy() {
        Log.i(TAG, "stop");
        dbHelper.close();
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    @Override
    public int version() throws RemoteException {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public void execX(String cmd, String procName, IExecResultCallback callback) {
        new Thread(() -> {
            var callbackWrapper = new ExecResultCallback(callback);
            try {
                Process p = Runtime.getRuntime().exec(USR_PATH + "/bin/bash");
                OutputStream out = p.getOutputStream();
                out.write((USR_PATH + "/bin/bash 2>&1\n").getBytes());
                out.flush();
                out.write("echo $$\n".getBytes());
                out.flush();
                out.write((". " + USR_PATH + "/etc/profile\n").getBytes());
                out.flush();
                out.write((cmd + "\n").getBytes());
                out.flush();
                out.close();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null)
                    callbackWrapper.onOutput(line);
                callbackWrapper.onExit(p.waitFor());
            } catch (InterruptedException | IOException e) {
                Log.e(TAG, getStackTraceString(e));
                callbackWrapper.onOutput(" ! Exception: " + getStackTraceString(e));
            }
        }).start();
    }

    @Override
    public String exec(String cmd) {
        return "";
    }

    @Override
    public int size() {
        return commandDao.size();
    }

    @Override
    public CommandInfo read(int position) {
        return commandDao.read(position);
    }

    @Override
    public CommandInfo[] readAll() {
        return commandDao.readAll();
    }

    @Override
    public void delete(int position) {
        commandDao.delete(position);
    }

    @Override
    public void edit(CommandInfo cmdInfo, int position) {
        commandDao.edit(cmdInfo, position);
    }

    @Override
    public void insert(CommandInfo cmdInfo) {
        commandDao.insert(cmdInfo);
    }

    @Override
    public void move(int fromPosition, int toPosition) {
        commandDao.move(fromPosition, toPosition);
    }

    @Override
    public void insertInto(CommandInfo cmdInfo, int position) {
        commandDao.insertInto(cmdInfo, position);
    }

    @Override
    public boolean backupData(String input, boolean includeTerm) {
        return false;
    }

    @Override
    public boolean restoreData(String output) {
        return false;
    }

    @Override
    public void installTermExt(String termExtZip, IInstallTermExtCallback callback) {
        var callbackWrapper = new InstallTermExtCallback(callback);
        try {
            Log.i(TAG, "install terminal extension: " + termExtZip);
            callbackWrapper.onMessage(" - Install terminal extension: " + termExtZip);

            ZipFile app = new ZipFile(termExtZip);
            ZipEntry BP = app.getEntry("build.prop"), IS = app.getEntry("install.sh");
            if (BP == null) {
                Log.e(TAG, "'build.prop' doesn't exist");
                callbackWrapper.onMessage(" ! 'build.prop' doesn't exist");
                callbackWrapper.onExit(false);
                return;
            }
            if (IS == null) {
                Log.e(TAG, "'install.sh' doesn't exist");
                callbackWrapper.onMessage(" ! 'install.sh' doesn't exist");
                callbackWrapper.onExit(false);
                return;
            }
            InputStream buildProp = app.getInputStream(BP);
            TermExtVersion termExtVersion = new TermExtVersion(buildProp);
            buildProp.close();
            Log.i(TAG, String.format(Locale.getDefault(), """
                    terminal extension:
                    version: %s (%d)
                    abi: %s
                    """, termExtVersion.versionName, termExtVersion.versionCode, termExtVersion.abi));
            callbackWrapper.onMessage(String.format(Locale.getDefault(), """
                    - Terminal extension:
                    - Version: %s (%d)
                    - ABI: %s
                    """, termExtVersion.versionName, termExtVersion.versionCode, termExtVersion.abi));

            int indexOf = Arrays.asList(Build.SUPPORTED_ABIS).indexOf(termExtVersion.abi);
            if (indexOf == -1) {
                Log.e(TAG, "unsupported ABI: " + termExtVersion.abi);
                callbackWrapper.onMessage(" ! Unsupported ABI: " + termExtVersion.abi);
                callbackWrapper.onExit(false);
                return;
            } else if (indexOf != 0) {
                Log.w(TAG, "ABI is not preferred: " + termExtVersion.abi);
                callbackWrapper.onMessage(" - ABI is not preferred: " + termExtVersion.abi);
            }

            Log.i(TAG, "unzip files.");
            callbackWrapper.onMessage(" - Unzip files.");
            Enumeration<? extends ZipEntry> entries = app.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                try {
                    File file = new File(DATA_PATH + "/install_temp/" + zipEntry.getName());
                    Log.i(TAG, "unzip '" + zipEntry.getName() + "' to '" + file.getAbsolutePath() + "'");
                    callbackWrapper.onMessage(" - Unzip '" + zipEntry.getName() + "' to '" + file.getAbsolutePath() + "'");
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
                    Log.e(TAG, "unable to unzip file: " + zipEntry.getName(), e);
                    callbackWrapper.onMessage(" ! Unable to unzip file: " + zipEntry.getName() + "\n" + Log.getStackTraceString(e));
                    callbackWrapper.onExit(false);
                    return;
                }
            }
            Log.i(TAG, "complete unzipping");
            callbackWrapper.onMessage(" - Complete unzipping");

            String installScript = DATA_PATH + "/install_temp/install.sh";
            if (!new File(installScript).setExecutable(true)) {
                Log.e(TAG, "unable to set executable");
                callbackWrapper.onMessage(" ! Unable to set executable");
            }
            Log.i(TAG, "execute install script");
            callbackWrapper.onMessage(" - Execute install script");
            try {
                Process process = Runtime.getRuntime().exec("/system/bin/sh");
                OutputStream out = process.getOutputStream();
                out.write((installScript + " 2>&1\n").getBytes());
                out.flush();
                out.close();
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    Log.i(TAG, "output: " + line);
                    callbackWrapper.onMessage(" - ScriptOuts: " + line);
                }
                br.close();
                int ev = process.waitFor();
                if (ev == 0) {
                    Log.i(TAG, "exit with 0");
                    callbackWrapper.onMessage(" - Install script exit successfully");
                } else {
                    Log.e(TAG, "exit with non-zero value " + ev);
                    rmRF(new File(DATA_PATH + "/install_temp"));
                    callbackWrapper.onMessage(" ! Install script exit with non-zero value " + ev);
                    callbackWrapper.onExit(false);
                }
            } catch (InterruptedException | IOException e) {
                rmRF(new File(DATA_PATH + "/install_temp"));
                callbackWrapper.onMessage(" ! " + Log.getStackTraceString(e));
                callbackWrapper.onExit(false);
            }
            rmRF(new File(DATA_PATH + "/install_temp"));
            Log.i(TAG, "finish");
            callbackWrapper.onMessage(" - Finish");
            callbackWrapper.onExit(true);
        } catch (IOException e) {
            Log.e(TAG, "read terminal extension file error!", e);
            callbackWrapper.onMessage(" ! Read terminal extension file error!\n" + Log.getStackTraceString(e));
            callbackWrapper.onExit(false);
        }
    }

    @Override
    public TermExtVersion getTermExtVersion() throws RemoteException {
        File buildProp = new File(USR_PATH + "/build.prop");
        TermExtVersion result = null;
        if (buildProp.exists() && buildProp.isFile()) {
            try (FileInputStream in = new FileInputStream(buildProp)) {
                result = new TermExtVersion(in);
            } catch (IOException e) {
                Log.e(TAG, "getTermExtVersion error", e);
                throw new RemoteException(getStackTraceString(e));
            }
        }
        return result == null ? new TermExtVersion("", -1, "") : result;
    }

    public static void rmRF(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    rmRF(f);
                }
            }
        }
        file.delete();
    }
}
