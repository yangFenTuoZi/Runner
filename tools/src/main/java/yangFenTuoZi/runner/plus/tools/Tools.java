package yangFenTuoZi.runner.plus.tools;

import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;

public class Tools {
    private static IPackageManager pm;

    public static IPackageManager getPackageManager() {
        return IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    }

    public static IActivityManager getActivityManager() {
        return IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
    }

    public static String getAppPath(String packageName) throws RemoteException {
        if (pm == null)
            pm = getPackageManager();
        long flags = 0;
        ApplicationInfo applicationInfo;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            applicationInfo = pm.getApplicationInfo(packageName, flags, 0);
        } else {
            applicationInfo = pm.getApplicationInfo(packageName, 0, 0);
        }
        return applicationInfo.sourceDir;
    }
}
