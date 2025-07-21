package yangfentuozi.runner.app

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
import yangfentuozi.runner.BuildConfig
import yangfentuozi.runner.server.IService
import yangfentuozi.runner.server.ServerMain


object Runner {
    var service: IService? = null
    var binder: IBinder? = null
    var shizukuPermission: Boolean = false
    var shizukuStatus: Boolean = false
    var shizukuUid: Int = -1
    var shizukuApiVersion: Int = -1
    var shizukuPatchVersion: Int = -1
    var serviceVersion: Int = -1

    private val userServiceArgs: UserServiceArgs = UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            ServerMain::class.java.getName()
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
                scheduleServiceStatusListener(true)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            binder = null
            service = null
            serviceVersion = -1
            scheduleServiceStatusListener(false)
        }
    }

    private val onRequestPermissionResultListener =
        OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            if (requestCode != 7890) return@OnRequestPermissionResultListener
            shizukuPermission = grantResult == PackageManager.PERMISSION_GRANTED
            scheduleShizukuPermissionListener(shizukuPermission)
            shizukuStatus = Shizuku.pingBinder()
            scheduleShizukuStatusListener(shizukuStatus)
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
        scheduleShizukuStatusListener(true)
        tryBindService()
    }

    private val onBinderDeadListener = OnBinderDeadListener {
        shizukuStatus = false
        shizukuPatchVersion = -1
        shizukuApiVersion = -1
        shizukuUid = -1
        serviceVersion = -1
        binder = null
        service = null
        scheduleServiceStatusListener(false)
        scheduleShizukuStatusListener(false)
    }

    fun refreshStatus() {
        shizukuStatus = Shizuku.pingBinder()
        shizukuPermission = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: RuntimeException) {
            App.instance.checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
        }
        scheduleShizukuStatusListener(shizukuStatus)
        scheduleShizukuPermissionListener(shizukuPermission)
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

    fun waitShizuku(timeOut: Long): Boolean {
        try {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeOut) {
                if (Shizuku.pingBinder()) return true
                Thread.sleep(100)
            }
            return false
        } catch (_: InterruptedException) {
            return false
        }
    }

    fun waitService(timeOut: Long): Boolean {
        try {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeOut) {
                if (pingServer()) return true
                Thread.sleep(100)
            }
            return false
        } catch (_: InterruptedException) {
            return false
        }
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

    fun interface ShizukuPermissionListener {
        fun onChange(granted: Boolean)
    }

    fun interface ShizukuStatusListener {
        fun onChange(running: Boolean)
    }

    fun interface ServiceStatusListener {
        fun onChange(running: Boolean)
    }

    private val shizukuPermissionListener: ArrayList<ShizukuPermissionListener> =
        ArrayList<ShizukuPermissionListener>()
    private val shizukuStatusListener: ArrayList<ShizukuStatusListener> =
        ArrayList<ShizukuStatusListener>()
    private val serviceStatusListener: ArrayList<ServiceStatusListener> =
        ArrayList<ServiceStatusListener>()


    private fun scheduleShizukuPermissionListener(granted: Boolean) {
        synchronized(shizukuPermissionListener) {
            for (listener in shizukuPermissionListener) {
                listener.onChange(granted)
            }
        }
    }

    private fun scheduleShizukuStatusListener(running: Boolean) {
        synchronized(shizukuStatusListener) {
            for (listener in shizukuStatusListener) {
                listener.onChange(running)
            }
        }
    }

    private fun scheduleServiceStatusListener(running: Boolean) {
        synchronized(serviceStatusListener) {
            for (listener in serviceStatusListener) {
                listener.onChange(running)
            }
        }
    }

    fun addShizukuPermissionListener(listener: ShizukuPermissionListener) {
        shizukuPermissionListener.add(listener)
    }

    fun removeShizukuPermissionListener(listener: ShizukuPermissionListener) {
        synchronized(shizukuPermissionListener) {
            shizukuPermissionListener.remove(listener)
        }
    }

    fun addShizukuStatusListener(listener: ShizukuStatusListener) {
        shizukuStatusListener.add(listener)
    }

    fun removeShizukuStatusListener(listener: ShizukuStatusListener) {
        synchronized(shizukuStatusListener) {
            shizukuStatusListener.remove(listener)
        }
    }

    fun addServiceStatusListener(listener: ServiceStatusListener) {
        serviceStatusListener.add(listener)
    }

    fun removeServiceStatusListener(listener: ServiceStatusListener) {
        synchronized(serviceStatusListener) {
            serviceStatusListener.remove(listener)
        }
    }
}