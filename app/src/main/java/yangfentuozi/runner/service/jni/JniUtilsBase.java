package yangfentuozi.runner.service.jni;

import android.annotation.SuppressLint;
import android.util.Log;

@SuppressLint("UnsafeDynamicallyLoadedCode")
public abstract class JniUtilsBase {

    private boolean libraryLoaded = false;

    public synchronized void loadLibrary() {
        if (!libraryLoaded) {
            try {
                System.load(getJniPath());
                libraryLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                Log.e("ProcessUtils", "Failed to load native library: ", e);
                libraryLoaded = false;
            }
        }
    }

    public boolean isLibraryLoaded() {
        return libraryLoaded;
    }

    public abstract String getJniPath();
}