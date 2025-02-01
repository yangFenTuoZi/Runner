package yangFenTuoZi.runner.plus.server;
import yangFenTuoZi.runner.plus.cli.CmdInfo;
import android.os.IBinder;

interface IService {
    String[] version();
    int execX(String cmd, String name,int port);
    String exec(String cmd);

    void openCursor();
    void closeCursor();
    int count();
    CmdInfo query(int id);
    void delete(int id);
    void update(in CmdInfo cmdInfo);
    void insert(in CmdInfo cmdInfo);
    CmdInfo[] getAll();

    String backupData(int port);
    boolean restoreData(int port, String sha256);
}