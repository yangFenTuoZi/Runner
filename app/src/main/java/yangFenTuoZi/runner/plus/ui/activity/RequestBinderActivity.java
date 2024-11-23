package yangFenTuoZi.runner.plus.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.DialogRequestPermissionBinding;
import yangFenTuoZi.runner.plus.server.Logger;
import yangFenTuoZi.runner.plus.server.Server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestBinderActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || !Server.ACTION_REQUEST_BINDER.equals(intent.getAction())) return;
        int uid = intent.getIntExtra("uid", -1);
        if (uid == -1) return;

        Bundle data = intent.getBundleExtra("data");
        if (data == null) finish();
        IBinder binder = data != null ? data.getBinder("binder") : null;
        if (binder == null) finish();

        IBinder binder1 = waitForBinder();
        if (binder1 == null)
            finish();

        SharedPreferences sharedPreferences = getSharedPreferences("data", 0);
        Set<String> allow_apps = new HashSet<>(sharedPreferences.getStringSet("allow_apps", new HashSet<>()));
        if (uid == 0 || allow_apps.contains(String.valueOf(uid))) {
            reply(true, binder, binder1);
            finish();
        } else {
            String[] packageNames = intent.getStringArrayExtra("packageNames");
            DialogRequestPermissionBinding binding = DialogRequestPermissionBinding.inflate(LayoutInflater.from(this));
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                    .setView(binding.getRoot())
                    .setOnDismissListener(dialog -> finish())
                    .create();

            TextView t = binding.title;
            t.setText(Html.fromHtml(getString(R.string.grant_description, Arrays.toString(packageNames)), 0));
            binding.button1.setOnClickListener(v -> {
                allow_apps.add(String.valueOf(uid));
                sharedPreferences.edit()
                        .putStringSet("allow_apps", allow_apps)
                        .apply();
                reply(true, binder, binder1);
                alertDialog.cancel();
            });
            binding.button3.setOnClickListener(v -> {
                reply(false, binder, binder1);
                alertDialog.cancel();
            });
            alertDialog.show();
        }
    }

    public void reply(boolean allow, IBinder binder, IBinder binder1) {
        Parcel data = Parcel.obtain();
        try {
            if (allow)
                data.writeStrongBinder(binder1);
            binder.transact(allow ? 1 : 2, data, null, IBinder.FLAG_ONEWAY);
        } catch (Throwable e) {
            Log.d(getClass().getSimpleName(), Logger.getStackTraceString(e));
        } finally {
            data.recycle();
        }
    }

    public IBinder waitForBinder() {
        if (App.pingServer())
            return App.iService.asBinder();
        AtomicBoolean b = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            b.set(true);
        }).start();
        while (!App.pingServer() && b.get());
        return App.pingServer() ? App.iService.asBinder() : null;
    }
}
