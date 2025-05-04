package yangFenTuoZi.runner.plus.service;

import yangFenTuoZi.runner.plus.service.data.CommandInfo;
import yangFenTuoZi.runner.plus.service.data.TermExtVersion;

import yangFenTuoZi.runner.plus.service.callback.IExecResultCallback;
import yangFenTuoZi.runner.plus.service.callback.IInstallTermExtCallback;

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
    boolean updateEnv(String fromKey, String fromValue, String toKey, String toValue) = 302;
    String getEnv(String key) = 303;
    Map<String, String> getAllEnv() = 304;

    void installTermExt(String termExtZip, in IInstallTermExtCallback callback) = 1000;
    void removeTermExt() = 1001;
    TermExtVersion getTermExtVersion() = 1002;
}