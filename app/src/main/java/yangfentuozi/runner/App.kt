package yangfentuozi.runner

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import yangfentuozi.runner.ui.activity.CrashReportActivity
import yangfentuozi.runner.util.ThemeUtil
import java.io.File
import java.util.LinkedList


class App : Application(), Thread.UncaughtExceptionHandler {
    private val activities: MutableList<Activity> = LinkedList<Activity>()
    private lateinit var pref: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        instance = this
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(ThemeUtil.darkTheme)

        Runner.init()
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
                .putExtra("crash_info", e.stackTraceToString())
                .putExtra("crash_file", file.absolutePath)
        )
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Thread {
            Looper.prepare()
            crashHandler(t, e)
            Looper.loop()
        }.start()
    }

    companion object {
        lateinit var instance: App
            private set

        fun getPreferences(): SharedPreferences {
            return instance.pref
        }
    }
}