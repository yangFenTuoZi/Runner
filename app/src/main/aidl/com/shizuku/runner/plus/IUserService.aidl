package com.shizuku.runner.plus;

interface IUserService {
    void destroy() = 16777114;
    void exit() = 1;

    void releaseFile(String packageName, String libraryPath, String apkPath) = 2;

    int execX(String cmd, String packageName, String pipe, int port) = 3;
    void stopExec(boolean keep_in_alive, int pid, String pipe) = 4;
    String exec(String cmd, String packageName) = 5;
}