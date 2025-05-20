#include <jni.h>
#include <signal.h>
#include <dirent.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

// 定义 ProcessInfo 类对应的字段和方法
static jclass processInfoClass;
static jfieldID pidField;
static jfieldID ppidField;
static jfieldID exeField;
static jfieldID argsField;


// 初始化 ProcessInfo 类的字段引用
void initProcessInfoFields(JNIEnv *env) {
    processInfoClass = (*env)->FindClass(env, "yangfentuozi/runner/service/data/ProcessInfo");
    pidField = (*env)->GetFieldID(env, processInfoClass, "pid", "I");
    ppidField = (*env)->GetFieldID(env, processInfoClass, "ppid", "I");
    exeField = (*env)->GetFieldID(env, processInfoClass, "exe", "Ljava/lang/String;");
    argsField = (*env)->GetFieldID(env, processInfoClass, "args", "[Ljava/lang/String;");
}

// 读取进程的命令行参数，适配16kb分页
int readProcessArgs(int pid, char ***args_out) {
    char filename[256];
    snprintf(filename, sizeof(filename), "/proc/%d/cmdline", pid);
    FILE *file = fopen(filename, "rb");
    if (!file) return 0;
    char *buffer = (char*)malloc(getpagesize());
    size_t len = fread(buffer, 1, getpagesize(), file);
    fclose(file);
    int argc = 0;
    for (size_t i = 0; i < len; ) {
        size_t l = strlen(buffer + i);
        if (l == 0) break;
        argc++;
        i += l + 1;
    }
    char **args = (char**)malloc(argc * sizeof(char*));
    size_t idx = 0;
    for (size_t i = 0; i < len && idx < argc; ) {
        size_t l = strlen(buffer + i);
        if (l == 0) break;
        args[idx] = strdup(buffer + i);
        idx++;
        i += l + 1;
    }
    free(buffer);
    *args_out = args;
    return argc;
}

// 读取进程状态获取 PPID
int readProcessPpid(int pid) {
    char filename[256];
    snprintf(filename, sizeof(filename), "/proc/%d/status", pid);
    FILE *file = fopen(filename, "r");
    if (!file) return 0;
    char line[256];
    int ppid = 0;
    while (fgets(line, sizeof(line), file)) {
        if (strncmp(line, "PPid:", 5) == 0) {
            sscanf(line, "PPid:\t%d", &ppid);
            break;
        }
    }
    fclose(file);
    return ppid;
}

// 读取进程的可执行文件路径
int readProcessExe(int pid, char *exePath, size_t exePathLen) {
    char linkPath[256];
    snprintf(linkPath, sizeof(linkPath), "/proc/%d/exe", pid);
    ssize_t len = readlink(linkPath, exePath, exePathLen - 1);
    if (len != -1) {
        exePath[len] = '\0';
        return 1;
    }
    exePath[0] = '\0';
    return 0;
}

// 获取所有进程
JNIEXPORT jobjectArray JNICALL
Java_yangfentuozi_runner_service_util_ProcessUtils_getProcesses(JNIEnv *env, jobject thiz) {
    initProcessInfoFields(env);
    DIR* dir = opendir("/proc");
    if (!dir) return NULL;
    int pids[4096];
    int count = 0;
    struct dirent* entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_type == DT_DIR) {
            char* end;
            long pid = strtol(entry->d_name, &end, 10);
            if (*end == '\0' && count < 4096) {
                pids[count++] = (int)pid;
            }
        }
    }
    closedir(dir);
    jobjectArray result = (*env)->NewObjectArray(env, count, processInfoClass, NULL);
    if (!result) return NULL;
    for (int i = 0; i < count; ++i) {
        int pid = pids[i];
        char exePath[PATH_MAX];
        readProcessExe(pid, exePath, sizeof(exePath));
        int ppid = readProcessPpid(pid);
        char **args = NULL;
        int argc = readProcessArgs(pid, &args);
        jobject processInfo = (*env)->AllocObject(env, processInfoClass);
        (*env)->SetIntField(env, processInfo, pidField, pid);
        (*env)->SetIntField(env, processInfo, ppidField, ppid);
        jstring exe = (*env)->NewStringUTF(env, exePath);
        (*env)->SetObjectField(env, processInfo, exeField, exe);
        (*env)->DeleteLocalRef(env, exe);
        jclass strClass = (*env)->FindClass(env, "java/lang/String");
        jobjectArray argsArray = (*env)->NewObjectArray(env, argc, strClass, NULL);
        for (int j = 0; j < argc; ++j) {
            jstring arg = (*env)->NewStringUTF(env, args[j]);
            (*env)->SetObjectArrayElement(env, argsArray, j, arg);
            (*env)->DeleteLocalRef(env, arg);
            free(args[j]);
        }
        free(args);
        (*env)->SetObjectField(env, processInfo, argsField, argsArray);
        (*env)->DeleteLocalRef(env, argsArray);
        (*env)->SetObjectArrayElement(env, result, i, processInfo);
        (*env)->DeleteLocalRef(env, processInfo);
    }
    return result;
}

// 发送信号给进程
JNIEXPORT jboolean JNICALL
Java_yangfentuozi_runner_service_util_ProcessUtils_sendSignal(JNIEnv *env, jobject thiz, jint pid, jint signal) {
    if (kill(pid, signal) == 0) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

