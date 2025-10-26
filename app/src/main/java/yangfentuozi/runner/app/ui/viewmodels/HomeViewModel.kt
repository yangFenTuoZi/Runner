package yangfentuozi.runner.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.screens.main.HideAllDialogs
import yangfentuozi.runner.shared.data.TermExtVersion

class HomeViewModel(application: Application) : AndroidViewModel(application), HideAllDialogs {
    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: StateFlow<Int> = _refreshTrigger.asStateFlow()

    private val _termExtVersion = MutableStateFlow<TermExtVersion?>(null)
    val termExtVersion: StateFlow<TermExtVersion?> = _termExtVersion.asStateFlow()

    private val _showRemoveTermExtConfirmDialog = MutableStateFlow(false)
    val showRemoveTermExtConfirmDialog: StateFlow<Boolean> = _showRemoveTermExtConfirmDialog.asStateFlow()

    // Shizuku 权限监听器
    private val shizukuPermissionListener = Runner.ShizukuPermissionListener {
        triggerRefresh()
    }

    // Shizuku 状态监听器
    private val shizukuStatusListener = Runner.ShizukuStatusListener {
        triggerRefresh()
    }

    // 服务状态监听器
    private val serviceStatusListener = Runner.ServiceStatusListener {
        triggerRefresh()
        // 服务状态改变时，重新加载终端扩展版本
        loadTermExtVersion()
    }

    init {
        // 初始化时刷新状态
        viewModelScope.launch(Dispatchers.IO) {
            Runner.refreshStatus()
        }

        // 注册监听器
        Runner.addShizukuPermissionListener(shizukuPermissionListener)
        Runner.addShizukuStatusListener(shizukuStatusListener)
        Runner.addServiceStatusListener(serviceStatusListener)

        // 加载终端扩展版本
        loadTermExtVersion()
    }

    fun triggerRefresh() {
        _refreshTrigger.value++
    }

    fun loadTermExtVersion() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val version = Runner.service?.termExtVersion
                _termExtVersion.value = version
            } catch (e: Exception) {
                e.printStackTrace()
                _termExtVersion.value = null
            }
        }
    }

    fun tryBindService() {
        viewModelScope.launch(Dispatchers.IO) {
            Runner.tryBindService()
        }
    }

    fun requestPermission() {
        Runner.requestPermission()
    }

    override fun onCleared() {
        super.onCleared()
        // 移除监听器
        Runner.removeShizukuPermissionListener(shizukuPermissionListener)
        Runner.removeShizukuStatusListener(shizukuStatusListener)
        Runner.removeServiceStatusListener(serviceStatusListener)
    }

    fun removeTermExt() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Runner.service?.removeTermExt()
                loadTermExtVersion()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showRemoveTermExtConfirmDialog() {
        _showRemoveTermExtConfirmDialog.value = true
    }

    fun hideRemoveTermExtConfirmDialog() {
        _showRemoveTermExtConfirmDialog.value = false
    }

    override fun hideAllDialogs() {
        hideRemoveTermExtConfirmDialog()
    }
}

