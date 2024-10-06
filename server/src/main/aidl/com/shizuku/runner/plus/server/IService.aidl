package com.shizuku.runner.plus.server;

interface IService {
    int getuid();

    int execX(String cmd, String name,int port);
    String exec(String cmd);
}