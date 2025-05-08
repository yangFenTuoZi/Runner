package yangfentuozi.runner.service.callback;

import android.os.RemoteException;

public class ExecResultCallback {

    private final IExecResultCallback mCallback;

    public ExecResultCallback(IExecResultCallback callback) {
        mCallback = callback;
    }

    public void onOutput(String outputs) {
        try {
            mCallback.onOutput(outputs);
        } catch (RemoteException ignored) {
        }
    }

    public void onExit(int exitValue) {
        try {
            mCallback.onExit(exitValue);
        } catch (RemoteException ignored) {
        }
    }
}
