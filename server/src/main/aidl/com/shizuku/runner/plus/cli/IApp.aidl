package com.shizuku.runner.plus.cli;

import com.shizuku.runner.plus.cli.cmdInfo;

interface IApp {
    cmdInfo[] getAllCmds();
    cmdInfo getCmdByID(int id);
    void delete(int id);
    void edit(in cmdInfo cmd_info);

}