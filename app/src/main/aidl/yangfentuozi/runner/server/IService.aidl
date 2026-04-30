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

    void installTermModule(String modZip, in IExitCallback callback, in ParcelFileDescriptor stdout) = 2000;
    void uninstallTermModule(String moduleId, in IExitCallback callback, in ParcelFileDescriptor stdout, boolean purge) = 2001;
    TermModuleInfo[] getTermModules() = 2002;

    String[] getAvailableShells() = 2100;
    String getCurrentShell() = 2101;
    void setCurrentShell(String shell) = 2102;

    IBinder getShellService() = 30000;
}