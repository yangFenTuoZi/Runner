package yangfentuozi.runner.server.util

import yangfentuozi.runner.server.Server
import yangfentuozi.runner.shared.data.ProcessInfo

class ProcessUtils : JniUtilsBase() {
    external fun sendSignal(pid: Int, signal: Int): Boolean

    external fun getProcesses(): Array<ProcessInfo>?

    override val jniPath: String
        get() = Server.LIB_PROCESS_UTILS
}