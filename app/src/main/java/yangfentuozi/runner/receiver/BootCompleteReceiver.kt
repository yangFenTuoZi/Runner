package yangfentuozi.runner.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import yangfentuozi.runner.App
import yangfentuozi.runner.Runner
import yangfentuozi.runner.service.callback.IExecResultCallback

class BootCompleteReceiver : BroadcastReceiver() {

    val TAG = "BootCompleteReceiver"
    val shizukuTimeout: Long = 100000
    val mHandler = Handler(Looper.getMainLooper())

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED != intent.action && Intent.ACTION_BOOT_COMPLETED != intent.action) {
            return
        }

        if (!App.getPreferences().getBoolean("auto_start_exec", false)) return
        if (Process.myUid() / 100000 > 0) return

        Log.i(TAG, "onReceive: $intent")

        var shizukuStarted = false
        var finished = false

        val shizukuListener = object : Runner.ShizukuStatusListener {
            override fun onChange(running: Boolean) {
                if (running && !shizukuStarted) {
                    shizukuStarted = true
                    mHandler.removeCallbacksAndMessages(null)
                    Runner.removeShizukuStatusListener(this)
                    Runner.addServiceStatusListener(object : Runner.ServiceStatusListener {
                        override fun onChange(running: Boolean) {
                            if (running) {
                                Runner.removeServiceStatusListener(this)
                                exec()
                            }
                        }
                    })
                }
            }
        }

        Runner.addShizukuStatusListener(shizukuListener)

        Runner.refreshStatus()

        mHandler.postDelayed({
            if (!shizukuStarted && !finished) {
                finished = true
                Log.i(TAG, "wait Shizuku start timeout")
                Looper.myLooper()?.quitSafely()
            }
        }, shizukuTimeout)

        Looper.prepare()
        Looper.loop()
    }

    fun exec() {
        Thread {
            val command = App.getPreferences().getString("auto_start_exec_script", "")
            val ids = App.getPreferences().getString("auto_start_exec_ids", "")
            if (command.isNullOrEmpty()) {
                Log.i(TAG, "exec: command is empty")
                return@Thread
            }
            Log.i(TAG, "exec: $command")
            Runner.service?.exec(command, ids, "ExecOnBootTask", object : IExecResultCallback.Stub() {
                override fun onOutput(outputs: String?) {
                    Log.i(TAG, "onOutput: $outputs")
                }

                override fun onExit(exitValue: Int) {
                    Log.i(TAG, "exit with $exitValue")
                    mHandler.post { Looper.myLooper()?.quitSafely() }
                }
            })
        }.start()
    }
}
