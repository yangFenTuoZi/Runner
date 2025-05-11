package yangfentuozi.runner.ui.fragment.proc

import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import yangfentuozi.runner.App
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseDialogBuilder
import yangfentuozi.runner.databinding.HomeItemContainerBinding
import yangfentuozi.runner.databinding.ItemProcBinding
import yangfentuozi.runner.service.ServiceImpl
import yangfentuozi.runner.service.data.ProcessInfo
import yangfentuozi.runner.ui.activity.MainActivity

class ProcAdapter(private val mContext: MainActivity, val mFragment: ProcFragment) :
    RecyclerView.Adapter<ProcAdapter.ViewHolder>() {

    private var data: List<ProcessInfo>? = null

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val outer =
            HomeItemContainerBinding.inflate(LayoutInflater.from(parent.context)!!, parent, false)
        val inner =
            ItemProcBinding.inflate(LayoutInflater.from(parent.context), outer.getRoot(), true)
        return ViewHolder(inner, outer)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        var processInfo: ProcessInfo? = data?.get(position)
        if (processInfo == null) return

        var niceNameFlag = false
        for (arg in processInfo.args) {
            if (niceNameFlag) {
                holder.mBindingInner.itemName.text = arg
                break
            }
            if (arg == "--nice-name") niceNameFlag = true
        }

        holder.mBindingInner.itemPid.text = mContext.getString(R.string.pid_info, processInfo.pid)

        //设置点击事件
        holder.mBindingInner.itemButton.setOnClickListener {
            try {
                BaseDialogBuilder(mContext)
                    .setTitle(R.string.kill_process_ask)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        Thread {
                            //杀死进程
                            killPIDs(intArrayOf(processInfo.pid))
                            mContext.runOnUiThread { updateData() }
                        }.start()
                    }
                    .setNeutralButton(android.R.string.cancel, null)
                    .show()
            } catch (_: BaseDialogBuilder.DialogShowingException) {
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class ViewHolder(bindingInner: ItemProcBinding, bindingOuter: HomeItemContainerBinding) :
        RecyclerView.ViewHolder(bindingOuter.root) {
        val mBindingInner: ItemProcBinding = bindingInner
        val mBindingOuter: HomeItemContainerBinding = bindingOuter
    }

    fun updateData() {
        mContext.runOnUiThread { mFragment.binding.swipeRefreshLayout.isRefreshing = true }
        data = if (Runner.pingServer()) {
            try {
                val processes = Runner.service?.processes
                if (processes == null) null
                else {
                    val a: ArrayList<ProcessInfo> = ArrayList()
                    for (pi in processes) {
                        if (pi.exe == ServiceImpl.USR_PATH + "/bin/bash") a.add(pi)
                    }
                    a
                }
            } catch (_: RemoteException) {
                null
            }
        } else {
            null
        }
        notifyDataSetChanged()
        mContext.runOnUiThread { mFragment.binding.swipeRefreshLayout.isRefreshing = false }
    }

    fun killAll() {
        val pids = IntArray(data?.size ?: 0)
        for (i in 0 until (data?.size ?: 0)) {
            pids[i] = data?.get(i)?.pid ?: -1
        }
        if (pids.isEmpty()) return
        killPIDs(pids)
        updateData()
    }

    open class GetChildProcesses(val mProcesses: Array<ProcessInfo>) {
        private var mResult: ArrayList<Int> = ArrayList<Int>()
        fun find(pid: Int) {
            // 遍历所有进程，查找父进程 ID 匹配的子进程
            for (process in mProcesses) {
                if (process.ppid == pid) {
                    // 添加子进程 ID 到结果列表
                    mResult.add(process.pid)
                    // 递归查找该子进程的子进程
                    find(process.pid)
                }
            }
        }
        fun fix() {
            mResult = ArrayList(LinkedHashSet(mResult))
        }
        fun getResult(): ArrayList<Int> {
            return mResult
        }
    }

    companion object {

        fun killPIDs(pids: IntArray) {
            if (Runner.pingServer()) {
                try {
                    val signal = if (App.getPreferences().getBoolean("force_kill", false)) 9 else 15
                    if (App.getPreferences().getBoolean("kill_child_processes", false)) {
                        val processes = Runner.service?.processes
                        if (processes == null) {
                            Runner.service?.sendSignal(pids, signal)
                        } else {
                            val getChildProcesses = GetChildProcesses(processes)
                            for (i in pids)
                                getChildProcesses.find(i)
                            getChildProcesses.fix()
                            Runner.service?.sendSignal(getChildProcesses.getResult().toIntArray(), signal)
                        }
                    } else Runner.service?.sendSignal(pids, signal)
                } catch (e: RemoteException) {
                    Log.e("ProcAdapter", "killPIDs: ", e)
                    null
                }
            } else null
        }
    }
}