package yangfentuozi.runner.app.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.server.callback.IExecResultCallback
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ExecOnBootService : Service() {

    companion object {
        const val TAG = "ExecOnBootService"
        var isRunning = false
    }

    private lateinit var executor: ExecutorService

    val mHandler = Handler(Looper.getMainLooper())

    val timeout: Long = 20_000L

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "service created")
        executor = Executors.newFixedThreadPool(1)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "service starting command")
        isRunning = true

        executor.execute {
            Runner.refreshStatus()
            if (Runner.waitShizuku(timeout)) {
                Log.i(TAG, "Shizuku is ready")
                Runner.refreshStatus()
                Runner.tryBindService()
                if (Runner.waitService((timeout * 3).toLong())) {
                    Log.i(TAG, "Service is ready")
                    exec()
                } else {
                    Log.i(TAG, "Service is not ready")
                    mHandler.post { stopSelf() }
                }
            } else {
                Log.i(TAG, "Shizuku is not ready")
                mHandler.post { stopSelf() }
            }
        }

        return START_STICKY
    }

    fun exec() {
        val sharedPreferences = App.Companion.instance.getSharedPreferences("startup_script", MODE_PRIVATE)
        val command = sharedPreferences.getString("startup_script_command", "")
        val targetPerm = if (sharedPreferences.getBoolean("startup_script_reduce_perm", false)) sharedPreferences.getString("startup_script_target_perm", null) else null
        if (command.isNullOrEmpty()) {
            Log.i(TAG, "exec: command is empty")
            return
        }
        Log.i(TAG, "exec: $command")
        Runner.service?.exec(
            command,
            targetPerm ?: "",
            "ExecOnBootTask",
            object : IExecResultCallback.Stub() {
                override fun onOutput(outputs: String?) {
                    Log.i(TAG, "onOutput: $outputs")
                }

                override fun onExit(exitValue: Int) {
                    Log.i(TAG, "exit with $exitValue")
                    stopSelf()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
