package yangfentuozi.runner.server.util

import android.util.Log
import yangfentuozi.runner.BuildConfig
import java.io.IOException
import java.util.Locale
import java.util.logging.FileHandler
import java.util.logging.SimpleFormatter

class Logger(
    private val tag: String
) {
    companion object {
        private val mFileHandler = FileHandler(file).apply {
            formatter = SimpleFormatter()
        }
    }

    private val javaLogger = java.util.logging.Logger.getLogger(tag)

    init {
        try {
            javaLogger.addHandler(mFileHandler)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun isLoggable(level: Int): Boolean {
        if (BuildConfig.DEBUG) return true
        return level >= Log.INFO
    }

    fun v(msg: String) {
        if (!isLoggable(Log.VERBOSE)) return
        println(Log.VERBOSE, msg)
    }

    fun v(fmt: String, vararg args: Any?) {
        if (isLoggable(Log.VERBOSE)) {
            println(Log.VERBOSE, fmt.format(Locale.ENGLISH, *args))
        }
    }

    fun v(msg: String?, tr: Throwable?) {
        if (isLoggable(Log.VERBOSE)) {
            println(Log.VERBOSE, msg + '\n' + Log.getStackTraceString(tr))
        }
    }

    fun d(msg: String) {
        if (isLoggable(Log.DEBUG)) {
            println(Log.DEBUG, msg)
        }
    }

    fun d(fmt: String, vararg args: Any?) {
        if (isLoggable(Log.DEBUG)) {
            println(Log.DEBUG, fmt.format(Locale.ENGLISH, *args))
        }
    }

    fun d(msg: String?, tr: Throwable?) {
        if (isLoggable(Log.DEBUG)) {
            println(Log.DEBUG, msg + '\n' + Log.getStackTraceString(tr))
        }
    }

    fun i(msg: String) {
        if (isLoggable(Log.INFO)) {
            println(Log.INFO, msg)
        }
    }

    fun i(fmt: String, vararg args: Any?) {
        if (isLoggable(Log.INFO)) {
            println(Log.INFO, fmt.format(Locale.ENGLISH, *args))
        }
    }

    fun i(msg: String?, tr: Throwable?) {
        if (isLoggable(Log.INFO)) {
            println(Log.INFO, msg + '\n' + Log.getStackTraceString(tr))
        }
    }

    fun w(msg: String) {
        if (isLoggable(Log.WARN)) {
            println(Log.WARN, msg)
        }
    }

    fun w(fmt: String, vararg args: Any?) {
        if (isLoggable(Log.WARN)) {
            println(Log.WARN, fmt.format(Locale.ENGLISH, *args))
        }
    }

    fun w(tr: Throwable?, fmt: String, vararg args: Any?) {
        if (isLoggable(Log.WARN)) {
            println(
                Log.WARN,
                fmt.format(Locale.ENGLISH, *args) + '\n' + Log.getStackTraceString(tr)
            )
        }
    }

    fun w(msg: String?, tr: Throwable?) {
        if (isLoggable(Log.WARN)) {
            println(Log.WARN, msg + '\n' + Log.getStackTraceString(tr))
        }
    }

    fun e(msg: String) {
        if (isLoggable(Log.ERROR)) {
            println(Log.ERROR, msg)
        }
    }

    fun e(fmt: String, vararg args: Any?) {
        if (isLoggable(Log.ERROR)) {
            println(Log.ERROR, fmt.format(Locale.ENGLISH, *args))
        }
    }

    fun e(msg: String?, tr: Throwable?) {
        if (isLoggable(Log.ERROR)) {
            println(Log.ERROR, msg + '\n' + Log.getStackTraceString(tr))
        }
    }

    fun e(tr: Throwable?, fmt: String, vararg args: Any?) {
        if (isLoggable(Log.ERROR)) {
            println(
                Log.ERROR,
                fmt.format(Locale.ENGLISH, *args) + '\n' + Log.getStackTraceString(tr)
            )
        }
    }

    fun println(priority: Int, msg: String): Int {
        javaLogger.info(msg)
        return Log.println(priority, tag, msg)
    }
}