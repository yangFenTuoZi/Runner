package yangFenTuoZi.runner.plus.receiver;

import yangFenTuoZi.runner.plus.server.IService;

public interface OnServiceConnectListener {
    default void onServiceConnect(IService iService) {}
}