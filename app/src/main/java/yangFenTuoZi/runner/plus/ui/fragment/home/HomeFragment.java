package yangFenTuoZi.runner.plus.ui.fragment.home;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import rikka.recyclerview.RecyclerViewKt;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.Runner;
import yangFenTuoZi.runner.plus.adapters.CmdAdapter;
import yangFenTuoZi.runner.plus.base.BaseFragment;
import yangFenTuoZi.runner.plus.databinding.DialogEditBinding;
import yangFenTuoZi.runner.plus.databinding.FragmentHomeBinding;
import yangFenTuoZi.runner.plus.service.CommandInfo;

public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private final HomeAdapter statusAdapter = new HomeAdapter();
    private CmdAdapter adapter;

    public FragmentHomeBinding getBinding() {
        return binding;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> new Handler().postDelayed(() -> {
            initList();
            binding.swipeRefreshLayout.setRefreshing(false);
        }, 1000));

        binding.add.setOnClickListener(view -> {

            if (mContext.isDialogShow)
                return;
            DialogEditBinding binding = DialogEditBinding.inflate(LayoutInflater.from(mContext));

            binding.dialogUidGid.setVisibility(View.GONE);
            binding.dialogChid.setOnCheckedChangeListener((buttonView, isChecked) -> binding.dialogUidGid.setVisibility(isChecked ? View.VISIBLE : View.GONE));

            binding.dialogName.requestFocus();
            binding.dialogName.postDelayed(() -> ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(binding.dialogName, 0), 200);
            mContext.isDialogShow = true;
            new MaterialAlertDialogBuilder(mContext).setTitle(mContext.getString(R.string.dialog_edit)).setView(binding.getRoot()).setPositiveButton(mContext.getString(R.string.dialog_finish), (dialog, which) -> {
                if (!Runner.pingServer()) {
                    Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show();
                    return;
                }
                CommandInfo info = new CommandInfo();
                info.command = String.valueOf(binding.dialogCommand.getText());
                info.name = String.valueOf(binding.dialogName.getText());
                info.keepAlive = binding.dialogKeepItAlive.isChecked();
                info.useChid = binding.dialogChid.isChecked();
                info.ids = binding.dialogChid.isChecked() ? Objects.requireNonNull(binding.dialogIds.getText()).toString() : null;
                adapter.add(info);
            }).setOnDismissListener(dialog -> mContext.isDialogShow = false).show();
        });


        var recyclerView = binding.list;
        recyclerView.setAdapter(statusAdapter);
        RecyclerViewKt.fixEdgeEffect(recyclerView, true, true);
        RecyclerViewKt.addItemSpacing(recyclerView, 0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP);
        RecyclerViewKt.addEdgeSpacing(recyclerView, 16f, 4f, 16f, 4f, TypedValue.COMPLEX_UNIT_DIP);

        return root;
    }

    //初始化列表
    public void initList() {
//        if (!Runner.pingServer()) {
//            Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            Runner.iService.closeCursor();
//            int count = iService.count();
//            if (adapter == null) {
//                adapter = new CmdAdapter(mContext, count);
//                binding.recyclerView.setAdapter(adapter);
//            }
//            else adapter.updateData(count);
//        } catch (Exception e) {
//            throwableToDialog(mContext, e);
//        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        View.OnClickListener l = v -> recyclerView.smoothScrollToPosition(0);
        getToolbar().setOnClickListener(l);
        Runner.refreshStatus();
        statusAdapter.updateData();
    }

    //菜单选择事件
//    @SuppressLint("WrongConstant")
//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case CmdAdapter.long_click_copy_name:
//                if (!Runner.pingServer()) {
//                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
//                    return super.onContextItemSelected(item);
//                }
//                try {
//                    String name = iService.query(item.getGroupId()).name;
//                    ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", name));
//                    Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + name, Toast.LENGTH_SHORT).show();
//                } catch (RemoteException e) {
//                    throwableToDialog(mContext, e);
//                }
//                return true;
//            case CmdAdapter.long_click_copy_command:
//                if (!Runner.pingServer()) {
//                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
//                    return super.onContextItemSelected(item);
//                }
//                try {
//                    String command = iService.query(item.getGroupId()).command;
//                    ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", command));
//                    Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + command, Toast.LENGTH_SHORT).show();
//                } catch (RemoteException e) {
//                    throwableToDialog(mContext, e);
//                }
//                return true;
//            case CmdAdapter.long_click_new:
//
//                return true;
//            case CmdAdapter.long_click_pack:
//                Intent intent = new Intent(getContext(), PackActivity.class);
//                intent.putExtra("id", item.getGroupId());
//                startActivity(intent);
//                return true;
//            case CmdAdapter.long_click_del:
//                if (!App.pingServer()) {
//                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
//                    return super.onContextItemSelected(item);
//                }
//                adapter.remove(item.getGroupId());
//                return true;
//            default:
//                return super.onContextItemSelected(item);
//        }
//    }
}