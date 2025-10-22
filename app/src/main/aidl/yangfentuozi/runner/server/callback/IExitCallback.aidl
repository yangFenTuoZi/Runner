package yangfentuozi.runner.server.callback;

interface IExitCallback {
    void onExit(int exitValue);
    void errorMessage(String message);
}