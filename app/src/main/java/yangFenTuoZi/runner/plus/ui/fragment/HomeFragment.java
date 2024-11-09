package yangFenTuoZi.runner.plus.ui.fragment;

import static yangFenTuoZi.runner.plus.ui.activity.MainActivity.sendSomethingToServerBySocket;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.adapters.CmdAdapter;
import yangFenTuoZi.runner.plus.cli.CmdInfo;
import yangFenTuoZi.runner.plus.receiver.OnServiceConnectListener;
import yangFenTuoZi.runner.plus.receiver.OnServiceDisconnectListener;
import yangFenTuoZi.runner.plus.server.IService;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.ui.activity.PackActivity;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.FragmentHomeBinding;
import yangFenTuoZi.runner.plus.ui.dialog.StartServerDialog;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import rikka.core.util.ResourceUtils;

public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
    private OnServiceConnectListener onServiceConnectListener = new OnServiceConnectListener() {
        @Override
        public void onServiceConnect(IService iService) {
            requireActivity().runOnUiThread(() -> initList());
        }
    };
    private OnServiceDisconnectListener onServiceDisconnectListener = new OnServiceDisconnectListener() {
        @Override
        public void onServiceDisconnect() {
            requireActivity().runOnUiThread(() -> listView.setAdapter(null));
        }
    };
    public ListView listView;

    public FragmentHomeBinding getBinding() {
        return binding;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        App.addOnServiceConnectListener(onServiceConnectListener);
        App.addOnServiceDisconnectListener(onServiceDisconnectListener);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        listView = binding.list;
        ((MainActivity) requireContext()).setHomeFragment(this);
        setupToolbar(binding.toolbar, null, R.string.app_name, R.menu.menu_home);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_start_server) {
                AtomicInteger a = new AtomicInteger();
                @SuppressLint("WrongConstant") AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.choose_start_server)
                        .setView(R.layout.dialog_choose_start_server)
                        .setNegativeButton(R.string.start, (dialog, which) -> {
                            if (a.get() == 2) {
                                String command = "sh " + requireActivity().getExternalFilesDir("") + "/server_starter.sh";
                                AlertDialog alertDialog1 = new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.exec_start_server_command)
                                        .setMessage(command)
                                        .setNegativeButton(R.string.long_click_copy_command, (dialog1, which1) -> {
                                            ((ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", command));
                                            Toast.makeText(requireContext(), requireContext().getString(R.string.home_copy_command) + "\n" + command, Toast.LENGTH_SHORT).show();
                                        })
                                        .show();
                                try {
                                    Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
                                    mAlert.setAccessible(true);
                                    Object mAlertController = mAlert.get(alertDialog1);
                                    Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
                                    mMessage.setAccessible(true);
                                    TextView mMessageView = (TextView) mMessage.get(mAlertController);
                                    mMessageView.setTextIsSelectable(true);
                                } catch (Exception ignored) {
                                }
                            } else
                                new StartServerDialog(requireActivity(), a.get()).show();
                        })
                        .show();
                alertDialog.findViewById(R.id.root).setOnClickListener(v -> {
                    if (((MaterialRadioButton) v).isChecked())
                        a.set(0);
                });
                alertDialog.findViewById(R.id.shizuku).setOnClickListener(v -> {
                    if (((MaterialRadioButton) v).isChecked())
                        a.set(1);
                });
                alertDialog.findViewById(R.id.adb).setOnClickListener(v -> {
                    if (((MaterialRadioButton) v).isChecked())
                        a.set(2);
                });
            } else if (item.getItemId() == R.id.menu_stop_server) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.warning)
                        .setMessage(R.string.confirm_stop_server)
                        .setNegativeButton(R.string.yes, (dialog, which) -> new Thread(() -> {
                            try {
                                sendSomethingToServerBySocket("stopServer");
                            } catch (Exception e) {
                                requireActivity().runOnUiThread(() -> new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.error)
                                        .setMessage(getString(R.string.can_not_stop_server) + "\n" + e.getMessage())
                                        .show());
                            }
                        }).start())
                        .show();
                return true;
            }
            return true;
        });
        return root;
    }

    //初始化列表
    public void initList() {
        if (!App.pingServer()) {
            Toast.makeText(requireContext(), R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            CmdInfo[] cmdInfos = App.iService.getAllCmds();
            int[] data = new int[cmdInfos.length + 1];
            int max = -1;
            for (int i = 0; i < cmdInfos.length; i++) {
                data[i] = cmdInfos[i].id;
                if (cmdInfos[i].id > max)
                    max = cmdInfos[i].id;
            }
            data[cmdInfos.length] = max + 1;
            Log.d("dsfdsfdsfds", Arrays.toString(data));
            listView.setAdapter(new CmdAdapter(requireContext(), data, this, cmdInfos));
        } catch (RemoteException ignored) {
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

        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.isHome = true;
        if (mainActivity.serviceState) {
            binding.toolbar.setSubtitle(R.string.home_service_is_running);
        } else {
            binding.toolbar.setSubtitle(R.string.home_service_is_not_running);
        }
        if (App.pingServer())
            initList();
    }

    @Override
    public void onStop() {
        super.onStop();
        ((MainActivity) requireActivity()).isHome = false;
    }

    //菜单选择事件
    @SuppressLint("WrongConstant")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CmdAdapter.long_click_copy_name:
                if (!App.pingServer()) {
                    Toast.makeText(requireContext(), R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                    return super.onContextItemSelected(item);
                }
                try {
                    String name = App.iService.getCmdByID(item.getGroupId()).name;
                    ((ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", name));
                    Toast.makeText(requireContext(), requireContext().getString(R.string.home_copy_command) + "\n" + name, Toast.LENGTH_SHORT).show();
                } catch (RemoteException ignored) {
                }
                return true;
            case CmdAdapter.long_click_copy_command:
                if (!App.pingServer()) {
                    Toast.makeText(requireContext(), R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                    return super.onContextItemSelected(item);
                }
                try {
                    String command = App.iService.getCmdByID(item.getGroupId()).command;
                    ((ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", command));
                    Toast.makeText(requireContext(), requireContext().getString(R.string.home_copy_command) + "\n" + command, Toast.LENGTH_SHORT).show();
                } catch (RemoteException ignored) {
                }
                return true;
            case CmdAdapter.long_click_new:

                return true;
            case CmdAdapter.long_click_pack:
                Intent intent = new Intent(getContext(), PackActivity.class);
                intent.putExtra("id", item.getGroupId());
                requireContext().startActivity(intent);
                return true;
            case CmdAdapter.long_click_del:
                if (!App.pingServer()) {
                    Toast.makeText(requireContext(), R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                    return super.onContextItemSelected(item);
                }
                try {
                    App.iService.delete(item.getGroupId());
                } catch (RemoteException ignored) {
                }
                listView.setAdapter(null);
                initList();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}