package yangFenTuoZi.runner.plus.ui.activity;

import static yangFenTuoZi.runner.plus.ui.activity.MainActivity.sendSomethingToServerBySocket;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;

import androidx.appcompat.app.AppCompatActivity;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.server.Server;

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

        if (App.iService == null || !App.iService.asBinder().pingBinder()) {
            new Thread(() -> {
                try {
                    sendSomethingToServerBySocket("sendBinderToApp");
                } catch (IOException ignored) {
                }
            }).start();
        }

        if (uid >= 0 && uid < 10000) {
            reply(true, binder);
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

    public static void reply(boolean allow, IBinder binder) {
        Parcel data = Parcel.obtain();
        try {
            if (allow)
                data.writeStrongBinder(App.iService.asBinder());
            binder.transact(allow ? 1 : 2, data, null, IBinder.FLAG_ONEWAY);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            data.recycle();
        }
    }
}
