package com.shizuku.runner.plus.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.adapters.ProcessAdapter;
import com.shizuku.runner.plus.databinding.FragmentProcessesBinding;
import com.shizuku.runner.plus.ui.activity.MainActivity;

import rikka.core.util.ResourceUtils;

public class ProcessesFragment extends Fragment {

    private FragmentProcessesBinding binding;
    private ListView listView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProcessesBinding.inflate(inflater, container, false);
        listView = binding.procList;
        binding.procKillAll.setOnClickListener(v -> {
            if (((MainActivity) requireContext()).iUserService != null) {
                try {
                    if (listView.getAdapter().getCount() == 0) {
                        Toast.makeText(requireContext(), R.string.process_there_are_no_running_processes, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NullPointerException ignored) {
                }
            } else {
                Toast.makeText(requireContext(), R.string.home_service_is_disconnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (((MainActivity) requireContext()).isDialogShow)
                return;
            ((MainActivity) requireContext()).isDialogShow = true;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.process_kill_all_processes)
                    .setPositiveButton(R.string.yes, ((dialog, which) -> {
                        if (((MainActivity) requireContext()).iUserService != null) {
                            try {
                                ((MainActivity) requireContext()).iUserService.exec("sh /data/local/tmp/$APP_PACKAGE_NAME/etc/profile k_a", requireContext().getApplicationInfo().packageName);
                                initList();
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Toast.makeText(requireContext(), R.string.home_service_is_disconnected, Toast.LENGTH_SHORT).show();
                        }
                    }))
                    .setOnDismissListener(dialog -> ((MainActivity) requireContext()).isDialogShow = false)
                    .show();
        });
        return binding.getRoot();
    }

    public void initList() {
        if (((MainActivity) requireContext()).iUserService != null) {
            try {
                String[] strings = ((MainActivity) requireContext()).iUserService.exec("busybox ps -A -o pid,args|grep RUNNER-bash:|grep -v grep", requireContext().getApplicationInfo().packageName).split("\n");
                int[] data = new int[strings.length];
                String[] data_name = new String[strings.length];
                int i = 0;
                for (String proc : strings) {
                    if (!proc.isEmpty()) {
                        String[] pI = proc.replaceAll(" +", " ").trim().split(" ");
                        if (pI[2].matches("^RUNNER-bash:.*")) {
                            data[i] = Integer.parseInt(pI[0]);
                            data_name[i] = pI[2].split(":", 2)[1];
                            i++;
                        }
                    }
                }
                listView.setAdapter(new ProcessAdapter((MainActivity) requireContext(), data, data_name, this));
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        } else {
            Toast.makeText(requireContext(), R.string.home_service_is_disconnected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        initList();
        Window window = requireActivity().getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        if (ResourceUtils.isNightMode(getResources().getConfiguration())) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }
}