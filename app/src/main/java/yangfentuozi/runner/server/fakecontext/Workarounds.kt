package yangfentuozi.runner.server.fakecontext

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import yangfentuozi.runner.server.ServerMain
import java.lang.reflect.Constructor

@SuppressLint("PrivateApi,BlockedPrivateApi,SoonBlockedPrivateApi,DiscouragedPrivateApi")
object Workarounds {
    private var ACTIVITY_THREAD_CLASS: Class<*>? = null
    private var ACTIVITY_THREAD: Any? = null

    init {
        try {
            ACTIVITY_THREAD_CLASS = Class.forName("android.app.ActivityThread")
            val activityThreadConstructor: Constructor<*>? =
                ACTIVITY_THREAD_CLASS?.getDeclaredConstructor()
            activityThreadConstructor?.isAccessible = true
            ACTIVITY_THREAD = activityThreadConstructor?.newInstance()

            val sCurrentActivityThreadField =
                ACTIVITY_THREAD_CLASS?.getDeclaredField("sCurrentActivityThread")
            sCurrentActivityThreadField?.isAccessible = true
            sCurrentActivityThreadField?.set(null, ACTIVITY_THREAD)
        } catch (e: Exception) {
            throw AssertionError(e)
        }
    }

    val systemContext: Context?
        get() {
            try {
                val getSystemContextMethod =
                    ACTIVITY_THREAD_CLASS!!.getDeclaredMethod("getSystemContext")
                return getSystemContextMethod.invoke(ACTIVITY_THREAD) as Context?
            } catch (throwable: Throwable) {
                Log.e(
                    ServerMain.TAG,
                    "Workarounds: Failed to get system context: ${throwable.stackTraceToString()}",
                    throwable
                )
                return null
            }
        }
}