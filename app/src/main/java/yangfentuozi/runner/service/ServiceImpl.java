package yangfentuozi.runner.service;

import static android.util.Log.getStackTraceString;

import android.ddm.DdmHandleAppName;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.system.Os;
import android.system.OsConstants;
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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import rikka.hidden.compat.PackageManagerApis;
import yangfentuozi.runner.BuildConfig;
import yangfentuozi.runner.service.callback.ExecResultCallback;
import yangfentuozi.runner.service.callback.IExecResultCallback;
import yangfentuozi.runner.service.callback.IInstallTermExtCallback;
import yangfentuozi.runner.service.callback.InstallTermExtCallback;
import yangfentuozi.runner.service.data.CommandInfo;
import yangfentuozi.runner.service.data.EnvInfo;
import yangfentuozi.runner.service.data.ProcessInfo;
import yangfentuozi.runner.service.data.TermExtVersion;
import yangfentuozi.runner.service.database.CommandDao;
import yangfentuozi.runner.service.database.DataDbHelper;
import yangfentuozi.runner.service.database.EnvironmentDao;
import yangfentuozi.runner.service.fakecontext.FakeContext;
import yangfentuozi.runner.service.jni.ProcessUtils;

public class ServiceImpl extends IService.Stub {

    public static final String TAG = "runner_server";
    public static final String DATA_PATH = "/data/local/tmp/runner";
    public static final String USR_PATH = DATA_PATH + "/usr";
    public static final String HOME_PATH = DATA_PATH + "/home";
    public static final String STARTER = HOME_PATH + "/.local/bin/starter";
    public static final String JNI_PROCESS_UTILS = HOME_PATH + "/.local/lib/libprocessutils.so";
    public final Handler mHandler;
    private final DataDbHelper dataDbHelper = new DataDbHelper(FakeContext.get());
    private final CommandDao commandDao = new CommandDao(dataDbHelper.getDatabase());
    private final EnvironmentDao environmentDao = new EnvironmentDao(dataDbHelper.getDatabase());
    private final ProcessUtils processUtils = new ProcessUtils();

    public ServiceImpl() {
        DdmHandleAppName.setAppName(TAG, Os.getuid());
        Log.i(TAG, "start");
        {
            ifExistsOrMkdirs(new File(HOME_PATH + "/.local/bin"));
            ifExistsOrMkdirs(new File(HOME_PATH + "/.local/lib"));
        }
        mHandler = new Handler();

        ZipFile app = null;
        try {
            app = new ZipFile(PackageManagerApis.getApplicationInfo(BuildConfig.APPLICATION_ID, 0, 0).sourceDir);
        } catch (RemoteException | IOException e) {
            Log.e(TAG, e instanceof RemoteException ? "get application info error" : "open apk zip file error", e);
        }

        if (app == null) {
            Log.w(TAG, "ignore unzip library from app zip file");
        } else {
            try {
                ZipEntry entry = app.getEntry("lib/" + Build.SUPPORTED_ABIS[0] + "/libstarter.so");
                if (entry != null) {
                    Log.i(TAG, "unzip starter");
                    InputStream in = app.getInputStream(entry);
                    File file = new File(STARTER);
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    copyFile(in, new FileOutputStream(file));

                    file.setExecutable(true);
                } else {
                    Log.e(TAG, "libstarter.so doesn't exist");
                }
                entry = app.getEntry("lib/" + Build.SUPPORTED_ABIS[0] + "/libprocessutils.so");
                if (entry != null) {
                    Log.i(TAG, "unzip libprocessutils.so");
                    InputStream in = app.getInputStream(entry);
                    File file = new File(JNI_PROCESS_UTILS);
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    copyFile(in, new FileOutputStream(file));

                    file.setExecutable(true);

                    processUtils.loadLibrary();
                } else {
                    Log.e(TAG, "libprocessutils.so doesn't exist");
                }
            } catch (IOException e) {
                Log.e(TAG, "unzip error", e);
            } finally {
                try {
                    app.close();
                } catch (IOException e) {
                    Log.e(TAG, "close apk zip file error", e);
                }
            }
        }
    }

    @Override
    public void destroy() {
        Log.i(TAG, "stop");
        dataDbHelper.close();
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
    public void exec(String cmd, String ids, String procName, IExecResultCallback callback) {
        new Thread(() -> {
            var callbackWrapper = new ExecResultCallback(callback);
            try {
                var finalIds = ids == null || ids.isEmpty() ? "-1" : ids;
                var finalProcName = procName == null || procName.isEmpty() ? "execTask" : procName;
                ProcessBuilder processBuilder = new ProcessBuilder(STARTER, finalIds, finalProcName);
                Map<String, String> processEnv = processBuilder.environment();
                var customEnv = getAllEnv();
                processEnv.put("PREFIX", USR_PATH);
                processEnv.put("HOME", HOME_PATH);
                processEnv.put("TMPDIR", USR_PATH + "/tmp");
                processEnv.merge("PATH", HOME_PATH + "/.local/bin:" + USR_PATH + "/bin:" + USR_PATH + "/bin/applets", (oldValue, newValue) -> newValue + ":" + oldValue);
                processEnv.merge("LD_LIBRARY_PATH", HOME_PATH + "/.local/lib:" + USR_PATH + "/lib", (oldValue, newValue) -> newValue + ":" + oldValue);

                for (EnvInfo entry : customEnv) {
                    processEnv.merge(entry.key, entry.value, (oldValue, newValue) ->
                            newValue.replaceAll(String.format("\\$(%1$s|\\{%1$s\\})", Pattern.quote(entry.key)), oldValue));
                }
                processBuilder.redirectErrorStream(true);
                Process p = processBuilder.start();
                OutputStream out = p.getOutputStream();
                out.write(String.format("""
                        echo $$;. %s/etc/profile;%s;exit
                        """, USR_PATH, cmd).getBytes());
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
    public void deleteEnv(String key) {
        environmentDao.delete(key);
    }

    @Override
    public boolean insertEnv(String key, String value) {
        return environmentDao.insert(key, value);
    }

    @Override
    public boolean updateEnv(EnvInfo from, EnvInfo to) {
        return environmentDao.update(from.key, from.value, to.key, to.value);
    }

    @Override
    public String getEnv(String key) {
        return environmentDao.getValue(key);
    }

    @Override
    public EnvInfo[] getAllEnv() {
        return environmentDao.getAll().toArray(new EnvInfo[0]);
    }

    @Override
    public ProcessInfo[] getProcesses() {
        if (processUtils.isLibraryLoaded()) {
            Log.i(TAG, "get processes");
            return processUtils.getProcesses();
        } else {
            Log.e(TAG, "process utils library not loaded");
            return null;
        }
    }

    @Override
    public boolean[] sendSignal(int[] pid, int signal) {
        if (processUtils.isLibraryLoaded()) {
            boolean[] result = new boolean[pid.length];
            for (int i = 0; i < pid.length; i++) {
                Log.i(TAG, "kill process: " + pid[i]);
                result[i] = processUtils.sendSignal(pid[i], signal);
            }
            return result;
        } else {
            Log.e(TAG, "process utils library not loaded");
            return null;
        }
    }

    @Override
    public void installTermExt(String termExtZip, IInstallTermExtCallback callback) {
        new Thread(() -> {
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
                        if (zipEntry.isDirectory()) {
                            Log.i(TAG, "unzip '" + zipEntry.getName() + "' to '" + DATA_PATH + "/install_temp/" + zipEntry.getName() + "'");
                            callbackWrapper.onMessage(" - Unzip '" + zipEntry.getName() + "' to '" + DATA_PATH + "/install_temp/" + zipEntry.getName() + "'");
                            File file = new File(DATA_PATH + "/install_temp/" + zipEntry.getName());
                            if (!file.exists()) {
                                file.mkdirs();
                            }
                        } else {
                            File file = new File(DATA_PATH + "/install_temp/" + zipEntry.getName());
                            Log.i(TAG, "unzip '" + zipEntry.getName() + "' to '" + file.getAbsolutePath() + "'");
                            callbackWrapper.onMessage(" - Unzip '" + zipEntry.getName() + "' to '" + file.getAbsolutePath() + "'");
                            if (!Objects.requireNonNull(file.getParentFile()).exists())
                                file.getParentFile().mkdirs();
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            copyFile(app.getInputStream(zipEntry), new FileOutputStream(file));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "unable to unzip file: " + zipEntry.getName(), e);
                        callbackWrapper.onMessage(" ! Unable to unzip file: " + zipEntry.getName() + "\n" + getStackTraceString(e));
                        callbackWrapper.onMessage(" - Cleanup " + DATA_PATH + "/install_temp");
                        rmRF(new File(DATA_PATH + "/install_temp"));
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
                    callbackWrapper.onMessage(" - Clean up " + DATA_PATH + "/install_temp");
                    rmRF(new File(DATA_PATH + "/install_temp"));
                    callbackWrapper.onExit(false);
                    return;
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
                        callbackWrapper.onMessage(" ! Install script exit with non-zero value " + ev);
                        callbackWrapper.onMessage(" - Cleanup " + DATA_PATH + "/install_temp");
                        rmRF(new File(DATA_PATH + "/install_temp"));
                        callbackWrapper.onExit(false);
                        return;
                    }
                } catch (InterruptedException | IOException e) {
                    callbackWrapper.onMessage(" ! " + getStackTraceString(e));
                    callbackWrapper.onMessage(" - Cleanup " + DATA_PATH + "/install_temp");
                    rmRF(new File(DATA_PATH + "/install_temp"));
                    callbackWrapper.onExit(false);
                    return;
                }
                callbackWrapper.onMessage(" - Cleanup " + DATA_PATH + "/install_temp");
                rmRF(new File(DATA_PATH + "/install_temp"));
                Log.i(TAG, "finish");
                callbackWrapper.onMessage(" - Finish");
                callbackWrapper.onExit(true);
            } catch (IOException e) {
                Log.e(TAG, "read terminal extension file error!", e);
                callbackWrapper.onMessage(" ! Read terminal extension file error!\n" + getStackTraceString(e));
                callbackWrapper.onExit(false);
            }
        }).start();
    }

    @Override
    public void removeTermExt() {
        Log.i(TAG, "remove terminal extension");
        rmRF(new File(USR_PATH));
        Log.i(TAG, "finish");
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

    public static void ifExistsOrMkdirs(File file) {
        if (!file.exists())
            file.mkdirs();
    }

    public static void copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        BufferedInputStream in = new BufferedInputStream(inputStream);
        BufferedOutputStream out = new BufferedOutputStream(outputStream);
        int len;
        byte[] b = new byte[PAGE_SIZE];
        while ((len = in.read(b)) != -1) {
            out.write(b, 0, len);
        }
        in.close();
        out.close();
    }

    public static final int PAGE_SIZE = (int) Os.sysconf(OsConstants._SC_PAGESIZE);
}
