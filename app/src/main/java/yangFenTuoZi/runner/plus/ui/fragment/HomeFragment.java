package yangFenTuoZi.runner.plus.ui.fragment;

import static yangFenTuoZi.runner.plus.App.iService;
import static yangFenTuoZi.runner.plus.ui.activity.MainActivity.sendSomethingToServerBySocket;
import static yangFenTuoZi.runner.plus.utils.ExceptionUtils.throwableToDialog;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import rikka.material.app.LocaleDelegate;
import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.BuildConfig;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.adapters.CmdAdapter;
import yangFenTuoZi.runner.plus.cli.CmdInfo;
import yangFenTuoZi.runner.plus.databinding.DialogAboutBinding;
import yangFenTuoZi.runner.plus.databinding.DialogChooseStartServerBinding;
import yangFenTuoZi.runner.plus.databinding.DialogEditBinding;
import yangFenTuoZi.runner.plus.databinding.FragmentHomeBinding;
import yangFenTuoZi.runner.plus.receiver.OnServiceConnectListener;
import yangFenTuoZi.runner.plus.receiver.OnServiceDisconnectListener;
import yangFenTuoZi.runner.plus.ui.activity.PackActivity;
import yangFenTuoZi.runner.plus.ui.dialog.BaseDialogBuilder;
import yangFenTuoZi.runner.plus.ui.dialog.BlurBehindDialogBuilder;
import yangFenTuoZi.runner.plus.ui.dialog.StartServerDialogBuilder;

public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
    private CmdAdapter adapter;
    private final OnServiceConnectListener onServiceConnectListener = iService -> runOnUiThread(() -> {
        initList();
        try {
            String[] ver = iService.version();
            String string = getString(R.string.home_service_version, ver[0], Integer.parseInt(ver[1]));
            binding.toolbarLayout.setSubtitle(string);
            binding.toolbar.setSubtitle(string);
        } catch (Exception ignored) {
        }
    });
    private final OnServiceDisconnectListener onServiceDisconnectListener = () -> runOnUiThread(() -> {
        if (adapter != null) adapter.close();
        binding.recyclerView.setAdapter(adapter = null);
        String string = getString(R.string.home_service_is_not_running);
        binding.toolbarLayout.setSubtitle(string);
        binding.toolbar.setSubtitle(string);
    });

    public FragmentHomeBinding getBinding() {
        return binding;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.recyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        binding.swipeRefreshLayout.setOnRefreshListener(() -> new Handler().postDelayed(() -> {
            initList();
            binding.swipeRefreshLayout.setRefreshing(false);
        }, 1000));

        setupToolbar(binding.toolbar, binding.clickView, R.string.app_name, R.menu.menu_home);
        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setOnClickListener(v -> showAbout());
        binding.clickView.setOnClickListener(v -> showAbout());
        binding.appBar.setLiftable(true);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_start_server) {
                AtomicInteger a = new AtomicInteger();
                DialogChooseStartServerBinding startServerBinding = DialogChooseStartServerBinding.inflate(LayoutInflater.from(mContext));
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.choose_start_server)
                        .setView(startServerBinding.getRoot())
                        .setNegativeButton(R.string.start, (dialog, which) -> {
                            if (a.get() == 2) {
                                String command = "sh " + mContext.getExternalFilesDir("") + "/server_starter.sh";
                                @SuppressLint("WrongConstant") AlertDialog alertDialog1 = new MaterialAlertDialogBuilder(mContext)
                                        .setTitle(R.string.exec_start_server_command)
                                        .setMessage(command)
                                        .setNegativeButton(R.string.long_click_copy_command, (dialog1, which1) -> {
                                            ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", command));
                                            Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + command, Toast.LENGTH_SHORT).show();
                                        })
                                        .show();
                                try {
                                    Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
                                    mAlert.setAccessible(true);
                                    Object mAlertController = mAlert.get(alertDialog1);
                                    assert mAlertController != null;
                                    Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
                                    mMessage.setAccessible(true);
                                    TextView mMessageView = (TextView) mMessage.get(mAlertController);
                                    assert mMessageView != null;
                                    mMessageView.setTextIsSelectable(true);
                                } catch (Exception ignored) {
                                }
                            } else {
                                try {
                                    new StartServerDialogBuilder(mContext, a.get()).show();
                                } catch (BaseDialogBuilder.DialogShowException ignored) {
                                }
                            }
                        })
                        .show();
                startServerBinding.root.setOnClickListener(v -> {
                    if (((MaterialRadioButton) v).isChecked())
                        a.set(0);
                });
                startServerBinding.shizuku.setOnClickListener(v -> {
                    if (((MaterialRadioButton) v).isChecked())
                        a.set(1);
                });
                startServerBinding.adb.setOnClickListener(v -> {
                    if (((MaterialRadioButton) v).isChecked())
                        a.set(2);
                });
            } else if (item.getItemId() == R.id.menu_stop_server) {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.confirm_stop_server)
                        .setNegativeButton(R.string.yes, (dialog, which) -> new Thread(() -> {
                            try {
                                sendSomethingToServerBySocket("stopServer");
                            } catch (Exception e) {
                                throwableToDialog(mContext, e);
                            }
                        }).start())
                        .show();
                return true;
            }
            return true;
        });
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
                if (!App.pingServer()) {
                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                    return;
                }
                CmdInfo info = new CmdInfo();
                info.command = String.valueOf(binding.dialogCommand.getText());
                info.name = String.valueOf(binding.dialogName.getText());
                info.keepAlive = binding.dialogKeepItAlive.isChecked();
                info.useChid = binding.dialogChid.isChecked();
                info.ids = binding.dialogChid.isChecked() ? Objects.requireNonNull(binding.dialogIds.getText()).toString() : null;
                adapter.add(info);
            }).setOnDismissListener(dialog -> mContext.isDialogShow = false).show();
        });

        App.addOnServiceConnectListener(onServiceConnectListener);
        App.addOnServiceDisconnectListener(onServiceDisconnectListener);
        if (App.pingServer())
            onServiceConnectListener.onServiceConnect(iService);
        else
            onServiceDisconnectListener.onServiceDisconnect();
        return root;
    }

    //初始化列表
    public void initList() {
        if (!App.pingServer()) {
            Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            App.iService.closeCursor();
            int count = iService.count();
            if (adapter == null) {
                adapter = new CmdAdapter(mContext, count);
                binding.recyclerView.setAdapter(adapter);
            }
            else adapter.updateData(count);
        } catch (Exception e) {
            throwableToDialog(mContext, e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        App.removeOnServiceConnectListener(onServiceConnectListener);
        App.removeOnServiceDisconnectListener(onServiceDisconnectListener);
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (App.pingServer()) onServiceConnectListener.onServiceConnect(iService);
        else onServiceDisconnectListener.onServiceDisconnect();
    }

    //菜单选择事件
    @SuppressLint("WrongConstant")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CmdAdapter.long_click_copy_name:
                if (!App.pingServer()) {
                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                    return super.onContextItemSelected(item);
                }
                try {
                    String name = iService.query(item.getGroupId()).name;
                    ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", name));
                    Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + name, Toast.LENGTH_SHORT).show();
                } catch (RemoteException e) {
                    throwableToDialog(mContext, e);
                }
                return true;
            case CmdAdapter.long_click_copy_command:
                if (!App.pingServer()) {
                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                    return super.onContextItemSelected(item);
                }
                try {
                    String command = iService.query(item.getGroupId()).command;
                    ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", command));
                    Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + command, Toast.LENGTH_SHORT).show();
                } catch (RemoteException e) {
                    throwableToDialog(mContext, e);
                }
                return true;
            case CmdAdapter.long_click_new:

                return true;
            case CmdAdapter.long_click_pack:
                Intent intent = new Intent(getContext(), PackActivity.class);
                intent.putExtra("id", item.getGroupId());
                startActivity(intent);
                return true;
            case CmdAdapter.long_click_del:
                if (!App.pingServer()) {
                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                    return super.onContextItemSelected(item);
                }
                adapter.remove(item.getGroupId());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showAbout() {
        DialogAboutBinding binding = DialogAboutBinding.inflate(getLayoutInflater(), null, false);
        binding.designAboutTitle.setText(R.string.app_name);
        binding.designAboutInfo.setMovementMethod(LinkMovementMethod.getInstance());
        binding.designAboutInfo.setText(HtmlCompat.fromHtml(getString(
                R.string.about_view_source_code,
                "<b><a href=\"https://github.com/yangFenTuoZi/Runner\">GitHub</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
        binding.designAboutVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        try {
            new BlurBehindDialogBuilder(mContext)
                    .setView(binding.getRoot()).show();
        } catch (BaseDialogBuilder.DialogShowException ignored) {
        }
    }
}