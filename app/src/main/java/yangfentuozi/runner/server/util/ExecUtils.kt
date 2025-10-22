package yangfentuozi.runner.server.util

import yangfentuozi.runner.server.ServerMain

class ExecUtils : JniUtilsBase() {
    override val jniPath: String
        get() = ServerMain.LIB_EXEC_UTILS

    /**
     * 使用 JNI 创建进程并重定向文件描述符
     * @param executable 可执行文件路径
     * @param argv 参数数组（argv[0] 将作为进程名）
     * @param envp 环境变量数组（格式：KEY=VALUE）
     * @param stdinFd 标准输入文件描述符（-1 表示继承）
     * @param stdoutFd 标准输出文件描述符（-1 表示继承）
     * @param stderrFd 标准错误文件描述符（-1 表示继承）
     * @return 子进程 PID，失败返回 -1
     */
    external fun exec(
        executable: String,
        argv: Array<String>,
        envp: Array<String>,
        stdinFd: Int,
        stdoutFd: Int,
        stderrFd: Int
    ): Int

    /**
     * 等待进程结束并返回退出码
     * @param pid 进程 PID
     * @return 退出码
     */
    external fun waitpid(pid: Int): Int
}

