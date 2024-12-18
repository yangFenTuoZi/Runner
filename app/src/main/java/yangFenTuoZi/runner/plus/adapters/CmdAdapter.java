package yangFenTuoZi.runner.plus.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.cli.CmdInfo;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.ui.dialog.BaseDialogBuilder;
import yangFenTuoZi.runner.plus.ui.dialog.ExecDialogBuilder;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.ui.fragment.HomeFragment;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;


public class CmdAdapter extends RecyclerView.Adapter<CmdAdapter.ViewHolder> {
    private final int[] data;
    private final CmdInfo[] cmdInfos;
    private final MainActivity mContext;
    private final HomeFragment home;
    public final static int long_click_copy_name = 0;
    public final static int long_click_copy_command = 1;
    public final static int long_click_new = 2;
    public final static int long_click_pack = 3;
    public final static int long_click_del = 4;

    public CmdAdapter(MainActivity mContext, int[] data, HomeFragment home, CmdInfo[] cmdInfos) {

        //设置adapter需要接收两个参数：上下文、int数组
        super();
        this.mContext = mContext;
        this.data = data;
        this.cmdInfos = cmdInfos;
        this.home = home;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cmd, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        view.setOnKeyListener((v, i, keyEvent) -> false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < cmdInfos.length)
            init(holder, cmdInfos[position], data[position]);
        else {
            CmdInfo info = new CmdInfo();
            info.id = data[position];
            init(holder, info, data[position]);
        }
    }

    @Override
    public int getItemCount() {
        return data.length;
    }

    //判断是否为空
    static boolean[] isEmpty(CmdInfo info) {
        boolean exist_c = info.command == null || info.command.isEmpty();
        boolean exist_n = info.name == null || info.name.isEmpty();
        return new boolean[]{exist_c && exist_n, exist_n, exist_c};
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text_name;
        TextView text_command;
        LinearLayout list_run;
        LinearLayout list_add;
        MaterialButton item_button;
        MaterialCardView layout;

        public ViewHolder(@NonNull View view) {
            super(view);
            text_name = view.findViewById(R.id.item_name);
            text_command = view.findViewById(R.id.item_command);
            list_run = view.findViewById(R.id.list_run);
            list_add = view.findViewById(R.id.list_add);
            item_button = view.findViewById(R.id.item_button);
            layout = view.findViewById(R.id.item_root);
        }
    }

    void init(ViewHolder holder, CmdInfo info, int id) {

        boolean[] empty = isEmpty(info);

        //这个点击事件是点击编辑命令
        @SuppressLint("WrongConstant") View.OnClickListener voc = view -> {
            if (mContext.isDialogShow)
                return;
            View v = View.inflate(mContext, R.layout.dialog_edit, null);
            final MaterialSwitch chid = v.findViewById(R.id.dialog_chid);
            final MaterialSwitch keep_in_alive = v.findViewById(R.id.dialog_keep_it_alive);
            final View view2 = v.findViewById(R.id.dialog_uid_gid);
            final TextInputEditText name = v.findViewById(R.id.dialog_name);
            final TextInputEditText command = v.findViewById(R.id.dialog_command);
            final TextInputEditText ids = v.findViewById(R.id.dialog_ids);

            chid.setChecked(info.useChid);
            chid.setOnClickListener(view1 -> view2.setVisibility(chid.isChecked() ? View.VISIBLE : View.GONE));
            keep_in_alive.setChecked(info.keepAlive);
            view2.setVisibility(info.useChid ? View.VISIBLE : View.GONE);
            name.setText(info.name);
            command.setText(info.command);

            ids.setText(info.ids);
            name.requestFocus();
            name.postDelayed(() -> ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(name, 0), 200);
            mContext.isDialogShow = true;
            new MaterialAlertDialogBuilder(mContext).setTitle(mContext.getString(R.string.dialog_edit)).setView(v).setPositiveButton(mContext.getString(R.string.dialog_finish), (dialog, which) -> {
                if (!App.pingServer()) {
                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                    return;
                }
                CmdInfo new_cmdInfo = new CmdInfo();
                new_cmdInfo.id = id;
                new_cmdInfo.command = Objects.requireNonNull(command.getText()).toString();
                new_cmdInfo.name = Objects.requireNonNull(name.getText()).toString();
                new_cmdInfo.keepAlive = keep_in_alive.isChecked();
                new_cmdInfo.useChid = chid.isChecked();
                new_cmdInfo.ids = chid.isChecked() ? Objects.requireNonNull(ids.getText()).toString() : null;
                try {
                    App.iService.edit(new_cmdInfo);
                } catch (RemoteException ignored) {
                }
                if (!empty[0] && isEmpty(new_cmdInfo)[0]) {
                    try {
                        App.iService.delete(id);
                    } catch (RemoteException ignored) {
                    }
                }
                home.getBinding().recyclerView.setAdapter(null);
                home.initList();
            }).setOnDismissListener(dialog -> mContext.isDialogShow = false).show();
        };

        //如果用户还没设置命令内容，则显示加号，否则显示运行符号
        holder.list_run.setVisibility(empty[0] ? View.GONE : View.VISIBLE);
        holder.list_add.setVisibility(empty[0] ? View.VISIBLE : View.GONE);

        //如果用户还没设置命令内容，则点击时将编辑命令，否则点击将运行命令
        holder.item_button.setOnClickListener(view -> {
            if (mContext.isDialogShow)
                return;

            if (App.pingServer()) {
                try {
                    CmdInfo cmdInfo = App.iService.getCmdByID(id);
                    Intent intent = new Intent()
                            .putExtra("id", cmdInfo.id)
                            .putExtra("command", cmdInfo.command)
                            .putExtra("name", cmdInfo.name)
                            .putExtra("keep_in_alive", cmdInfo.keepAlive);
                    if (cmdInfo.useChid) {
                        intent.putExtra("chid", true)
                                .putExtra("ids", cmdInfo.ids);
                    }
                    new ExecDialogBuilder(mContext, intent).show();
                } catch (RemoteException | BaseDialogBuilder.DialogShowException ignored) {
                }
            } else
                Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
        });

        //点击编辑
        holder.layout.setOnClickListener(voc);

        holder.text_name.setText(empty[1] ? getItalicText("__NAME__") : info.name);
        holder.text_command.setText(empty[2] ? getItalicText("__COMMAND__") : info.command);

        //如果不为空则设置长按菜单
        if (!isEmpty(info)[0])
            holder.layout.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                menu.add(id, long_click_copy_name, 0, mContext.getString(R.string.long_click_copy_name));
                menu.add(id, long_click_copy_command, 0, mContext.getString(R.string.long_click_copy_command));
                menu.add(id, long_click_new, 0, mContext.getString(R.string.long_click_new));
                menu.add(id, long_click_pack, 0, mContext.getString(R.string.long_click_pack));
                menu.add(id, long_click_del, 0, mContext.getString(R.string.long_click_del));
            });
    }

    public CharSequence getItalicText(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.setSpan(new StyleSpan(Typeface.ITALIC), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}
