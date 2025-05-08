#include <jni.h>
#include <csignal>
#include <dirent.h>
#include <cstring>
#include <fstream>
#include <sstream>
#include <vector>
#include <algorithm>
#include <unistd.h>

// 定义 ProcessInfo 类对应的字段和方法
static jclass processInfoClass;
static jfieldID pidField;
static jfieldID ppidField;
static jfieldID exeField;
static jfieldID argsField;

// 初始化 ProcessInfo 类的字段引用
void initProcessInfoFields(JNIEnv *env) {
    processInfoClass = env->FindClass("yangfentuozi/runner/service/data/ProcessInfo");
    pidField = env->GetFieldID(processInfoClass, "pid", "I");
    ppidField = env->GetFieldID(processInfoClass, "ppid", "I");
    exeField = env->GetFieldID(processInfoClass, "exe", "Ljava/lang/String;");
    argsField = env->GetFieldID(processInfoClass, "args", "[Ljava/lang/String;");
}

// 读取进程的命令行参数
std::vector<std::string> readProcessArgs(int pid) {
    std::vector<std::string> args;
    char filename[256];
    sprintf(filename, "/proc/%d/cmdline", pid);

    std::ifstream cmdlineFile(filename);
    if (cmdlineFile.is_open()) {
        std::string line;
        while (std::getline(cmdlineFile, line, '\0')) {
            if (!line.empty()) {
                args.push_back(line);
            }
        }
        cmdlineFile.close();
    }
    return args;
}

// 读取进程状态获取 PPID
int readProcessPpid(int pid) {
    char filename[256];
    sprintf(filename, "/proc/%d/status", pid);
    std::ifstream statusFile(filename);
    int ppid = 0;

    if (statusFile.is_open()) {
        std::string line;
        while (std::getline(statusFile, line)) {
            if (line.find("PPid:") == 0) {
                sscanf(line.c_str(), "PPid:\t%d", &ppid);
                break;
            }
        }
        statusFile.close();
    }
    return ppid;
}

// 读取进程的可执行文件路径
std::string readProcessExe(int pid) {
    char exePath[256];
    char resolvedPath[PATH_MAX];
    sprintf(exePath, "/proc/%d/exe", pid);

    ssize_t len = readlink(exePath, resolvedPath, sizeof(resolvedPath) - 1);
    if (len != -1) {
        resolvedPath[len] = '\0';
        return {resolvedPath};
    }
    return "";
}

// 获取所有进程
extern "C" JNIEXPORT jobjectArray JNICALL
Java_yangfentuozi_runner_service_jni_ProcessUtils_getProcesses(JNIEnv *env, jobject /* this */) {
    initProcessInfoFields(env);

    std::vector<int> pids;
    DIR* dir = opendir("/proc");
    if (dir == nullptr) {
        return nullptr;
    }

    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr) {
        if (entry->d_type == DT_DIR) {
            char* end;
            long pid = strtol(entry->d_name, &end, 10);
            if (*end == '\0') {
                pids.push_back(pid);
            }
        }
    }
    closedir(dir);

    // 创建 ProcessInfo 数组
    jobjectArray result = env->NewObjectArray(pids.size(), processInfoClass, nullptr);
    if (result == nullptr) {
        return nullptr;
    }

    for (size_t i = 0; i < pids.size(); ++i) {
        int pid = pids[i];

        // 读取进程名和参数
        std::vector<std::string> args = readProcessArgs(pid);
        int ppid = readProcessPpid(pid);
        std::string exePath = readProcessExe(pid);

        // 创建 ProcessInfo 对象
        jobject processInfo = env->AllocObject(processInfoClass);

        // 设置字段值
        env->SetIntField(processInfo, pidField, pid);
        env->SetIntField(processInfo, ppidField, ppid);

        // 设置可执行文件路径
        jstring exe = env->NewStringUTF(exePath.c_str());
        env->SetObjectField(processInfo, exeField, exe);
        env->DeleteLocalRef(exe);

        // 设置参数数组
        jobjectArray argsArray = env->NewObjectArray(args.size(), env->FindClass("java/lang/String"), nullptr);
        for (size_t j = 0; j < args.size(); ++j) {
            jstring arg = env->NewStringUTF(args[j].c_str());
            env->SetObjectArrayElement(argsArray, j, arg);
            env->DeleteLocalRef(arg);
        }
        env->SetObjectField(processInfo, argsField, argsArray);
        env->DeleteLocalRef(argsArray);

        // 将 ProcessInfo 添加到结果数组
        env->SetObjectArrayElement(result, i, processInfo);
        env->DeleteLocalRef(processInfo);
    }

    return result;
}

// 发送信号给进程
extern "C" JNIEXPORT jboolean JNICALL
Java_yangfentuozi_runner_service_jni_ProcessUtils_sendSignal(JNIEnv *env, jobject /* this */, jint pid, jint signal) {
    if (kill(pid, signal) == 0) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}
