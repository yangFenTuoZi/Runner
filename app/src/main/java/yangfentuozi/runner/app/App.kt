package yangfentuozi.runner.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import yangfentuozi.runner.app.data.DataRepository
import yangfentuozi.runner.app.ui.activity.CrashReportActivity
import yangfentuozi.runner.app.util.ThemeUtil
import java.io.File
import java.util.LinkedList
import kotlin.system.exitProcess


class App : Application(), Thread.UncaughtExceptionHandler {
    private val activities: MutableList<Activity> = LinkedList<Activity>()
    private lateinit var pref: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(this)
        instance = this
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(ThemeUtil.darkTheme)

        Runner.init()
        Runner.addServiceStatusListener {
            if (it) Runner.service?.syncAllData(DataRepository.Companion.getInstance(this).getAllEnvs())
        }
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
        onTerminate()
        exitProcess(0)
    }

    override fun onTerminate() {
        super.onTerminate()
        Runner.remove()
    }

    fun reinitializeDataRepository() {
        DataRepository.destroyInstance()
        DataRepository.getInstance(this)
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

        val preferences: SharedPreferences
            get() = instance.pref
    }
}