package yangfentuozi.runner.server.callback;

interface IInstallTermExtCallback {
    void onMessage(String message);
    void onExit(boolean isSuccessful);
}