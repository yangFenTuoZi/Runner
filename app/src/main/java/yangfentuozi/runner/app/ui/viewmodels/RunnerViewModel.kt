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
import yangfentuozi.runner.shared.data.CommandInfo

class RunnerViewModel(application: Application) : AndroidViewModel(application) {
    private val dataRepository = DataRepository.getInstance(application)

    private val _commands = MutableStateFlow<List<CommandInfo>>(emptyList())
    val commands: StateFlow<List<CommandInfo>> = _commands.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadCommands()
    }

    fun loadCommands() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val cmds = withContext(Dispatchers.IO) {
                dataRepository.getAllCommands()
            }
            _commands.value = cmds
            _isRefreshing.value = false
        }
    }

    fun addCommand(command: CommandInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            dataRepository.addCommand(command)
            loadCommands()
        }
    }

    fun updateCommand(command: CommandInfo, position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dataRepository.updateCommand(command, position)
            loadCommands()
        }
    }

    fun deleteCommand(position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dataRepository.deleteCommand(position)
            loadCommands()
        }
    }
}

