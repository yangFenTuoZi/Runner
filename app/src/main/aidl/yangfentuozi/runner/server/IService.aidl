package yangfentuozi.runner.server;

import yangfentuozi.runner.shared.data.TermExtVersion;
import yangfentuozi.runner.shared.data.ProcessInfo;
import yangfentuozi.runner.shared.data.EnvInfo;

import yangfentuozi.runner.server.callback.IExitCallback;

import java.util.List;

interface IService {
    void destroy() = 16777114;
    void exit() = 1;
    int version() = 2;

    void exec(String command, String procName, in IExitCallback callback, in ParcelFileDescriptor stdout) = 100;

    ProcessInfo[] getProcesses() = 400;
    boolean[] sendSignal(in int[] pid, int signal) = 401;

    void backupData(String output, boolean termHome, boolean termUsr) = 500;
    void restoreData(String input) = 501;

    void installTermExt(String termExtZip, in IExitCallback callback, in ParcelFileDescriptor stdout) = 1000;
    void removeTermExt() = 1001;
    TermExtVersion getTermExtVersion() = 1002;

    void syncAllData(in List<EnvInfo> envs) = 600;

    IBinder getShellService() = 30000;
}