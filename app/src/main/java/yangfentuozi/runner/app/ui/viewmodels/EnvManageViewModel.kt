package yangfentuozi.runner.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yangfentuozi.runner.app.data.DataRepository
import yangfentuozi.runner.app.ui.screens.main.HideAllDialogs
import yangfentuozi.runner.shared.data.EnvInfo

class EnvManageViewModel(application: Application) : AndroidViewModel(application), HideAllDialogs {
    private val dataRepository = DataRepository.getInstance(application)

    private val _envList = MutableStateFlow<List<EnvInfo>>(emptyList())
    val envList: StateFlow<List<EnvInfo>> = _envList.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _envToEdit = MutableStateFlow<EnvInfo?>(null)
    val envToEdit: StateFlow<EnvInfo?> = _envToEdit.asStateFlow()

    private val _envToDelete = MutableStateFlow<EnvInfo?>(null)
    val envToDelete: StateFlow<EnvInfo?> = _envToDelete.asStateFlow()

    init {
        loadEnvs()
    }

    fun loadEnvs() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val envs = withContext(Dispatchers.IO) {
                dataRepository.getAllEnvs()
            }
            _envList.value = envs
            _isRefreshing.value = false
        }
    }

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    fun showEditDialog(env: EnvInfo) {
        _envToEdit.value = env
    }

    fun hideEditDialog() {
        _envToEdit.value = null
    }

    fun showDeleteDialog(env: EnvInfo) {
        _envToDelete.value = env
    }

    fun hideDeleteDialog() {
        _envToDelete.value = null
    }

    fun addEnv(key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataRepository.addEnv(key, value)
            loadEnvs()
        }
    }

    fun updateEnv(oldKey: String, oldValue: String, newKey: String, newValue: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataRepository.updateEnv(oldKey, oldValue, newKey, newValue)
            loadEnvs()
        }
    }

    fun deleteEnv(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataRepository.deleteEnv(key)
            loadEnvs()
        }
    }

    override fun hideAllDialogs() {
        hideAddDialog()
        hideEditDialog()
        hideDeleteDialog()
    }
}

