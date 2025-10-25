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
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.server.ServerMain
import yangfentuozi.runner.shared.data.ProcessInfo

class ProcViewModel(application: Application) : AndroidViewModel(application) {
    private val _processes = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val processes: StateFlow<List<ProcessInfo>> = _processes.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _showKillAllDialog = MutableStateFlow(false)
    val showKillAllDialog: StateFlow<Boolean> = _showKillAllDialog.asStateFlow()

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
                        allProcesses?.filter { it.exe == ServerMain.USR_PATH + "/bin/bash" } ?: emptyList()
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

    fun showKillAllDialog() {
        _showKillAllDialog.value = true
    }

    fun hideKillAllDialog() {
        _showKillAllDialog.value = false
    }

    fun killProcess(pid: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            killPIDs(intArrayOf(pid))
            loadProcesses()
        }
    }

    fun killAllProcesses() {
        viewModelScope.launch(Dispatchers.IO) {
            val pids = _processes.value.map { it.pid }.toIntArray()
            killPIDs(pids)
            loadProcesses()
        }
    }

    private fun killPIDs(pids: IntArray) {
        if (Runner.pingServer()) {
            try {
                val signal = if (App.preferences.getBoolean("force_kill", false)) 9 else 15
                if (App.preferences.getBoolean("kill_child_processes", false)) {
                    val processes = Runner.service?.processes
                    if (processes == null) {
                        Runner.service?.sendSignal(pids, signal)
                    } else {
                        val childPids = mutableSetOf<Int>()
                        pids.forEach { pid ->
                            childPids.addAll(findChildProcesses(processes, pid))
                        }
                        Runner.service?.sendSignal((pids.toList() + childPids).toIntArray(), signal)
                    }
                } else {
                    Runner.service?.sendSignal(pids, signal)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun findChildProcesses(processes: Array<ProcessInfo>, pid: Int): List<Int> {
        val result = mutableListOf<Int>()
        processes.filter { it.ppid == pid }.forEach { child ->
            result.add(child.pid)
            result.addAll(findChildProcesses(processes, child.pid))
        }
        return result.distinct()
    }
}

