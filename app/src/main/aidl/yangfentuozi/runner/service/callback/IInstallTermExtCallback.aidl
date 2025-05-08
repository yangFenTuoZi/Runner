package yangfentuozi.runner.service.callback;

interface IInstallTermExtCallback {
    void onMessage(String message);
    void onExit(boolean isSuccessful);
}