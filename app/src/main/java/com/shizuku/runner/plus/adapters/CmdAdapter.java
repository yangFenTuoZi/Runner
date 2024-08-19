package com.shizuku.runner.plus.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shizuku.runner.plus.ui.activity.MainActivity;
import com.shizuku.runner.plus.ui.dialog.ExecAlertDialog;
import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.ui.fragment.HomeFragment;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Objects;


public class CmdAdapter extends BaseAdapter {
    private final int[] data;
    private final Context mContext;
    private final HomeFragment home;
    public final static int long_click_copy_name = 0;
    public final static int long_click_copy_command = 1;
    public final static int long_click_new = 2;
    public final static int long_click_pack = 3;
    public final static int long_click_del = 4;

    public CmdAdapter(Context mContext, int[] data, HomeFragment home) {

        //设置adapter需要接收两个参数：上下文、int数组
        super();
        this.mContext = mContext;
        this.data = data;
        this.home = home;
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
        SharedPreferences b = mContext.getSharedPreferences(String.valueOf(data[position]), 0);
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_cmd, null);
            holder = new ViewHolder();
            holder.text_name = convertView.findViewById(R.id.item_name);
            holder.text_command = convertView.findViewById(R.id.item_command);
            holder.list_run = convertView.findViewById(R.id.list_run);
            holder.list_add = convertView.findViewById(R.id.list_add);
            holder.item_button = convertView.findViewById(R.id.item_button);
            holder.item_button_add = convertView.findViewById(R.id.item_button_add);
            holder.layout = convertView.findViewById(R.id.item_root);
            convertView.setTag(holder);
            convertView.setOnKeyListener((view, i, keyEvent) -> false);
        } else {

            //对于已经加载过的item就直接使用，不需要再次加载了，这就是ViewHolder的作用
            holder = (ViewHolder) convertView.getTag();
        }

        //获得用户对于这个格子的设置
        init(holder, b, data[position]);
        return convertView;
    }

    static boolean[] isEmpty(SharedPreferences b) {
        boolean exist_c = b.getString("command", null) == null || b.getString("command", "").isEmpty();
        boolean exist_n = b.getString("name", null) == null || b.getString("name", "").isEmpty();
        return new boolean[]{exist_c && exist_n, exist_n, exist_c};
    }

    static class ViewHolder {
        TextView text_name;
        TextView text_command;
        LinearLayout list_run;
        LinearLayout list_add;
        MaterialButton item_button;
        MaterialButton item_button_add;
        LinearLayout layout;
    }

    void init(ViewHolder holder, SharedPreferences b, int id) {

        boolean[] empty = isEmpty(b);

        //这个点击事件是点击编辑命令
        View.OnClickListener voc = view -> {
            View v = View.inflate(mContext, R.layout.dialog_edit, null);
            final MaterialSwitch chid = v.findViewById(R.id.dialog_chid);
            final MaterialSwitch keep_in_alive = v.findViewById(R.id.dialog_keep_it_alive);
            final View view2 = v.findViewById(R.id.dialog_uid_gid);
            final TextInputEditText name = v.findViewById(R.id.dialog_name);
            final TextInputEditText command = v.findViewById(R.id.dialog_command);
            final TextInputEditText ids = v.findViewById(R.id.dialog_ids);

            chid.setChecked(b.getBoolean("chid", false));
            chid.setOnClickListener(view1 -> view2.setVisibility(chid.isChecked() ? View.VISIBLE : View.GONE));
            keep_in_alive.setChecked(b.getBoolean("keep_in_alive", false));
            view2.setVisibility(b.getBoolean("chid", false) ? View.VISIBLE : View.GONE);
            name.setText(b.getString("name", null));
            command.setText(b.getString("command", null));

            ids.setText(b.getString("ids", null));
            name.requestFocus();
            name.postDelayed(() -> ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(name, 0), 200);
            new MaterialAlertDialogBuilder(mContext).setTitle(mContext.getString(R.string.dialog_edit)).setView(v).setPositiveButton(mContext.getString(R.string.dialog_finish), (dialog, which) -> {
                b.edit().putString("command", Objects.requireNonNull(command.getText()).toString())
                        .putString("name", Objects.requireNonNull(name.getText()).toString())
                        .putBoolean("keep_in_alive", keep_in_alive.isChecked())
                        .putBoolean("chid", chid.isChecked())
                        .putString("ids", chid.isChecked() ? Objects.requireNonNull(ids.getText()).toString() : null)
                        .apply();
                if (empty[0] && !isEmpty(b)[0]) {
                    SharedPreferences sp = mContext.getSharedPreferences("data", 0);
                    sp.edit().putString("data", sp.getString("data", "").isEmpty()
                            ? String.valueOf(id)
                            : sp.getString("data", "") + "," + id
                    ).apply();
                    init(holder, b, id);
                    home.initList();
                } else
                    init(holder, b, id);
            }).show();
        };

        //如果用户还没设置命令内容，则显示加号，否则显示运行符号
        holder.list_run.setVisibility(empty[0] ? View.GONE : View.VISIBLE);
        holder.list_add.setVisibility(empty[0] ? View.VISIBLE : View.GONE);

        //如果用户还没设置命令内容，则点击时将编辑命令，否则点击将运行命令
        holder.item_button.setOnClickListener(view -> {

            //这里会根据用户是否勾选了降权，来执行不同的命令
            Intent intent = new Intent()
                    .putExtra("name", b.getString("name",""))
                    .putExtra("command", b.getString("command", ""))
                    .putExtra("keep_in_alive", b.getBoolean("keep_in_alive", false));
            if (b.getBoolean("chid", false)) {
                intent.putExtra("chid", b.getBoolean("chid", false))
                        .putExtra("ids", b.getString("ids", ""));
            }
            new ExecAlertDialog((MainActivity) mContext, intent).show();
        });
        holder.item_button_add.setOnClickListener(voc);
        holder.text_name.setText(empty[1] ? "" : b.getString("name", ""));
        holder.text_command.setText(empty[2] ? "" : b.getString("command", ""));
        holder.layout.setOnClickListener(voc);
        if (new File(mContext.getApplicationInfo().dataDir + "/shared_prefs/" + id + ".xml").exists())
            holder.layout.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                menu.setHeaderTitle("选择操作");
                menu.add(id, long_click_copy_name, 0, mContext.getString(R.string.long_click_copy_name));
                menu.add(id, long_click_copy_command, 0, mContext.getString(R.string.long_click_copy_command));
                menu.add(id, long_click_new, 0, mContext.getString(R.string.long_click_new));
                menu.add(id, long_click_pack, 0, mContext.getString(R.string.long_click_pack));
                menu.add(id, long_click_del, 0, mContext.getString(R.string.long_click_del));
            });
    }
}
