package yangFenTuoZi.runner.plus.ui.activity;

import android.content.Intent;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.DialogRequestPermissionBinding;
import yangFenTuoZi.runner.plus.server.Server;

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

        File file = new File(getFilesDir(), "apps.json");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            InputStream in = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            bos.close();
            in.close();
        } catch (Exception ignored) {
        }

        JSONArray allow_apps;
        try {
            allow_apps = new JSONArray(bos.toString());
        } catch (JSONException e) {
            allow_apps = new JSONArray();
        }
        if (uid == 0) {
            reply(true, binder, binder1);
            finish();
        } else {
            boolean allow = false;
            
            for (int i = 0; i < allow_apps.length(); i++) {
                try {
                    JSONObject packageInfo = allow_apps.getJSONObject(i);
                    if (packageInfo.getInt("uid") == uid || packageInfo.getBoolean("allow"))
                        allow = true;
                } catch (JSONException ignored) {
                }
            }
            if (allow) {
                reply(true, binder, binder1);
                finish();
            }

            String[] packageNames = intent.getStringArrayExtra("packageNames");
            DialogRequestPermissionBinding binding = DialogRequestPermissionBinding.inflate(LayoutInflater.from(this));
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                    .setView(binding.getRoot())
                    .setOnDismissListener(dialog -> finish())
                    .create();

            TextView t = binding.title;
            t.setText(Html.fromHtml(getString(R.string.grant_description, Arrays.toString(packageNames)), 0));
            JSONArray finalAllow_apps = allow_apps;
            binding.button1.setOnClickListener(v -> {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("uid", uid);
                    jsonObject.put("allow", true);
                } catch (JSONException ignored) {
                }

                finalAllow_apps.put(jsonObject);
                try {
                    OutputStream out = new FileOutputStream(file);
                    out.write(finalAllow_apps.toString().getBytes());
                    out.close();
                } catch (IOException ignored) {
                }
                reply(true, binder, binder1);
                alertDialog.cancel();
            });
            binding.button3.setOnClickListener(v -> {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("uid", uid);
                    jsonObject.put("allow", false);
                } catch (JSONException ignored) {
                }

                finalAllow_apps.put(jsonObject);
                try {
                    OutputStream out = new FileOutputStream(file);
                    out.write(finalAllow_apps.toString().getBytes());
                    out.close();
                } catch (IOException ignored) {
                }
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
            Log.d(getClass().getSimpleName(), Log.getStackTraceString(e));
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
        while (!App.pingServer() && b.get()) ;
        return App.pingServer() ? App.iService.asBinder() : null;
    }
}
