package yangfentuozi.runner.server;

interface IRishService {
    void createHost(
            in String[] args, in String[] env, in String dir,
            byte tty,
            in ParcelFileDescriptor stdin, in ParcelFileDescriptor stdout, in ParcelFileDescriptor stderr) = 0;

    void setWindowSize(long size) = 1;

    int getExitCode() = 2;
}