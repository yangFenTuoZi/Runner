package yangFenTuoZi.runner.plus.adapters

import android.os.RemoteException
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.ui.activity.MainActivity
import yangFenTuoZi.runner.plus.ui.fragment.proc.ProcFragment

class ProcAdapter //设置adapter需要接收两个参数：上下文、int数组
    (
    private val mContext: MainActivity,
    private val data: IntArray,
    private val data_name: Array<String?>,
    private val procFragment: ProcFragment
) : RecyclerView.Adapter<ProcAdapter.ViewHolder?>() {
    //获取长度
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
        init(holder, data[position], data_name[position])
    }

    //固定的写法
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    //此函数定义每一个item的显示
    //    public View getView(int position, View convertView, ViewGroup parent) {
    //        ViewHolder holder;
    //        if (convertView == null) {
    //            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_process, null);
    //            holder = new ViewHolder();
    //            convertView.setTag(holder);
    //            convertView.setOnKeyListener((view, i, keyEvent) -> false);
    //        } else {
    //
    //            //对于已经加载过的item就直接使用，不需要再次加载了，这就是ViewHolder的作用
    //            holder = (ViewHolder) convertView.getTag();
    //        }
    //
    //        //获得用户对于这个格子的设置
    //        init(holder, data[position], data_name[position]);
    //        return convertView;
    //    }
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var text_name: TextView = view.findViewById<TextView?>(R.id.item_proc_name)
        var text_pid: TextView
        var button_kill: MaterialButton


        init {
            text_pid = view.findViewById<TextView?>(R.id.item_proc_pid)
            button_kill = view.findViewById<MaterialButton?>(R.id.item_proc_kill)
        }
    }

    //初始化
    fun init(holder: ViewHolder, pid: Int, name: String?) {
        holder.text_name.text = name
        holder.text_pid.text = mContext.getString(R.string.exec_pid, pid)

        //设置点击事件
        holder.button_kill.setOnClickListener(View.OnClickListener { view: View? ->
            MaterialAlertDialogBuilder(mContext).setTitle(R.string.dialog_kill_this_process)
                .setPositiveButton(R.string.dialog_finish) { dialog, which ->
                    Thread(
                        Runnable {
                            //杀死进程
                            if (killPID(pid)) {
                                mContext.runOnUiThread(Runnable {
                                    procFragment.initList()
                                    Toast.makeText(
                                        mContext,
                                        R.string.process_the_killing_process_succeeded,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                })
                            } else mContext.runOnUiThread(Runnable {
                                Toast.makeText(
                                    mContext,
                                    R.string.process_failed_to_kill_the_process,
                                    Toast.LENGTH_SHORT
                                ).show()
                            })
                        }).start()
                }.setNeutralButton(R.string.dialog_cancel, null).show()
        })
    }

    companion object {
        //判断进程是否噶了
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

        //噶进程，顺便判断死没死透
        fun killPID(pid: Int): Boolean {
            if (Runner.pingServer()) {
                try {
                    Runner.service?.exec("kill -9 $pid")
                    return isDied(
                        pid.toString(),
                        Runner.service?.exec("busybox ps -A -o pid,ppid|grep $pid")!!.split("\n".toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()
                    )
                } catch (e: RemoteException) {
                    throw RuntimeException(e)
                }
            }
            return false
        }

        //噶进程，顺便判断死没死透
        fun killPIDs(PIDs: IntArray) {
            if (Runner.pingServer()) {
                try {
                    val cmd = StringBuilder()
                    for (pid in PIDs) {
                        if (pid != 0) cmd.append("kill -9 ").append(pid).append("\n")
                    }
                    Runner.service?.exec(cmd.toString())
                } catch (e: RemoteException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}