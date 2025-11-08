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
import yangfentuozi.runner.server.ServerMain
import yangfentuozi.runner.shared.data.ProcessInfo

class ProcViewModel(application: Application) : AndroidViewModel(application), HideAllDialogs {
    private val _processes = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val processes: StateFlow<List<ProcessInfo>> = _processes.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _showKillDialog = MutableStateFlow(-1)
    val showKillDialog: StateFlow<Int> = _showKillDialog.asStateFlow()

    init {
        loadProcesses()
    }

    fun loadProcesses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val procs = withContext(Dispatchers.IO) {
                if (Runner.pingServer()) {
                    try {
                        val allProcesses = Runner.service?.processes
                        allProcesses?.filter { it.exe == ServerMain.USR_PATH + "/bin/bash" }
                            ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
            _processes.value = procs
            _isRefreshing.value = false
        }
    }

    fun showKillDialog(pid: Int) {
        _showKillDialog.value = pid
    }

    fun hideKillDialog() {
        _showKillDialog.value = -1
    }

    fun killProcess(
        pid: Int,
        forceKill: Boolean = false,
        killChildren: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            killPIDs(
                pids = if (pid == -1010) {
                    _processes.value.map { it.pid }.toIntArray()
                } else {
                    intArrayOf(pid)
                },
                forceKill = forceKill,
                killChildren = killChildren
            )
            loadProcesses()
        }
    }

    private fun killPIDs(
        pids: IntArray,
        forceKill: Boolean = false,
        killChildren: Boolean = false
    ) {
        if (Runner.pingServer()) {
            try {
                val signal = if (forceKill) 9 else 15
                if (killChildren) {
                    Runner.service?.sendSignal(IntArray(pids.size) { i -> -pids[i] }, signal)
                } else {
                    Runner.service?.sendSignal(pids, signal)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun hideAllDialogs() {
        hideKillDialog()
    }
}

