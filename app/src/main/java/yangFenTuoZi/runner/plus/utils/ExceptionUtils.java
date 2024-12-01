package yangFenTuoZi.runner.plus.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.server.Logger;

public class ExceptionUtils {

    public static void throwableToDialog(Activity context, Throwable tr) {
        throwableToDialog(context, getStackTraceString(tr));
    }

    public static void throwableToDialog(Activity context, String errorMsg) {
        if (Looper.getMainLooper() == Looper.myLooper())
            createDialog(context, errorMsg);
        else
            context.runOnUiThread(() -> createDialog(context, errorMsg));
    }

    private static void createDialog(Context context, String errorMsg) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.error)
                .setMessage(errorMsg)
                .show();
    }

    public static String getStackTraceString(Throwable tr) {
        return Logger.getStackTraceString(tr);
    }
}
