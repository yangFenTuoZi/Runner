package rikka.rish;

import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import yangfentuozi.runner.server.IRishService;

public class RishService extends IRishService.Stub {

    private static final String TAG = "RishService";

    private static final Map<Integer, RishHost> HOSTS = new HashMap<>();

    private static final boolean IS_ROOT = Os.getuid() == 0;

    @Override
    public void createHost(
            String[] args, String[] env, String dir,
            byte tty,
            ParcelFileDescriptor stdin, ParcelFileDescriptor stdout, ParcelFileDescriptor stderr) {

        int callingPid = Binder.getCallingPid();

        // Termux app set PATH and LD_PRELOAD to Termux's internal path.
        // Adb does not have sufficient permissions to access such places.

        // Under adb, users need to set RISH_PRESERVE_ENV=1 to preserve env.
        // Under root, keep env unless RISH_PRESERVE_ENV=0 is set.

        boolean allowEnv = IS_ROOT;
        for (String e : env) {
            if ("RISH_PRESERVE_ENV=1".equals(e)) {
                allowEnv = true;
                break;
            } else if ("RISH_PRESERVE_ENV=0".equals(e)) {
                allowEnv = false;
                break;
            }
        }
        if (!allowEnv) {
            env = null;
        }

        RishHost host = new RishHost(args, env, dir, tty, stdin, stdout, stderr);
        host.start();
        Log.d(TAG, "Forked " + host.getPid());

        HOSTS.put(callingPid, host);
    }

    @Override
    public void setWindowSize(long size) {
        int callingPid = Binder.getCallingPid();

        RishHost host = HOSTS.get(callingPid);
        if (host == null) {
            Log.d(TAG, "Not existing host created by " + callingPid);
            return;
        }

        host.setWindowSize(size);
    }

    @Override
    public int getExitCode() {
        int callingPid = Binder.getCallingPid();

        RishHost host = HOSTS.get(callingPid);
        if (host == null) {
            Log.d(TAG, "Not existing host created by " + callingPid);
            return -1;
        }

        return host.getExitCode();
    }

}
