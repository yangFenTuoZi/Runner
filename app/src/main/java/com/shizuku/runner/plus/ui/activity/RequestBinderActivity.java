package com.shizuku.runner.plus.ui.activity;

import static com.shizuku.runner.plus.ui.activity.MainActivity.sendSomethingToServerBySocket;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.system.Os;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.shizuku.runner.plus.App;
import com.shizuku.runner.plus.cli.IApp;
import com.shizuku.runner.plus.cli.cmdInfo;
import com.shizuku.runner.plus.server.Server;

import java.io.IOException;

public class RequestBinderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || !Server.ACTION_REQUEST_BINDER.equals(intent.getAction())) return;
        int uid = intent.getIntExtra("uid", -1);
        if (uid == -1) return;

        Bundle data = intent.getBundleExtra("data");
        if (data == null) return;
        IBinder binder = data.getBinder("binder");
        if (binder == null) return;

        if (App.binder == null || !App.binder.pingBinder()) {
            new Thread(() -> {
                try {
                    sendSomethingToServerBySocket("sendBinderToApp");
                } catch (IOException ignored) {
                }
            }).start();
        }

        if (uid >= 0 && uid < 10000) {
            reply(true, binder, getApplicationContext());
            finish();
        } else {
            startActivity(new Intent(this, RequestPermissionActivity.class)
                    .setAction(Server.ACTION_REQUEST_BINDER)
                    .putExtra("data", data)
                    .putExtra("uid", uid)
                    .putExtra("packageNames", intent.getStringArrayExtra("packageNames")));
            finish();
        }
    }

    public static void reply(boolean allow, IBinder binder, Context context) {
        Parcel data = Parcel.obtain();
        if (allow) {
            IBinder[] binders = new IBinder[2];
            binders[1] = App.binder;
            binders[0] = createBinder(context);
            try {
                data.writeBinderArray(binders);
                binder.transact(1, data, null, IBinder.FLAG_ONEWAY);
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                data.recycle();
            }
        } else {
            try {
                binder.transact(2, data, null, IBinder.FLAG_ONEWAY);
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                data.recycle();
            }
        }
    }

    private static IBinder createBinder(Context context) {
        return new IApp.Stub() {
            @Override
            public cmdInfo[] getAllCmds() throws RemoteException {
                SharedPreferences sp = context.getSharedPreferences("data", 0);
                if (sp.getString("data", "").isEmpty()) {
                    return new cmdInfo[0];
                } else {
                    String[] s = sp.getString("data", "").split(",");
                    cmdInfo[] cmdInfos = new cmdInfo[s.length];
                    for (int i = 0; i < s.length; i++) {
                        SharedPreferences sharedPreferences = context.getSharedPreferences(s[i], 0);
                        cmdInfos[i] = new cmdInfo();
                        cmdInfos[i].id = Integer.parseInt(s[i]);
                        cmdInfos[i].name = sharedPreferences.getString("name", "");
                        cmdInfos[i].command = sharedPreferences.getString("command", "");
                        cmdInfos[i].keepAlive = sharedPreferences.getBoolean("keep_in_alive", false);
                        cmdInfos[i].useChid = sharedPreferences.getBoolean("chid", false);
                        cmdInfos[i].ids = sharedPreferences.getString("ids", "");
                    }
                    return cmdInfos;
                }
            }

            @Override
            public cmdInfo getCmdByID(int id) throws RemoteException {
                return null;
            }

            @Override
            public void delete(int id) throws RemoteException {

            }

            @Override
            public void edit(cmdInfo cmd_info) throws RemoteException {

            }
        };
    }
}
