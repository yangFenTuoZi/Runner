package yangFenTuoZi.runner.plus

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnBinderDeadListener
import rikka.shizuku.Shizuku.OnBinderReceivedListener
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.shizuku.Shizuku.UserServiceArgs
import yangFenTuoZi.runner.plus.service.IService
import yangFenTuoZi.runner.plus.service.ServiceImpl


object Runner {
    var service: IService? = null
    var binder: IBinder? = null
    var shizukuPermission: Boolean = false
    var shizukuStatus: Boolean = false
    var shizukuUid: Int = 0
    var shizukuApiVersion: Int = 0
    var shizukuPatchVersion: Int = 0
    var serviceVersion: Int = 0

    private val userServiceArgs: UserServiceArgs = UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            ServiceImpl::class.java.getName()
        )
    )
        .daemon(true)
        .processNameSuffix("runner_server")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
            if (iBinder != null && iBinder.pingBinder()) {
                service = IService.Stub.asInterface(iBinder.also { binder = it })
                serviceVersion = try {
                    service!!.version()
                } catch (_: RemoteException) {
                    -1
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            binder = null
            service = null
            serviceVersion = -1
        }
    }

    private val onRequestPermissionResultListener =
        OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            if (requestCode != 7890) return@OnRequestPermissionResultListener
            shizukuPermission = grantResult == PackageManager.PERMISSION_GRANTED
            shizukuStatus = Shizuku.pingBinder()
            tryBindService()
        }

    private val onBinderReceivedListener = OnBinderReceivedListener {
        shizukuStatus = true
        shizukuUid = Shizuku.getUid()
        shizukuApiVersion = Shizuku.getVersion()
        try {
            val serverPatchVersionField = Shizuku::class.java.getDeclaredField("serverPatchVersion")
            serverPatchVersionField.isAccessible = true
            shizukuPatchVersion = serverPatchVersionField.getInt(null)
        } catch (_: NoSuchFieldException) {
            shizukuPatchVersion = 0
        } catch (_: IllegalAccessException) {
            shizukuPatchVersion = 0
        }
        if (shizukuPatchVersion < 0) shizukuPatchVersion = 0
        tryBindService()
    }

    private val onBinderDeadListener = OnBinderDeadListener {
        shizukuStatus = false
        shizukuPatchVersion = -1
        shizukuApiVersion = shizukuPatchVersion
        shizukuUid = shizukuApiVersion
        serviceVersion = shizukuUid
        binder = null
        service = null
    }

    fun refreshStatus() {
        shizukuStatus = Shizuku.pingBinder()
        shizukuPermission = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: RuntimeException) {
            App.instance?.checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
        }
        tryBindService()
    }

    fun tryBindService() {
        if (shizukuStatus && shizukuPermission && !pingServer()) Shizuku.bindUserService(
            userServiceArgs, serviceConnection
        )
    }

    fun tryUnbindService(remove: Boolean) {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, remove)
    }

    fun requestPermission() {
        Shizuku.requestPermission(7890)
    }

    fun pingServer(): Boolean {
        return binder != null && binder!!.pingBinder()
    }

    fun init() {
        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener)
        Shizuku.addBinderReceivedListenerSticky(onBinderReceivedListener)
        Shizuku.addBinderDeadListener(onBinderDeadListener)
    }

    fun remove() {
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener)
        Shizuku.removeBinderReceivedListener(onBinderReceivedListener)
        Shizuku.removeBinderDeadListener(onBinderDeadListener)
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, false)
    }
}