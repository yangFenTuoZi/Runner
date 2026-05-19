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
import yangfentuozi.runner.app.Service
import yangfentuozi.runner.server.Server
import yangfentuozi.runner.shared.data.ProcessInfo

class ProcViewModel(application: Application) : AndroidViewModel(application) {
    private val _processes = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val processes: StateFlow<List<ProcessInfo>> = _processes.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadProcesses()
    }

    fun loadProcesses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val procs = withContext(Dispatchers.IO) {
                if (Service.pingService()) {
                    try {
                        val allProcesses = Service.service?.processes
                        allProcesses?.filter { it.exe == Server.USR_PATH + "/bin/bash" }
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
        if (Service.pingService()) {
            try {
                val signal = if (forceKill) 9 else 15
                if (killChildren) {
                    Service.service?.sendSignal(IntArray(pids.size) { i -> -pids[i] }, signal)
                } else {
                    Service.service?.sendSignal(pids, signal)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

