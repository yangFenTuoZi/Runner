package yangFenTuoZi.runner.plus.service;

import yangFenTuoZi.runner.plus.service.CommandInfo;
import yangFenTuoZi.runner.plus.service.TermExtVersion;

interface IService {
    void destroy() = 16777114;
    void exit() = 1;
    int version() = 2;

    int execX(String cmd, String procName, int port) = 100;
    String exec(String cmd) = 101;

    void openCursor() = 201;
    void closeCursor() = 202;
    int count() = 203;
    CommandInfo query(int id) = 204;
    void delete(int id) = 205;
    void update(in CommandInfo cmdInfo) = 206;
    void insert(in CommandInfo cmdInfo) = 207;
    CommandInfo[] getAll() = 208;

    boolean backupData(String input, boolean includeTerm) = 300;
    boolean restoreData(String output) = 301;

    void installTermExt(String termExtZip) = 1000;
    TermExtVersion getTermExtVersion() = 1001;
}