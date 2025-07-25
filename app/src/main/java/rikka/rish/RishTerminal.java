package rikka.rish;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RishTerminal {

    private static final String TAG = "RishTerminal";

    public static int getFd(FileDescriptor[] fileDescriptor, int i) {
        if (fileDescriptor == null) {
            return -1;
        }
        return FileDescriptors.getFd(fileDescriptor[i]);
    }

    public static void closeFd(FileDescriptor[] fileDescriptor, int i) {
        if (fileDescriptor == null) {
            return;
        }
        FileDescriptors.closeSilently(fileDescriptor[i]);
    }

    private final String[] argv;
    private final byte tty;
    private FileDescriptor[] stdin;
    private FileDescriptor[] stdout;
    private FileDescriptor[] stderr;
    private int ttyFd = -1;
    private int exitCode;

    public RishTerminal(String[] argv) throws ErrnoException, RemoteException {
        this.argv = argv;
        this.tty = prepare();

        createHost();
    }

    private void createHost() throws ErrnoException, RemoteException {
        Log.d(TAG, "createHost");

        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            list.add(entry.getKey() + "=" + entry.getValue());
        }
        String[] env = list.toArray(new String[0]);

        String dir = new File("").getAbsolutePath();

        try {
            stdin = Os.pipe();
            stdout = Os.pipe();
            if ((tty & RishConstants.ATTY_ERR) == 0) {
                stderr = Os.pipe();
            }
            RishConfig.getService().createHost(argv, env, dir, tty,
                    ParcelFileDescriptor.dup(stdin[0]),
                    ParcelFileDescriptor.dup(stdout[1]),
                    stderr != null ? ParcelFileDescriptor.dup(stderr[1]) : null);
        } catch (IOException e) {
            throw (ErrnoException) e.getCause();
        } finally {
            closeFd(stdin, 0);
            closeFd(stdout, 1);
            closeFd(stderr, 1);
        }
    }

    public void start() {
        Log.d(TAG, "start");

        ttyFd = start(tty, getFd(stdin, 1), getFd(stdout, 0), getFd(stdout, 0));

        if (ttyFd != -1) {
            new Thread(() -> {
                while (true) {
                    Log.d(TAG, "waitForWindowSizeChange");

                    try {
                        long size = waitForWindowSizeChange(ttyFd);
                        setWindowSize(size);
                    } catch (Throwable e) {
                        Log.w(TAG, Log.getStackTraceString(e));
                    }
                }
            }).start();
        }
    }

    private void setWindowSize(long size) throws RemoteException {
        Log.d(TAG, "setWindowSize");
        RishConfig.getService().setWindowSize(size);
    }

    private int requestExitCode() throws RemoteException {
        Log.d(TAG, "requestExitCode");
        return RishConfig.getService().getExitCode();
    }

    public int waitFor() {
        Log.d(TAG, "waitFor");

        waitForProcessExit();
        try {
            exitCode = requestExitCode();
        } catch (Throwable e) {
            Log.w(TAG, Log.getStackTraceString(e));
            exitCode = -1;
        }
        return exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

    private static native byte prepare();

    private static native int start(byte tty, int stdin, int stdout, int stderr);

    private static native long waitForWindowSizeChange(int fd);

    private static native void waitForProcessExit();
}
