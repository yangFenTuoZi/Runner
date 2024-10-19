package yangFenTuoZi.runner.plus.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

import yangFenTuoZi.runner.plus.server.Server;
import yangFenTuoZi.runner.plus.ui.dialog.RequestPermissionDialog;

public class RequestPermissionActivity extends AppCompatActivity {

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

        new RequestPermissionDialog(this, binder, intent.getStringArrayExtra("packageNames"))
                .setOnDismissListener(dialog -> finish())
                .show();
    }
}
