package yangFenTuoZi.runner.plus.ui.dialog;

import static yangFenTuoZi.runner.plus.ui.activity.RequestBinderActivity.reply;

import android.app.Activity;
import android.os.IBinder;
import android.text.Html;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import yangFenTuoZi.runner.plus.R;

import java.util.Arrays;

public class RequestPermissionDialog extends MaterialAlertDialogBuilder {

    TextView t;
    MaterialTextView allow, refuse;
    AlertDialog alertDialog;
    IBinder binder;
    String[] packageNames;
    Activity mActivity;

    public RequestPermissionDialog(@NonNull Activity context, IBinder binder, String[] packageNames) {
        super(context);
        if (packageNames == null) return;
        setView(R.layout.dialog_request_permission);
        this.binder = binder;
        this.packageNames = packageNames;
        mActivity = context;
    }

    @NonNull
    @Override
    public AlertDialog create() {
        alertDialog = super.create();
        return alertDialog;
    }

    @Override
    public AlertDialog show() {
        super.show();
        t = alertDialog.findViewById(android.R.id.title);
        if (t != null) {
            t.setText(Html.fromHtml(mActivity.getString(R.string.grant_description, Arrays.toString(packageNames)), 0));
        }
        allow = alertDialog.findViewById(android.R.id.button1);
        refuse = alertDialog.findViewById(android.R.id.button3);
        allow.setOnClickListener(v -> {
            reply(true, binder);
            alertDialog.cancel();
        });
        refuse.setOnClickListener(v -> {
            reply(false, binder);
            alertDialog.cancel();
        });
        return alertDialog;
    }
}