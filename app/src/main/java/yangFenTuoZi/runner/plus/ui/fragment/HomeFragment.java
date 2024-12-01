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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;

import rikka.material.app.LocaleDelegate;
import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.BuildConfig;
import yangFenTuoZi.runner.plus.adapters.CmdAdapter;
import yangFenTuoZi.runner.plus.cli.CmdInfo;
import yangFenTuoZi.runner.plus.databinding.DialogAboutBinding;
import yangFenTuoZi.runner.plus.databinding.DialogChooseStartServerBinding;
import yangFenTuoZi.runner.plus.receiver.OnServiceConnectListener;
import yangFenTuoZi.runner.plus.receiver.OnServiceDisconnectListener;
import yangFenTuoZi.runner.plus.ui.activity.PackActivity;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.FragmentHomeBinding;
import yangFenTuoZi.runner.plus.ui.dialog.BaseDialogBuilder;
import yangFenTuoZi.runner.plus.ui.dialog.BlurBehindDialogBuilder;
import yangFenTuoZi.runner.plus.ui.dialog.StartServerDialogBuilder;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
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
        binding.recyclerView.setAdapter(null);
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
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
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
            CmdInfo[] cmdInfos = iService.getAllCmds();
            int[] data = new int[cmdInfos.length + 1];
            int max = -1;
            for (int i = 0; i < cmdInfos.length; i++) {
                data[i] = cmdInfos[i].id;
                if (cmdInfos[i].id > max)
                    max = cmdInfos[i].id;
            }
            data[cmdInfos.length] = max + 1;
            binding.recyclerView.setAdapter(new CmdAdapter(mContext, data, this, cmdInfos));
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
                    String name = iService.getCmdByID(item.getGroupId()).name;
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
                    String command = iService.getCmdByID(item.getGroupId()).command;
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
                try {
                    iService.delete(item.getGroupId());
                } catch (RemoteException e) {
                    throwableToDialog(mContext, e);
                }
                binding.recyclerView.setAdapter(null);
                initList();
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