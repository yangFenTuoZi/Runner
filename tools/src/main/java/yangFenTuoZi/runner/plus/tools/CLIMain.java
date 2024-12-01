package yangFenTuoZi.runner.plus.tools;

import android.content.pm.IPackageManager;
import android.os.ServiceManager;
import android.system.Os;

import dalvik.system.DexClassLoader;
import yangFenTuoZi.runner.plus.info.Info;

public class CLIMain {
    public static void main(String[] args) {
        try {
            String dexPath = null;
            try {
                IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
                dexPath = pm.getApplicationInfo(Info.APPLICATION_ID, 0, Os.getuid() / 100000).sourceDir;
                if (dexPath == null || dexPath.isEmpty()) throw new Throwable();
            } catch (Throwable e) {
                System.err.println("Unable to get the app path");
                System.err.flush();
                System.exit(255);
            }
            DexClassLoader classLoader = new DexClassLoader(dexPath, null, null, ClassLoader.getSystemClassLoader());
            classLoader.loadClass("yangFenTuoZi.runner.plus.cli.Main").getDeclaredMethod("main", String[].class)
                    .invoke(null, (Object) args);
        } catch (ClassNotFoundException tr) {
            System.err.println("Class not found");
            System.err.flush();
            System.exit(255);
        } catch (Throwable tr) {
            tr.printStackTrace(System.err);
            System.err.flush();
            System.exit(255);
        }
    }
}
