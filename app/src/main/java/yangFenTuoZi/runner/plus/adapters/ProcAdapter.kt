package yangFenTuoZi.runner.plus.adapters;

import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.Runner;
import yangFenTuoZi.runner.plus.service.IService;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.ui.fragment.proc.ProcFragment;

public class ProcAdapter extends RecyclerView.Adapter<ProcAdapter.ViewHolder> {
    private final int[] data;
    private final String[] data_name;
    private final MainActivity mContext;
    private final ProcFragment procFragment;

    public ProcAdapter(MainActivity mContext, int[] data, String[] data_name, ProcFragment procFragment) {
        //设置adapter需要接收两个参数：上下文、int数组
        super();
        this.mContext = mContext;
        this.data = data;
        this.procFragment = procFragment;
        this.data_name = data_name;
    }

    //获取长度
    @Override
    public int getItemCount() {
        int i = getRealItemCount();
        return i == 0 ? 1 : i;
    }

    public int getRealItemCount() {
        int i = 0;
        for (int x : data)
            if (x != 0) i++;
        return i;
    }

    public boolean isEmpty() {
        return getRealItemCount() == 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        boolean emptyPage = isEmpty();
        if (emptyPage) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.empty_processes, parent, false);
            view.getRootView().setOnClickListener(v -> {
                SwipeRefreshLayout refreshLayout = procFragment.getBinding().swipeRefreshLayout;
                if (!refreshLayout.isRefreshing()) {
                    refreshLayout.setRefreshing(true);
                    procFragment.onRefreshListener.onRefresh();
                }
            });
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_proc, parent, false);
        }
        ViewHolder holder = new ViewHolder(view, emptyPage);
        view.setTag(holder);
        view.setOnKeyListener((v, i, keyEvent) -> false);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!isEmpty())
            init(holder, data[position], data_name[position]);
    }

    //固定的写法
    @Override
    public long getItemId(int position) {
        return position;
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text_name;
        TextView text_pid;
        MaterialButton button_kill;


        public ViewHolder(View view, boolean emptyPage) {
            super(view);
            if (emptyPage) return;
            text_name = view.findViewById(R.id.item_proc_name);
            text_pid = view.findViewById(R.id.item_proc_pid);
            button_kill = view.findViewById(R.id.item_proc_kill);
        }
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
        if (Runner.pingServer()) {
            try {
                IService iService = Runner.service;
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
        if (Runner.pingServer()) {
            try {
                IService iService = Runner.service;
                StringBuilder cmd = new StringBuilder();
                for (int pid : PIDs) {
                    if (pid != 0)
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
                    procFragment.initList();
                    Toast.makeText(mContext, R.string.process_the_killing_process_succeeded, Toast.LENGTH_SHORT).show();
                });
            } else
                mContext.runOnUiThread(() -> Toast.makeText(mContext, R.string.process_failed_to_kill_the_process, Toast.LENGTH_SHORT).show());
        }).start()).setNeutralButton(R.string.dialog_cancel, null).show());
    }
}
