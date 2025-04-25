package yangFenTuoZi.runner.plus.service;

import yangFenTuoZi.runner.plus.service.data.CommandInfo;
import yangFenTuoZi.runner.plus.service.TermExtVersion;

interface IService {
    void destroy() = 16777114;
    void exit() = 1;
    int version() = 2;

    int execX(String cmd, String procName, int port) = 100;
    String exec(String cmd) = 101;

    int size() = 200;
    CommandInfo read(int position) = 201;
    CommandInfo[] readAll() = 202;
    void delete(int position) = 203;
    void edit(in CommandInfo cmdInfo, int position) = 204;
    void insert(in CommandInfo cmdInfo) = 205;
    void move(int position, int afterPosition) = 206;

    boolean backupData(String input, boolean includeTerm) = 300;
    boolean restoreData(String output) = 301;

    void installTermExt(String termExtZip) = 1000;
    TermExtVersion getTermExtVersion() = 1001;
}