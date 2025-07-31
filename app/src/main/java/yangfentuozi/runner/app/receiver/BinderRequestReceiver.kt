package yangfentuozi.runner.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import rikka.rish.Rish
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.server.callback.IRequestBinderCallback
import java.io.FileReader

class BinderRequestReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != "yangfentuozi.runner.intent.action.REQUEST_BINDER") {
            return
        }
        val data = intent.getBundleExtra("data")
        if (data == null) {
            return
        }
        val token = data.getString("token")
        if (token == null) {
            return
        } else {
            try {
                FileReader(Rish.tokenFile).use {
                    if (token != it.readText().trim()) {
                        throw Exception()
                    }
                }
            } catch (_: Exception) {
                return
            }
        }
        val callbackBinder = data.getBinder("binder")
        if (callbackBinder == null) {
            return
        }
        IRequestBinderCallback.Stub.asInterface(callbackBinder).onCallback(Runner.service?.shellService, context?.applicationInfo?.sourceDir)
    }
}