package com.shizuku.runner.plus.receiver;

import com.shizuku.runner.plus.server.IService;

public interface OnServiceConnectListener {
    default void onServiceConnect(IService iService) {}
}
