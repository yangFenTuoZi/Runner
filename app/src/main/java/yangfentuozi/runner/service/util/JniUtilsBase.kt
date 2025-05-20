package yangfentuozi.runner.service.util

import android.annotation.SuppressLint
import android.util.Log

@SuppressLint("UnsafeDynamicallyLoadedCode")
abstract class JniUtilsBase {
    var isLibraryLoaded: Boolean = false
        private set

    @Synchronized
    fun loadLibrary() {
        if (!this.isLibraryLoaded) {
            try {
                System.load(this.jniPath)
                this.isLibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e("ProcessUtils", "Failed to load native library: ", e)
                this.isLibraryLoaded = false
            }
        }
    }

    abstract val jniPath: String
}