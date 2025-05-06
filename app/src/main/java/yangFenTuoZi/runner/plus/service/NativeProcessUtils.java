package yangFenTuoZi.runner.plus.service;

import android.annotation.SuppressLint;
import android.util.Log;

import yangFenTuoZi.runner.plus.service.data.ProcessInfo;

@SuppressLint("UnsafeDynamicallyLoadedCode")
public class NativeProcessUtils {

    private static boolean libraryLoaded = false;

    public static synchronized void loadLibrary() {
        if (!libraryLoaded) {
            try {
                System.load(ServiceImpl.JNI_PROCESS_UTILS);
                libraryLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                Log.e("NativeProcessUtils", "Failed to load native library: ", e);
                throw e;
            }
        }
    }

    public static native boolean sendSignal(int pid, int signal);

    public static native ProcessInfo[] getProcesses();
}