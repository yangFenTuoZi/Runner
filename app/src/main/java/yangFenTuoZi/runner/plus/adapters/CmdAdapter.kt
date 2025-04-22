package yangFenTuoZi.runner.plus.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.Runner;
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder;
import yangFenTuoZi.runner.plus.databinding.DialogEditBinding;
import yangFenTuoZi.runner.plus.service.CommandInfo;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.ui.dialog.ExecDialogBuilder;
import yangFenTuoZi.runner.plus.utils.ExceptionUtils;


public class CmdAdapter extends RecyclerView.Adapter<CmdAdapter.ViewHolder> {
    private int count;
    private final MainActivity mContext;
    private final ExecutorService executorService; // 用于异步加载数据
    public final static int long_click_copy_name = 0;
    public final static int long_click_copy_command = 1;
    public final static int long_click_new = 2;
    public final static int long_click_pack = 3;
    public final static int long_click_del = 4;

    public CmdAdapter(MainActivity mContext, int count) {
        super();
        this.mContext = mContext;
        this.executorService = Executors.newSingleThreadExecutor();
        updateData(count);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(int count) {
        this.count = count;
        notifyDataSetChanged();

        try {
            Runner.service.closeCursor();
        } catch (RemoteException e) {
            ExceptionUtils.throwableToDialog(mContext, e);
        }
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

        // 异步加载数据
        executorService.execute(() -> {
            try {
                CommandInfo info = Runner.service.query(holder.position());
                mContext.runOnUiThread(() -> init(holder, info)); // 回到主线程更新 UI
            } catch (RemoteException e) {
                ExceptionUtils.throwableToDialog(mContext, e);
            }
        });
    }

    @Override
    public int getItemCount() {
        return count;
    }

    //判断是否为空
    static boolean[] isEmpty(CommandInfo info) {
        boolean exist_c = info.command == null || info.command.isEmpty();
        boolean exist_n = info.name == null || info.name.isEmpty();
        return new boolean[]{exist_c && exist_n, exist_n, exist_c};
    }

    public void remove(int position) {
        try {
            Runner.service.delete(position + 1);
            count--;
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount() - position);
        } catch (RemoteException e) {
            ExceptionUtils.throwableToDialog(mContext, e);
        }
    }

    public void add(CommandInfo info) {
        try {
            info.rowid = count++;
            Runner.service.insert(info);
            notifyItemChanged(count);
        } catch (RemoteException e) {
            ExceptionUtils.throwableToDialog(mContext, e);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text_name;
        TextView text_command;
        MaterialButton item_button;
        MaterialCardView layout;

        public ViewHolder(@NonNull View view) {
            super(view);
            text_name = view.findViewById(R.id.item_name);
            text_command = view.findViewById(R.id.item_command);
            item_button = view.findViewById(R.id.item_button);
            layout = view.findViewById(R.id.item_root);
        }

        public int position() {
            return getBindingAdapterPosition();
        }
    }

    void init(ViewHolder holder, CommandInfo info) {

        boolean[] empty = isEmpty(info);

        //如果用户还没设置命令内容，则点击时将编辑命令，否则点击将运行命令
        holder.item_button.setOnClickListener(view -> {
            if (mContext.isDialogShow)
                return;

            if (Runner.pingServer()) {
                try {
                    new ExecDialogBuilder(mContext, info).show();
                } catch (BaseDialogBuilder.DialogShowException ignored) {
                }
            } else
                Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show();
        });

        //点击编辑
        holder.layout.setOnClickListener(view -> {
            if (mContext.isDialogShow)
                return;
            DialogEditBinding binding = DialogEditBinding.inflate(LayoutInflater.from(mContext));

            binding.dialogChid.setChecked(info.useChid);
            binding.dialogChid.setOnCheckedChangeListener((buttonView, isChecked) -> binding.dialogUidGid.setVisibility(isChecked ? View.VISIBLE : View.GONE));
            binding.dialogKeepItAlive.setChecked(info.keepAlive);
            binding.dialogUidGid.setVisibility(info.useChid ? View.VISIBLE : View.GONE);
            binding.dialogName.setText(info.name);
            binding.dialogCommand.setText(info.command);

            binding.dialogIds.setText(info.ids);
            binding.dialogName.requestFocus();
            binding.dialogName.postDelayed(() -> ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(binding.dialogName, 0), 200);
            mContext.isDialogShow = true;
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle(mContext.getString(R.string.dialog_edit))
                    .setView(binding.getRoot())
                    .setPositiveButton(mContext.getString(R.string.dialog_finish), (dialog, which) -> {
                        if (!Runner.pingServer()) {
                            Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        info.rowid = holder.position();
                        info.command = String.valueOf(binding.dialogCommand.getText());
                        info.name = String.valueOf(binding.dialogName.getText());
                        info.keepAlive = binding.dialogKeepItAlive.isChecked();
                        info.useChid = binding.dialogChid.isChecked();
                        info.ids = binding.dialogChid.isChecked() ? Objects.requireNonNull(binding.dialogIds.getText()).toString() : null;
                        try {
                            Runner.service.update(info);
                        } catch (RemoteException ignored) {
                        }
                        if (!empty[0] && isEmpty(info)[0])
                            remove(holder.position());
                        else {
                            init(holder, info);
                        }
                    }).setOnDismissListener(dialog -> mContext.isDialogShow = false).show();
        });

        holder.text_name.setText(empty[1] ? getItalicText("__NAME__") : info.name);
        holder.text_command.setText(empty[2] ? getItalicText("__CMD__") : info.command);


        //如果不为空则设置长按菜单
        holder.layout.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            menu.add(holder.position(), long_click_copy_name, 0, mContext.getString(R.string.long_click_copy_name));
            menu.add(holder.position(), long_click_copy_command, 0, mContext.getString(R.string.long_click_copy_command));
            menu.add(holder.position(), long_click_new, 0, mContext.getString(R.string.long_click_new));
            menu.add(holder.position(), long_click_pack, 0, mContext.getString(R.string.long_click_pack));
            menu.add(holder.position(), long_click_del, 0, mContext.getString(R.string.long_click_del));
        });
    }

    public CharSequence getItalicText(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.setSpan(new StyleSpan(Typeface.ITALIC), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    public void close() {
        executorService.shutdown();

    }
}
