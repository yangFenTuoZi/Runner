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
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import yangFenTuoZi.runner.plus.BuildConfig;
import yangFenTuoZi.runner.plus.service.data.CommandDao;
import yangFenTuoZi.runner.plus.service.data.CommandDbHelper;
import yangFenTuoZi.runner.plus.service.data.CommandInfo;
import yangFenTuoZi.runner.plus.service.fakecontext.FakeContext;

public class ServiceImpl extends IService.Stub {

    public static final String TAG = "runner_server";
    public static final String DATA_PATH = "/data/local/tmp/runner";
    public static final String USR_PATH = DATA_PATH + "/usr";
    public final Handler mHandler;
    private CommandDbHelper dbHelper = new CommandDbHelper(FakeContext.get());
    private CommandDao commandDao;

    public ServiceImpl() {
        DdmHandleAppName.setAppName(TAG, Os.getuid());
        Log.i(TAG, "start");
        mHandler = new Handler();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        commandDao = new CommandDao(db);
    }

    @Override
    public void destroy() throws RemoteException {
        Log.i(TAG, "stop");
        dbHelper.close();
        System.exit(0);
    }

    @Override
    public void exit() throws RemoteException {
        destroy();
    }

    @Override
    public int version() throws RemoteException {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public int execX(String cmd, String procName, int port) throws RemoteException {
        return 0;
    }

    @Override
    public String exec(String cmd) throws RemoteException {
        return "";
    }

    @Override
    public int size() throws RemoteException {
        return commandDao.size();
    }

    @Override
    public CommandInfo read(int position) throws RemoteException {
        return commandDao.read(position);
    }

    @Override
    public CommandInfo[] readAll() throws RemoteException {
        return commandDao.readAll();
    }

    @Override
    public void delete(int position) throws RemoteException {
        commandDao.delete(position);
    }

    @Override
    public void edit(CommandInfo cmdInfo, int position) throws RemoteException {
        commandDao.edit(cmdInfo, position);
    }

    @Override
    public void insert(CommandInfo cmdInfo) throws RemoteException {
        commandDao.insert(cmdInfo);
    }

    @Override
    public void move(int fromPosition, int toPosition) throws RemoteException {
        commandDao.move(fromPosition, toPosition);
    }

    @Override
    public boolean backupData(String input, boolean includeTerm) throws RemoteException {
        return false;
    }

    @Override
    public boolean restoreData(String output) throws RemoteException {
        return false;
    }

    @Override
    public void installTermExt(String termExtZip) throws RemoteException {
        try {
            Log.i(TAG, "install terminal extension: " + termExtZip);
            ZipFile app = new ZipFile(termExtZip);
            ZipEntry BP = app.getEntry("build.prop"), IS = app.getEntry("install.sh");
            if (BP == null) {
                Log.e(TAG, "'build.prop' doesn't exist");
                throw new RemoteException("'build.prop' doesn't exist");
            }
            if (IS == null) {
                Log.e(TAG, "'install.sh' doesn't exist");
                throw new RemoteException("'install.sh' doesn't exist");
            }
            InputStream buildProp = app.getInputStream(BP);
            TermExtVersion termExtVersion = new TermExtVersion(buildProp);
            buildProp.close();
            Log.i(TAG, String.format("""
                    terminal extension:
                    version: %s (%d)
                    abi: %s
                    """, termExtVersion.versionName, termExtVersion.versionCode, termExtVersion.abi));
            int indexOf = Arrays.asList(Build.SUPPORTED_ABIS).indexOf(termExtVersion.abi);
            if (indexOf == -1) {
                Log.e(TAG, "unsupported ABI: " + termExtVersion.abi);
                throw new RemoteException("unsupported ABI: " + termExtVersion.abi);
            } else if (indexOf != 0) {
                Log.w(TAG, "ABI is not preferred: " + termExtVersion.abi);
            }

            Log.i(TAG, "unzip files.");
            Enumeration<? extends ZipEntry> entries = app.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                try {
                    File file = new File(DATA_PATH + "/install_temp/" + zipEntry.getName());
                    Log.i(TAG, "unzip '" + zipEntry.getName() + "' to '" + file.getAbsolutePath() + "'");
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
                    throw new RemoteException("unable to unzip file: " + zipEntry.getName() + "\n" + Log.getStackTraceString(e));
                }
            }
            Log.i(TAG, "complete unzipping");

            String installScript = DATA_PATH + "/install_temp/install.sh";
            if (!new File(installScript).setExecutable(true)) {
                Log.e(TAG, "unable to set executable");
                throw new RemoteException("Unable to set executable");
            }
            Log.i(TAG, "execute install script");
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
                }
                br.close();
                int ev = process.waitFor();
                if (ev == 0)
                    Log.i(TAG, "exit with 0");
                else {
                    Log.e(TAG, "exit with non-zero value " + ev);
                    rmRF(new File(DATA_PATH + "/install_temp"));
                    throw new RemoteException("install script exit with non-zero value " + ev);
                }
            } catch (Exception e) {
                rmRF(new File(DATA_PATH + "/install_temp"));
                throw new RemoteException(Log.getStackTraceString(e));
            }
            rmRF(new File(DATA_PATH + "/install_temp"));
            Log.i(TAG, "finish");
        } catch (IOException e) {
            Log.e(TAG, "read terminal extension file error!", e);
            throw new RemoteException("read terminal extension file error!\n" + Log.getStackTraceString(e));
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
