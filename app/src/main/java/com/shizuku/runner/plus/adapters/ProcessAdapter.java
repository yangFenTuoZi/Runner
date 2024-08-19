package com.shizuku.runner.plus.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.ui.fragment.ProcessesFragment;

public class ProcessAdapter extends BaseAdapter {
    private final int[] data;
    private final Context mContext;
    private final ProcessesFragment processesFragment;

    public ProcessAdapter(Context mContext, int[] data, ProcessesFragment processesFragment) {
        //设置adapter需要接收两个参数：上下文、int数组
        super();
        this.mContext = mContext;
        this.data = data;
        this.processesFragment = processesFragment;
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

    void init(ViewHolder holder, int id) {
        String name = mContext.getSharedPreferences("proc_" + id, 0).getString("name", "");
        holder.text_name.setText(name.isEmpty() ? "Process" : name);
        holder.text_pid.setText(String.valueOf(id));
        holder.button_kill.setOnClickListener((view) -> new MaterialAlertDialogBuilder(mContext).setTitle(R.string.dialog_kill_this_process).setPositiveButton(R.string.dialog_finish, (dialog, which) -> {

        }).setNeutralButton(R.string.dialog_cancel, null).show());
    }
}
