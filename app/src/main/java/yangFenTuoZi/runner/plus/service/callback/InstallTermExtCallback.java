package yangFenTuoZi.runner.plus.service.callback;

import android.os.RemoteException;

public class InstallTermExtCallback {
    private final IInstallTermExtCallback mCallback;

    public InstallTermExtCallback(IInstallTermExtCallback callback) {
        mCallback = callback;
    }

    public void onMessage(String message) {
        try {
            mCallback.onMessage(message);
        } catch (RemoteException ignored) {
        }
    }

    public void onExit(boolean isSuccessful) {
        try {
            mCallback.onExit(isSuccessful);
        } catch (RemoteException ignored) {
        }
    }
}
