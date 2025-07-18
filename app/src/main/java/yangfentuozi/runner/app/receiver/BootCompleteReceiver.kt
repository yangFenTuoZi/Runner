package yangfentuozi.runner.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.service.ExecOnBootService


class BootCompleteReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "BootCompleteReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED != intent.action && Intent.ACTION_BOOT_COMPLETED != intent.action) {
            return
        }

        if (!App.Companion.preferences.getBoolean("auto_start_exec", false)) return
        if (Process.myUid() / 100000 > 0) return

        Log.i(TAG, "receive $intent")

        if (ExecOnBootService.Companion.isRunning) {
            Log.i(TAG, "ExecOnBootService is already running")
            return
        }
        Log.i(TAG, "start ExecOnBootService")
        context.startService(Intent(context, ExecOnBootService::class.java))
    }
}
