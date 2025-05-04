package yangFenTuoZi.runner.plus.ui.fragment.proc

import android.os.RemoteException
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder
import yangFenTuoZi.runner.plus.service.callback.IExecResultCallback
import yangFenTuoZi.runner.plus.ui.activity.MainActivity

class ProcAdapter
    (
    private val mContext: MainActivity,
    private val data: IntArray,
    private val dataName: Array<String?>,
    private val procFragment: ProcFragment
) : RecyclerView.Adapter<ProcAdapter.ViewHolder?>() {

    override fun getItemCount(): Int {
        var i = 0
        for (x in data) if (x != 0) i++
        return i
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proc, parent, false)
        val holder = ViewHolder(view)
        view.tag = holder
        view.setOnKeyListener(View.OnKeyListener { v: View?, i: Int, keyEvent: KeyEvent? -> false })
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        init(holder, data[position], dataName[position])
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var textName: TextView = view.findViewById<TextView?>(R.id.item_proc_name)
        var textPid: TextView = view.findViewById<TextView?>(R.id.item_proc_pid)
        var buttonKill: MaterialButton = view.findViewById<MaterialButton?>(R.id.item_proc_kill)
    }

    //初始化
    fun init(holder: ViewHolder, pid: Int, name: String?) {
        holder.textName.text = name
        holder.textPid.text = mContext.getString(R.string.exec_pid, pid)

        //设置点击事件
        holder.buttonKill.setOnClickListener(View.OnClickListener { view: View? ->
            try {
                BaseDialogBuilder(mContext)
                    .setTitle(R.string.dialog_kill_this_process)
                    .setPositiveButton(R.string.dialog_finish) { dialog, which ->
                        Thread {
                            //杀死进程
                            if (killPID(pid)) {
                                mContext.runOnUiThread {
                                    procFragment.initList()
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
        })
    }

    companion object {
        fun isDied(pid: String, processesInfo: Array<String>): Boolean {
            var firstLine = true
            var isAlive = false
            for (processInfo in processesInfo) {
                if (firstLine) {
                    firstLine = false
                    continue
                }
                if (pid == processInfo.replace(" +".toRegex(), " ").split(" ".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                ) isAlive = true
            }
            return !isAlive
        }

        fun killPID(pid: Int): Boolean {
            if (Runner.pingServer()) {
                try {
                    Runner.service?.exec("kill -9 $pid", null, "Task-KillProc", null)
                    val outs = StringBuilder()
                    var waited = -1
                    Runner.service?.exec(
                        "busybox ps -A -o pid,ppid|grep $pid",
                        null,
                        "Task-GetProcList",
                        object : IExecResultCallback.Stub() {
                            @Throws(RemoteException::class)
                            override fun onOutput(outputs: String?) {
                                outs.append(outputs)
                            }

                            @Throws(RemoteException::class)
                            override fun onExit(exitValue: Int) {
                                if (exitValue != 0) {
                                    waited = if (isDied(
                                            pid.toString(),
                                            outs.toString().split("\n".toRegex())
                                                .dropLastWhile { it.isEmpty() }
                                                .toTypedArray())
                                    ) 1 else 0
                                }
                            }
                        })
                    while (waited == -1);
                    return waited == 1
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
            return false
        }

        fun killPIDs(pids: IntArray) {
            if (Runner.pingServer()) {
                try {
                    val cmd = StringBuilder()
                    for (pid in pids) {
                        if (pid != 0) cmd.append("kill -9 ").append(pid).append("\n")
                    }
                    Runner.service?.exec(cmd.toString(), null, "Task-KillProc", null)
                } catch (e: RemoteException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}