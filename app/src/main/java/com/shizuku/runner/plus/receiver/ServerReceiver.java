package com.shizuku.runner.plus.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.shizuku.runner.plus.App;
import com.shizuku.runner.plus.server.BinderContainer;
import com.shizuku.runner.plus.server.IService;
import com.shizuku.runner.plus.server.Server;

public class ServerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Server.ACTION_SERVER_RUNNING.equals(intent.getAction())) {
            BinderContainer binderContainer = intent.getParcelableExtra("binder");
            IBinder binder = binderContainer.getBinder();
            if (!binder.pingBinder()) return;
            App.onServerReceive(binder);
        } else if (Server.ACTION_SERVER_STOPPED.equals(intent.getAction())) {
            App.onServerReceive(null);
        }
    }
}
