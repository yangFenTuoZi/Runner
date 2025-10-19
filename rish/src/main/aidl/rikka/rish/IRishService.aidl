package rikka.rish;

interface IRishService {
    int createHost(
            in String[] args, in String[] env, in String dir,
            byte tty,
            in ParcelFileDescriptor stdin, in ParcelFileDescriptor stdout, in ParcelFileDescriptor stderr) = 0;

    void setWindowSize(int hostPid, long size) = 1;

    int getExitCode(int hostPid) = 2;

    int[] getAllHost() = 3;

    void releaseHost(int hostPid) = 4;
}