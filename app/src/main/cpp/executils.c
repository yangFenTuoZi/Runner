#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <errno.h>
#include <android/log.h>

#define TAG "ExecUtils"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/**
 * 执行命令并重定向文件描述符
 * @param executable 可执行文件路径
 * @param argv 参数数组（argv[0] 作为进程名）
 * @param envp 环境变量数组
 * @param stdinFd 标准输入 FD（-1 表示不重定向）
 * @param stdoutFd 标准输出 FD（-1 表示不重定向）
 * @param stderrFd 标准错误 FD（-1 表示不重定向）
 * @return 子进程 PID，失败返回 -1
 */
JNIEXPORT jint JNICALL
Java_yangfentuozi_runner_server_util_ExecUtils_exec(
    JNIEnv *env,
    jobject thiz,
    jstring executable,
    jobjectArray argv,
    jobjectArray envp,
    jint stdinFd,
    jint stdoutFd,
    jint stderrFd
) {
    // 转换可执行文件路径
    const char *exec_path = (*env)->GetStringUTFChars(env, executable, NULL);
    if (!exec_path) {
        LOGE("Failed to get executable path");
        return -1;
    }
    char *c_exec_path = strdup(exec_path);
    (*env)->ReleaseStringUTFChars(env, executable, exec_path);
    
    if (!c_exec_path) {
        LOGE("Failed to allocate executable path");
        return -1;
    }
    
    // 转换 argv
    jsize argc = (*env)->GetArrayLength(env, argv);
    char **c_argv = (char **)malloc((argc + 1) * sizeof(char *));
    if (!c_argv) {
        LOGE("Failed to allocate argv");
        free(c_exec_path);
        return -1;
    }
    
    for (int i = 0; i < argc; i++) {
        jstring jstr = (jstring)(*env)->GetObjectArrayElement(env, argv, i);
        const char *str = (*env)->GetStringUTFChars(env, jstr, NULL);
        c_argv[i] = strdup(str);
        (*env)->ReleaseStringUTFChars(env, jstr, str);
        (*env)->DeleteLocalRef(env, jstr);
    }
    c_argv[argc] = NULL;

    // 转换 envp
    jsize envc = (*env)->GetArrayLength(env, envp);
    char **c_envp = (char **)malloc((envc + 1) * sizeof(char *));
    if (!c_envp) {
        LOGE("Failed to allocate envp");
        for (int i = 0; i < argc; i++) free(c_argv[i]);
        free(c_argv);
        free(c_exec_path);
        return -1;
    }
    
    for (int i = 0; i < envc; i++) {
        jstring jstr = (jstring)(*env)->GetObjectArrayElement(env, envp, i);
        const char *str = (*env)->GetStringUTFChars(env, jstr, NULL);
        c_envp[i] = strdup(str);
        (*env)->ReleaseStringUTFChars(env, jstr, str);
        (*env)->DeleteLocalRef(env, jstr);
    }
    c_envp[envc] = NULL;

    // Fork 子进程
    pid_t pid = fork();
    
    if (pid < 0) {
        // Fork 失败
        LOGE("Fork failed: %s", strerror(errno));
        for (int i = 0; i < argc; i++) free(c_argv[i]);
        free(c_argv);
        for (int i = 0; i < envc; i++) free(c_envp[i]);
        free(c_envp);
        free(c_exec_path);
        return -1;
    }
    
    if (pid == 0) {
        // 子进程

        // 创建新会话
        if (setsid() < 0) {
            LOGE("setsid: %s", strerror(errno));
            exit(1);
        }
        
        // 重定向文件描述符
        if (stdinFd >= 0) {
            if (dup2(stdinFd, STDIN_FILENO) < 0) {
                LOGE("dup2 stdin failed: %s", strerror(errno));
                _exit(127);
            }
            close(stdinFd);
        }
        
        if (stdoutFd >= 0) {
            if (dup2(stdoutFd, STDOUT_FILENO) < 0) {
                LOGE("dup2 stdout failed: %s", strerror(errno));
                _exit(127);
            }
            // 如果 stderr 也使用相同的 fd，则合并到 stdout
            if (stderrFd == stdoutFd) {
                if (dup2(STDOUT_FILENO, STDERR_FILENO) < 0) {
                    LOGE("dup2 stderr to stdout failed: %s", strerror(errno));
                    _exit(127);
                }
            }
            close(stdoutFd);
        }
        
        if (stderrFd >= 0 && stderrFd != stdoutFd) {
            if (dup2(stderrFd, STDERR_FILENO) < 0) {
                LOGE("dup2 stderr failed: %s", strerror(errno));
                _exit(127);
            }
            close(stderrFd);
        }

        // 输出进程 id
        dprintf(STDOUT_FILENO, "%d\n", getpid());
        
        // 执行命令（使用独立的可执行文件路径）
        execve(c_exec_path, c_argv, c_envp);
        
        // 如果 execve 返回，说明失败了
        LOGE("execve failed: %s (executable: %s)", strerror(errno), c_exec_path);
        _exit(127);
    }
    
    // 父进程
    LOGD("Created child process with PID: %d", pid);
    
    // 关闭父进程中的 FD（如果传递了的话）
    if (stdinFd >= 0) close(stdinFd);
    if (stdoutFd >= 0) close(stdoutFd);
    // 只有当 stderrFd 不等于 stdoutFd 时才关闭（避免重复关闭）
    if (stderrFd >= 0 && stderrFd != stdoutFd) close(stderrFd);
    
    // 清理内存
    free(c_exec_path);
    for (int i = 0; i < argc; i++) free(c_argv[i]);
    free(c_argv);
    for (int i = 0; i < envc; i++) free(c_envp[i]);
    free(c_envp);
    
    return (jint)pid;
}

/**
 * 等待进程结束
 * @param pid 进程 PID
 * @return 退出码
 */
JNIEXPORT jint JNICALL
Java_yangfentuozi_runner_server_util_ExecUtils_waitpid(
    JNIEnv *env,
    jobject thiz,
    jint pid
) {
    int status;
    pid_t result = waitpid((pid_t)pid, &status, 0);
    
    if (result < 0) {
        LOGE("waitpid failed: %s", strerror(errno));
        return 255;
    }
    
    if (WIFEXITED(status)) {
        int exitCode = WEXITSTATUS(status);
        LOGD("Process %d exited with code %d", pid, exitCode);
        return exitCode;
    } else if (WIFSIGNALED(status)) {
        int signal = WTERMSIG(status);
        LOGD("Process %d terminated by signal %d", pid, signal);
        return 128 + signal;
    }
    
    return 255;
}

