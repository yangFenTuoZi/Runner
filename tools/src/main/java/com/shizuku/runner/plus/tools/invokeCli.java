package com.shizuku.runner.plus.tools;

import dalvik.system.DexClassLoader;

public class invokeCli {
    public static void main(String[] args) {
        String app_package_name = System.getenv("APP_PACKAGE_NAME");
        if (app_package_name == null)
            System.exit(1);

        try {
            String dexPath = null;
            try {
                dexPath = Tools.getAppPath(app_package_name);
            } catch (Throwable e) {
                System.err.println("Unable to get the app path");
                System.err.flush();
                System.exit(1);
            }
            if (dexPath == null || dexPath.isEmpty()) {
                System.err.println("Unable to get the app path");
                System.err.flush();
                System.exit(1);
            }
            DexClassLoader classLoader = new DexClassLoader(dexPath, null, null, ClassLoader.getSystemClassLoader());
            Class<?> cls = classLoader.loadClass("com.shizuku.runner.plus.cli.Main");
            cls.getDeclaredMethod("main", String[].class)
                    .invoke(null, (Object) args);
        } catch (ClassNotFoundException tr) {
            System.err.println("Class not found");
            System.err.flush();
            System.exit(1);
        } catch (Throwable tr) {
            tr.printStackTrace(System.err);
            System.err.flush();
            System.exit(1);
        }
    }
}
