package yangFenTuoZi.runner.plus.server;
import yangFenTuoZi.runner.plus.cli.CmdInfo;

interface IService {
    int execX(String cmd, String name,int port);
    String exec(String cmd);

    CmdInfo[] getAllCmds();
    CmdInfo getCmdByID(int id);
    void delete(int id);
    void edit(in CmdInfo cmdInfo);
}