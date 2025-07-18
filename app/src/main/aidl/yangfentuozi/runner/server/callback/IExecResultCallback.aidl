package yangfentuozi.runner.server.callback;

interface IExecResultCallback {
    void onOutput(String outputs);
    void onExit(int exitValue);
}