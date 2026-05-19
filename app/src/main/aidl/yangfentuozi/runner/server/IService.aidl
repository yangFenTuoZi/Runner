package yangfentuozi.runner.server;

import yangfentuozi.runner.shared.data.TermModuleInfo;
import yangfentuozi.runner.shared.data.ProcessInfo;

import yangfentuozi.runner.server.callback.IExitCallback;

interface IService {
    void destroy() = 16777114;
    void exit() = 1;
    int version() = 2;

    void exec(String command, in IExitCallback callback, in ParcelFileDescriptor stdout) = 100;

    ProcessInfo[] getProcesses() = 400;
    boolean[] sendSignal(in int[] pid, int signal) = 401;

    void backupData(String output, boolean termHome, boolean termUsr) = 500;
    void restoreData(String input) = 501;

    IBinder getShellService() = 30000;
}