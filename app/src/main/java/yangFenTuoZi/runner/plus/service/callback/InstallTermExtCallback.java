package yangFenTuoZi.runner.plus.service.callback;

public class InstallTermExtCallback {
    private final IInstallTermExtCallback mCallback;

    public InstallTermExtCallback(IInstallTermExtCallback callback) {
        mCallback = callback;
    }

    public void onMessage(String message) {
        try {
            mCallback.onMessage(message);
        } catch (Throwable ignored) {
        }
    }

    public void onExit(boolean isSuccessful) {
        try {
            mCallback.onExit(isSuccessful);
        } catch (Throwable ignored) {
        }
    }
}
