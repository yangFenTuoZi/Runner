package yangFenTuoZi.runner.plus.tools;

import android.content.pm.IPackageManager;
import android.os.ServiceManager;
import android.system.Os;

import yangFenTuoZi.runner.plus.info.Info;

public class GetAppPath {
    public static void main(String[] args) {
        try {
            IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            String appPath = pm.getApplicationInfo(Info.APPLICATION_ID, 0, Os.getuid() / 100000).sourceDir;
            if (appPath == null || appPath.isEmpty()) {
                System.exit(255);
            } else {
                System.out.println(appPath);
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(255);
        }
    }
}
