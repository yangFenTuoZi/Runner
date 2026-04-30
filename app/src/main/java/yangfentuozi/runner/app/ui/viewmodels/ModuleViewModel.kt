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
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.screens.main.HideAllDialogs
import yangfentuozi.runner.shared.data.TermModuleInfo

class ModuleViewModel(application: Application) : AndroidViewModel(application), HideAllDialogs {
    private val _modules = MutableStateFlow<List<TermModuleInfo>>(emptyList())
    val modules: StateFlow<List<TermModuleInfo>> = _modules.asStateFlow()

    private val _showUninstallDialog = MutableStateFlow<String?>(null)
    val showUninstallDialog: StateFlow<String?> = _showUninstallDialog.asStateFlow()

    init {
        loadModules()
    }

    fun loadModules() {
        viewModelScope.launch {
            _modules.value = withContext(Dispatchers.IO) {
                if (Runner.pingServer()) {
                    try {
                        (Runner.service?.termModules ?: emptyArray<TermModuleInfo>()).asList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
        }
    }

    fun showUninstallDialog(moduleId: String?) {
        _showUninstallDialog.value = moduleId
    }

    fun hideUninstallDialog() {
        _showUninstallDialog.value = null
    }

    override fun hideAllDialogs() {
        hideUninstallDialog()
    }
}

