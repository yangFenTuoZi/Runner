package com.shizuku.runner.plus.adapters;

import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shizuku.runner.plus.IUserService;
import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.ui.activity.MainActivity;
import com.shizuku.runner.plus.ui.fragment.ProcessesFragment;

public class ProcessAdapter extends BaseAdapter {
    private final int[] data;
    private final MainActivity mContext;
    private final ProcessesFragment processesFragment;
    private String[] processesInfo;

    public ProcessAdapter(MainActivity mContext, int[] data, ProcessesFragment processesFragment) {
        //设置adapter需要接收两个参数：上下文、int数组
        super();
        this.mContext = mContext;
        this.data = data;
        this.processesFragment = processesFragment;
    }

    public void setProcessesInfo(String[] processesInfo) {
        this.processesInfo = processesInfo;
    }

    //固定的写法
    public int getCount() {
        return data.length;
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
        init(holder, data[position]);
        return convertView;
    }

    static class ViewHolder {
        TextView text_name;
        TextView text_pid;
        MaterialButton button_kill;
    }

    //获取一个进程的所有子进程(包括子子进程、子子子进程…)
    private static String getChidProcess(String pid, String[] processesInfo) {
        StringBuilder result = new StringBuilder();
        boolean firstLine = true;
        for (String processInfo : processesInfo) {
            if (firstLine) {
                firstLine = false;
                continue;
            }
            String[] pI = processInfo.replaceAll(" +", " ").split(" ");
            String PPID = pI[1];
            if (pid.equals(PPID)) {
                String PID = pI[0];
                result.append(" ");
                result.append(PID);
                result.append(getChidProcess(PID, processesInfo));
            }
        }
        return result.toString();
    }

    //获取需要杀死的所有进程
    public static String getPIDs(String pid, String[] processesInfo) {
        return pid + getChidProcess(pid, processesInfo);
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

    //噶进程，删管道，顺便判断死没死透
    public boolean killPID(String pipe, int pid) {
        if (mContext.iUserService != null) {
            try {
                IUserService iUserService = mContext.iUserService;
                String PIDs = getPIDs(String.valueOf(pid), processesInfo);
                iUserService.exec("kill -9 " + PIDs + "\nrm -rf " + pipe + "\n", mContext.getApplicationInfo().packageName);
                processesInfo = iUserService.exec("busybox ps -A -o pid,ppid | grep " + pid, mContext.getApplicationInfo().packageName).split("\n");
                return isDied(String.valueOf(pid), processesInfo);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    //噶进程，删管道，顺便判断死没死透
    public static boolean killPID(String pipe, int pid, MainActivity mContext) {
        if (mContext.iUserService != null) {
            try {
                IUserService iUserService = mContext.iUserService;
                String[] processesInfo = iUserService.exec("busybox ps -A -o pid,ppid | grep " + pid, mContext.getApplicationInfo().packageName).split("\n");
                String PIDs = getPIDs(String.valueOf(pid), processesInfo);
                iUserService.exec("kill -9 " + PIDs + "\nrm -rf " + pipe + "\n", mContext.getApplicationInfo().packageName);
                return isDied(String.valueOf(pid), iUserService.exec("busybox ps -A -o pid,ppid | grep " + pid, mContext.getApplicationInfo().packageName).split("\n"));
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    //初始化
    void init(ViewHolder holder, int pid) {

        //判断进程是否已经死了，如果是就删掉这个格子
        try {
            if (mContext.iUserService != null)
                if (isDied(String.valueOf(pid), processesInfo)) {
                    mContext.deleteSharedPreferences("proc_" + pid);
                    mContext.runOnUiThread(processesFragment::initList);
                    processesInfo = mContext.iUserService.exec("busybox ps -A -o pid,ppid | grep " + pid, mContext.getApplicationInfo().packageName).split("\n");
                }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        String name = mContext.getSharedPreferences("proc_" + pid, 0).getString("name", "");
        holder.text_name.setText(name.isEmpty() ? "Process" : name);
        holder.text_pid.setText(String.valueOf(pid));

        //设置点击事件
        holder.button_kill.setOnClickListener((view) -> new MaterialAlertDialogBuilder(mContext).setTitle(R.string.dialog_kill_this_process).setPositiveButton(R.string.dialog_finish, (dialog, which) -> new Thread(() -> {

            //杀死进程
            if (killPID(mContext.getSharedPreferences("proc_" + pid, 0).getString("pipe", ""), pid)) {
                mContext.deleteSharedPreferences("proc_" + pid);
                mContext.runOnUiThread(() -> {
                    processesFragment.initList();
                    Toast.makeText(mContext, R.string.process_the_killing_process_succeeded, Toast.LENGTH_SHORT).show();
                });
            } else
                mContext.runOnUiThread(() -> Toast.makeText(mContext, R.string.process_failed_to_kill_the_process, Toast.LENGTH_SHORT).show());
        }).start()).setNeutralButton(R.string.dialog_cancel, null).show());
    }
}
