package yangFenTuoZi.runner.plus.service.callback;

interface IExecResultCallback {
    void onOutput(String outputs);
    void onExit(int exitValue);
}