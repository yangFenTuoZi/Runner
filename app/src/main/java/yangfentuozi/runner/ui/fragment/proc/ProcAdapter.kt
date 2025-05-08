package yangfentuozi.runner.ui.fragment.proc

import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseDialogBuilder
import yangfentuozi.runner.databinding.HomeItemContainerBinding
import yangfentuozi.runner.databinding.ItemProcBinding
import yangfentuozi.runner.service.ServiceImpl
import yangfentuozi.runner.service.data.ProcessInfo
import yangfentuozi.runner.ui.activity.MainActivity

class ProcAdapter(private val mContext: MainActivity) :
    RecyclerView.Adapter<ProcAdapter.ViewHolder>() {

    private var data: List<ProcessInfo>? = null;

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

        holder.mBindingInner.itemPid.text = mContext.getString(R.string.exec_pid, processInfo.pid)

        //设置点击事件
        holder.mBindingInner.itemButton.setOnClickListener {
            try {
                BaseDialogBuilder(mContext)
                    .setTitle(R.string.dialog_kill_this_process)
                    .setPositiveButton(R.string.dialog_finish) { dialog, which ->
                        Thread {
                            //杀死进程
                            if (killPID(processInfo.pid)) {
                                mContext.runOnUiThread {
                                    updateData()
                                    Toast.makeText(
                                        mContext,
                                        R.string.process_the_killing_process_succeeded,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else mContext.runOnUiThread {
                                Toast.makeText(
                                    mContext,
                                    R.string.process_failed_to_kill_the_process,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.start()
                    }
                    .setNeutralButton(R.string.dialog_cancel, null)
                    .show()
            } catch (_: BaseDialogBuilder.DialogShowException) {
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

    companion object {

        fun killPID(pid: Int): Boolean {
            if (Runner.pingServer()) {
                try {
                    var result: BooleanArray? = Runner.service?.sendSignal(intArrayOf(pid), 9)
                    return if (result == null) false else result[0]
                } catch (e: RemoteException) {
                    Log.e("ProcAdapter", "killPID: ", e)
                }
            }
            return false
        }

        fun killPIDs(pids: IntArray) {
            if (Runner.pingServer()) {
                if (Runner.pingServer()) {
                    try {
                        Runner.service?.sendSignal(pids, 9)
                    } catch (e: RemoteException) {
                        Log.e("ProcAdapter", "killPIDs: ", e)
                    }
                }
            }
        }
    }
}