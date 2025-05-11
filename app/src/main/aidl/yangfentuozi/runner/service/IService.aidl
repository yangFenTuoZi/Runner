package yangfentuozi.runner.service;

import yangfentuozi.runner.service.data.CommandInfo;
import yangfentuozi.runner.service.data.TermExtVersion;
import yangfentuozi.runner.service.data.ProcessInfo;
import yangfentuozi.runner.service.data.EnvInfo;

import yangfentuozi.runner.service.callback.IExecResultCallback;
import yangfentuozi.runner.service.callback.IInstallTermExtCallback;

interface IService {
    void destroy() = 16777114;
    void exit() = 1;
    int version() = 2;

    void exec(String command, String ids, String procName, in IExecResultCallback callback) = 100;

    int size() = 200;
    CommandInfo read(int position) = 201;
    CommandInfo[] readAll() = 202;
    void delete(int position) = 203;
    void edit(in CommandInfo cmdInfo, int position) = 204;
    void insert(in CommandInfo cmdInfo) = 205;
    void move(int position, int afterPosition) = 206;
    void insertInto(in CommandInfo cmdInfo, int position) = 207;

//    boolean backupData(String input, boolean includeTerm) = 300;
//    boolean restoreData(String output) = 301;

    void deleteEnv(String key) = 300;
    boolean insertEnv(String key, String value) = 301;
    boolean updateEnv(in EnvInfo from, in EnvInfo to) = 302;
    String getEnv(String key) = 303;
    EnvInfo[] getAllEnv() = 304;

    ProcessInfo[] getProcesses() = 400;
    boolean[] sendSignal(in int[] pid, int signal) = 401;

    void installTermExt(String termExtZip, in IInstallTermExtCallback callback) = 1000;
    void removeTermExt() = 1001;
    TermExtVersion getTermExtVersion() = 1002;
}