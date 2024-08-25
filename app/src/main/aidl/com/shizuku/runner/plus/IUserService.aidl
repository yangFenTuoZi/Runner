package com.shizuku.runner.plus;

interface IUserService {
    void destroy() = 16777114;
    void exit() = 1;

    void releaseFile(String packageName, String libraryPath, String apkPath) = 2;

    int execX(String cmd, String name, String packageName, String pipe, int port) = 3;
    String exec(String cmd, String packageName) = 4;

    void deleteFreePIPE(String packageName) = 5;
}