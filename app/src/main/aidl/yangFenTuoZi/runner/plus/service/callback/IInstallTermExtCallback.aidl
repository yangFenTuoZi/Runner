package yangFenTuoZi.runner.plus.service.callback;

interface IInstallTermExtCallback {
    void onMessage(String message);
    void onExit(boolean isSuccessful);
}