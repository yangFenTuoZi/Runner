package yangfentuozi.runner.server.fakecontext

import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.system.Os
import androidx.annotation.RequiresApi

class FakeContext private constructor() : ContextWrapper(Workarounds.systemContext) {
    override fun getPackageName(): String {
        return PACKAGE_NAME
    }

    override fun getOpPackageName(): String {
        return PACKAGE_NAME
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getAttributionSource(): AttributionSource {
        val builder = AttributionSource.Builder(Os.getuid())
        builder.setPackageName(PACKAGE_NAME)
        return builder.build()
    }

    // @Override to be added on SDK upgrade for Android 14
    @Suppress("unused")
    override fun getDeviceId(): Int {
        return 0
    }

    override fun getApplicationContext(): Context {
        return this
    }

    companion object {
        var PACKAGE_NAME: String = if (Os.getuid() == 0) "root" else "com.android.shell"
        private val INSTANCE = FakeContext()

        fun get(): FakeContext {
            return INSTANCE
        }
    }
}