package yangfentuozi.runner.service.callback

import android.os.RemoteException

class InstallTermExtCallback(private val mCallback: IInstallTermExtCallback?) {
    fun onMessage(message: String?) {
        try {
            mCallback?.onMessage(message)
        } catch (_: RemoteException) {
        }
    }

    fun onExit(isSuccessful: Boolean) {
        try {
            mCallback?.onExit(isSuccessful)
        } catch (_: RemoteException) {
        }
    }
}