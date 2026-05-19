package yangfentuozi.runner.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import yangfentuozi.runner.app.Service

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: StateFlow<Int> = _refreshTrigger.asStateFlow()

    // Shizuku 权限监听器
    private val shizukuPermissionListener = Service.ShizukuPermissionListener {
        triggerRefresh()
    }

    // Shizuku 状态监听器
    private val shizukuStatusListener = Service.ShizukuStatusListener {
        triggerRefresh()
    }

    // 服务状态监听器
    private val serviceStatusListener = Service.ServiceStatusListener {
        triggerRefresh()
    }

    init {
        // 初始化时刷新状态
        viewModelScope.launch(Dispatchers.IO) {
            Service.refreshStatus()
        }

        // 注册监听器
        Service.addShizukuPermissionListener(shizukuPermissionListener)
        Service.addShizukuStatusListener(shizukuStatusListener)
        Service.addServiceStatusListener(serviceStatusListener)
    }

    fun triggerRefresh() {
        _refreshTrigger.value++
    }

    fun tryBindService() {
        viewModelScope.launch(Dispatchers.IO) {
            Service.tryBindService()
        }
    }

    fun requestPermission() {
        Service.requestPermission()
    }

    override fun onCleared() {
        super.onCleared()
        // 移除监听器
        Service.removeShizukuPermissionListener(shizukuPermissionListener)
        Service.removeShizukuStatusListener(shizukuStatusListener)
        Service.removeServiceStatusListener(serviceStatusListener)
    }
}

