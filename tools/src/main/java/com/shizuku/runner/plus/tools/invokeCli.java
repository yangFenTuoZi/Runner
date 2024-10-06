package com.shizuku.runner.plus.tools;

public class invokeCli {

    public static void main(String[] args) {
        String app_package_name = System.getenv("APP_PACKAGE_NAME");
        if (app_package_name == null)
            System.exit(1);
    }
}
