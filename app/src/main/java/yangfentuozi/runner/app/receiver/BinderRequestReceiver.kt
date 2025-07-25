package yangfentuozi.runner.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.server.callback.IRequestBinderCallback

class BinderRequestReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != "yangfentuozi.runner.intent.action.REQUEST_BINDER") {
            return
        }
        val callbackBinder = intent.getBundleExtra("data")?.getBinder("binder")
        if (callbackBinder == null) {
            return
        }
        IRequestBinderCallback.Stub.asInterface(callbackBinder).onCallback(Runner.service?.shellService, context?.applicationInfo?.sourceDir)
    }
}