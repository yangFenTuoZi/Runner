package yangfentuozi.runner.service.util

import yangfentuozi.runner.service.ServiceImpl
import yangfentuozi.runner.service.data.ProcessInfo

class ProcessUtils : JniUtilsBase() {
    external fun sendSignal(pid: Int, signal: Int): Boolean

    external fun getProcesses(): Array<ProcessInfo>?

    override val jniPath: String
        get() = ServiceImpl.JNI_PROCESS_UTILS
}