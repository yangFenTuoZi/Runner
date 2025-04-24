package yangFenTuoZi.runner.plus

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Environment
import android.os.Looper
import android.util.Log
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import yangFenTuoZi.runner.plus.ui.activity.CrashReportActivity
import yangFenTuoZi.runner.plus.utils.ThemeUtils.getTheme
import yangFenTuoZi.runner.plus.utils.ThemeUtils.isDark
import java.io.File
import java.util.LinkedList

class App : Application(), Thread.UncaughtExceptionHandler {
    val dynamicColorsOptions: DynamicColorsOptions = DynamicColorsOptions.Builder().build()
    private val activities: MutableList<Activity> = LinkedList<Activity>()
    var isDark: Int = -1
    override fun onCreate() {
        super.onCreate()
        instance = this
        Runner.init()
        val isDark = isDark(this)
        this.isDark = if (isDark) 1 else 0
        setTheme(getTheme(isDark))
        DynamicColors.applyToActivitiesIfAvailable(this, this.dynamicColorsOptions)
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    fun addActivity(activity: Activity?) {
        activities.add(activity!!)
    }

    fun removeActivity(activity: Activity?) {
        activities.remove(activity)
    }

    fun finishApp() {
        for (activity in activities) activity.finish()
        activities.clear()
    }

    override fun onTerminate() {
        super.onTerminate()
        Runner.remove()
    }

    @SuppressLint("WrongConstant")
    private fun crashHandler(t: Thread, e: Throwable) {
        val fileName = "runnerCrash-" + System.currentTimeMillis() + ".log"
        val file: File?
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val dir =
                File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            file = File(dir, fileName)
        } else {
            file = getExternalFilesDir(fileName)
        }
        checkNotNull(file)

        startActivity(
            Intent(this, CrashReportActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("crash_info", Log.getStackTraceString(e))
                .putExtra("crash_file", file.absolutePath)
        )
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Thread(Runnable {
            Looper.prepare()
            crashHandler(t, e)
            Looper.loop()
        }).start()
    }

    companion object {
        var instance: App? = null
            private set
    }
}