package com.shizuku.runner.plus.server;

interface IService {
    void destroy() = 16777114;
    void exit() = 1;

    int getuid() = 2;

    int execX(String cmd, String name, String pipe, int port) = 3;
    String exec(String cmd) = 4;

    void deleteFreePIPE() = 5;
}