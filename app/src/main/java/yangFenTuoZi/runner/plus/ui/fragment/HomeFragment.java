package yangFenTuoZi.runner.plus.ui.fragment;

import static yangFenTuoZi.runner.plus.ui.activity.MainActivity.sendSomethingToServerBySocket;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
import yangFenTuoZi.runner.plus.adapters.CmdAdapter;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.ui.activity.PackActivity;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.FragmentHomeBinding;
import yangFenTuoZi.runner.plus.ui.dialog.StartServerDialog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rikka.core.util.ResourceUtils;

public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
    private SharedPreferences sp;
    public ListView listView;

    public FragmentHomeBinding getBinding() {
        return binding;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        listView = binding.list;
        sp = requireContext().getSharedPreferences("data", 0);
        initList();
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
        String[] s;
        int[] data;
        if (sp.getString("data", "").isEmpty()) {
            data = new int[1];
            data[0] = 1;
        } else {
            s = sp.getString("data", "").split(",");
            data = new int[s.length + 1];
            for (int i = 0; i < s.length; i++)
                data[i] = Integer.parseInt(s[i]);
            data[s.length] = data[s.length - 1] + 1;
        }
        listView.setAdapter(new CmdAdapter(requireContext(), data, this));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        new Thread(() -> {
            try {
                Thread.sleep(100);
                sendSomethingToServerBySocket("sendBinderToApp");
            } catch (Exception ignored) {
            }
        }).start();
        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.isHome = true;
        if (mainActivity.serviceState) {
            binding.toolbar.setSubtitle(R.string.home_service_is_running);
        } else {
            binding.toolbar.setSubtitle(R.string.home_service_is_not_running);
        }
        initList();
        Window window = requireActivity().getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        if (ResourceUtils.isNightMode(getResources().getConfiguration())) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
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
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(String.valueOf(item.getGroupId()), 0);
        switch (item.getItemId()) {
            case CmdAdapter.long_click_copy_name:
                ((ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", sharedPreferences.getString("name", "")));
                Toast.makeText(requireContext(), requireContext().getString(R.string.home_copy_command) + "\n" + sharedPreferences.getString("name", ""), Toast.LENGTH_SHORT).show();
                return true;
            case CmdAdapter.long_click_copy_command:
                ((ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", sharedPreferences.getString("command", "")));
                Toast.makeText(requireContext(), requireContext().getString(R.string.home_copy_command) + "\n" + sharedPreferences.getString("command", ""), Toast.LENGTH_SHORT).show();
                return true;
            case CmdAdapter.long_click_new:

                return true;
            case CmdAdapter.long_click_pack:
                Intent intent = new Intent(getContext(), PackActivity.class);
                intent.putExtra("id", item.getGroupId());
                requireContext().startActivity(intent);
                return true;
            case CmdAdapter.long_click_del:
                List<String> list = Arrays.asList(sp.getString("data", "").split(","));
                if (list.contains(String.valueOf(item.getGroupId()))) {
                    List<String> arrayList = new ArrayList<>(list);
                    arrayList.remove(String.valueOf(item.getGroupId()));
                    sp.edit().putString("data", String.join(",", arrayList)).apply();
                }
                requireContext().deleteSharedPreferences(String.valueOf(item.getGroupId()));
                listView.setAdapter(null);
                initList();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}