package yangfentuozi.runner.service.callback;

interface IExecResultCallback {
    void onOutput(String outputs);
    void onExit(int exitValue);
}