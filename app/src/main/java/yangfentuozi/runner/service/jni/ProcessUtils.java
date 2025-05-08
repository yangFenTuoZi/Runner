package yangfentuozi.runner.service.jni;

import android.annotation.SuppressLint;

import yangfentuozi.runner.service.ServiceImpl;
import yangfentuozi.runner.service.data.ProcessInfo;

@SuppressLint("UnsafeDynamicallyLoadedCode")
public class ProcessUtils extends JniUtilsBase {

    public native boolean sendSignal(int pid, int signal);

    public native ProcessInfo[] getProcesses();

    @Override
    public String getJniPath() {
        return ServiceImpl.JNI_PROCESS_UTILS;
    }
}