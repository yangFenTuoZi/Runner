package yangfentuozi.runner.server.callback

import android.os.RemoteException

class ExecResultCallback(private val mCallback: IExecResultCallback?) {
    fun onOutput(outputs: String?) {
        try {
            mCallback?.onOutput(outputs)
        } catch (_: RemoteException) {
        }
    }

    fun onExit(exitValue: Int) {
        try {
            mCallback?.onExit(exitValue)
        } catch (_: RemoteException) {
        }
    }
}
