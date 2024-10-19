package yangFenTuoZi.runner.plus.adapters;

import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.server.IService;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.ui.fragment.ProcessesFragment;

public class ProcessAdapter extends BaseAdapter {
    private final int[] data;
    private final String[] data_name;
    private final MainActivity mContext;
    private final ProcessesFragment processesFragment;

    public ProcessAdapter(MainActivity mContext, int[] data, String[] data_name, ProcessesFragment processesFragment) {
        //设置adapter需要接收两个参数：上下文、int数组
        super();
        this.mContext = mContext;
        this.data = data;
        this.processesFragment = processesFragment;
        this.data_name = data_name;
    }

    //获取长度
    public int getCount() {
        int i = 0;
        for (int x : data)
            if (x != 0) i++;
        mContext.findViewById(R.id.proc_mask).setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        return i;
    }

    //固定的写法
    @Override
    public Object getItem(int position) {
        return null;
    }

    //固定的写法
    @Override
    public long getItemId(int position) {
        return position;
    }

    //此函数定义每一个item的显示
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_process, null);
            holder = new ViewHolder();
            holder.text_name = convertView.findViewById(R.id.item_proc_name);
            holder.text_pid = convertView.findViewById(R.id.item_proc_pid);
            holder.button_kill = convertView.findViewById(R.id.item_proc_kill);
            convertView.setTag(holder);
            convertView.setOnKeyListener((view, i, keyEvent) -> false);
        } else {

            //对于已经加载过的item就直接使用，不需要再次加载了，这就是ViewHolder的作用
            holder = (ViewHolder) convertView.getTag();
        }

        //获得用户对于这个格子的设置
        init(holder, data[position], data_name[position]);
        return convertView;
    }

    static class ViewHolder {
        TextView text_name;
        TextView text_pid;
        MaterialButton button_kill;
    }

    //判断进程是否噶了
    public static boolean isDied(String pid, String[] processesInfo) {
        boolean firstLine = true;
        boolean isAlive = false;
        for (String processInfo : processesInfo) {
            if (firstLine) {
                firstLine = false;
                continue;
            }
            if (pid.equals(processInfo.replaceAll(" +", " ").split(" ")[0]))
                isAlive = true;
        }
        return !isAlive;
    }

    //噶进程，顺便判断死没死透
    public static boolean killPID(int pid) {
        if (App.pingServer()) {
            try {
                IService iService = App.iService;
                iService.exec("kill -9 " + pid);
                return isDied(String.valueOf(pid), iService.exec("busybox ps -A -o pid,ppid|grep " + pid).split("\n"));
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    //噶进程，顺便判断死没死透
    public static void killPIDs(int[] PIDs) {
        if (App.pingServer()) {
            try {
                IService iService = App.iService;
                StringBuilder cmd = new StringBuilder();
                for (int pid : PIDs) {
                    cmd.append("kill -9 ").append(pid).append("\n");
                }
                iService.exec(cmd.toString());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //初始化
    void init(ViewHolder holder, int pid, String name) {

        holder.text_name.setText(name);
        holder.text_pid.setText(mContext.getString(R.string.exec_pid, pid));

        //设置点击事件
        holder.button_kill.setOnClickListener((view) -> new MaterialAlertDialogBuilder(mContext).setTitle(R.string.dialog_kill_this_process).setPositiveButton(R.string.dialog_finish, (dialog, which) -> new Thread(() -> {

            //杀死进程
            if (killPID(pid)) {
                mContext.runOnUiThread(() -> {
                    processesFragment.initList();
                    Toast.makeText(mContext, R.string.process_the_killing_process_succeeded, Toast.LENGTH_SHORT).show();
                });
            } else
                mContext.runOnUiThread(() -> Toast.makeText(mContext, R.string.process_failed_to_kill_the_process, Toast.LENGTH_SHORT).show());
        }).start()).setNeutralButton(R.string.dialog_cancel, null).show());
    }
}
