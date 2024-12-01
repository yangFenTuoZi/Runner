package yangFenTuoZi.runner.plus.ui.dialog;

import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import yangFenTuoZi.runner.plus.ui.activity.BaseActivity;

public class BaseDialogBuilder extends MaterialAlertDialogBuilder {
    private final BaseActivity mBaseActivity;
    private AlertDialog mAlertDialog;
    private DialogInterface.OnDismissListener mOnDismissListener;

    public static class DialogShowException extends Exception {
    }

    public BaseDialogBuilder(@NonNull BaseActivity context) throws DialogShowException {
        super(context);
        mBaseActivity = context;
        if (mBaseActivity.isDialogShow) throw new DialogShowException();
        mBaseActivity.isDialogShow = true;
        super.setOnDismissListener(dialogInterface -> {
            mBaseActivity.isDialogShow = false;
            if (mOnDismissListener != null)
                mOnDismissListener.onDismiss(dialogInterface);
        });
    }

    public AlertDialog getAlertDialog() {
        return mAlertDialog;
    }

    @NonNull
    @Override
    public AlertDialog create() {
        return mAlertDialog = super.create();
    }

    @NonNull
    @Override
    public MaterialAlertDialogBuilder setOnDismissListener(@Nullable DialogInterface.OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
        return this;
    }

    public void runOnUiThread(Runnable action) {
        mBaseActivity.runOnUiThread(action);
    }

    @NonNull
    public final String getString(@StringRes int resId, Object... formatArgs) {
        return mBaseActivity.getString(resId, formatArgs);
    }

    @NonNull
    public final String getString(@StringRes int resId) {
        return mBaseActivity.getString(resId);
    }
}
