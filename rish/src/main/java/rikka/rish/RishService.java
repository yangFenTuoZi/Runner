package rikka.rish;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RishService extends IRishService.Stub {

    private static final String TAG = "RishService";

    private static final Map<Integer, RishHost> HOSTS = new HashMap<>();

    @NonNull
    private String[] customEnv = new String[0];

    @Override
    public int createHost(
            String[] args, String term, String dir,
            byte tty,
            ParcelFileDescriptor stdin, ParcelFileDescriptor stdout, ParcelFileDescriptor stderr) {

        ArrayList<String> finalEnv = new ArrayList<>(customEnv.length + 1);
        finalEnv.addAll(Arrays.asList(customEnv));
        finalEnv.removeIf(s -> s.startsWith("TERM="));
        finalEnv.add("TERM=" + term);
        RishHost host = new RishHost(args, finalEnv.toArray(new String[0]), dir, tty, stdin, stdout, stderr);
        host.start();
        Log.d(TAG, "Forked " + host.getPid());

        HOSTS.put(host.getPid(), host);
        return host.getPid();
    }

    @Override
    public void setWindowSize(int hostPid, long size) {
        RishHost host = HOSTS.get(hostPid);
        if (host == null) {
            Log.d(TAG, "Not existing host: " + hostPid);
            return;
        }

        host.setWindowSize(size);
    }

    @Override
    public int getExitCode(int hostPid) {
        RishHost host = HOSTS.get(hostPid);
        if (host == null) {
            Log.d(TAG, "Not existing host: " + hostPid);
            return -1;
        }

        return host.getExitCode();
    }

    @Override
    public int[] getAllHost() {
        int[] pids = new int[HOSTS.size()];
        int i = 0;
        for (Integer pid : HOSTS.keySet()) {
            pids[i++] = pid;
        }
        return pids;
    }

    @Override
    public void releaseHost(int hostPid) {
        RishHost host = HOSTS.get(hostPid);
        if (host == null) {
            Log.d(TAG, "Not existing host: " + hostPid);
            return;
        }
        if (host.getExitCode() == Integer.MIN_VALUE) {
            Log.d(TAG, "Killing host " + hostPid);
            try {
                Os.kill(hostPid, 9);
            } catch (ErrnoException e) {
                Log.e(TAG, "Failed to kill host " + hostPid, e);
            }
        }
        HOSTS.remove(hostPid);
    }

    public void updateEnv(@NonNull String[] envs) {
        customEnv = envs;
    }
}
