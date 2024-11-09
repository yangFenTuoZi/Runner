package yangFenTuoZi.runner.plus.ui.fragment;

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
import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.adapters.ProcessAdapter;
import yangFenTuoZi.runner.plus.databinding.FragmentProcessesBinding;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;

import rikka.core.util.ResourceUtils;

public class ProcessesFragment extends BaseFragment {

    private FragmentProcessesBinding binding;
    private ListView listView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProcessesBinding.inflate(inflater, container, false);
        listView = binding.procList;
        binding.procKillAll.setOnClickListener(v -> {
            if (App.pingServer()) {
                try {
                    if (listView.getAdapter().getCount() == 0) {
                        Toast.makeText(requireContext(), R.string.process_there_are_no_running_processes, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NullPointerException ignored) {
                }
            } else {
                Toast.makeText(requireContext(), R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                return;
            }
            if (((MainActivity) requireContext()).isDialogShow)
                return;
            ((MainActivity) requireContext()).isDialogShow = true;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.process_kill_all_processes)
                    .setPositiveButton(R.string.yes, ((dialog, which) -> new Thread(() -> {
                        if (App.pingServer()) {
                            try {
                                String[] strings = App.iService.exec("busybox ps -A -o pid,args|grep RUNNER-proc:|grep -v grep").split("\n");
                                int[] data = new int[strings.length];
                                int i = 0;
                                for (String proc : strings) {
                                    if (!proc.isEmpty()) {
                                        String[] pI = proc.replaceAll(" +", " ").trim().split(" ");
                                        if (pI[2].matches("^RUNNER-proc:.*")) {
                                            data[i] = Integer.parseInt(pI[0]);
                                            i++;
                                        }
                                    }
                                }
                                ProcessAdapter.killPIDs(data);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                            initList();
                        } else {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show());
                        }
                    }).start()))
                    .setOnDismissListener(dialog -> ((MainActivity) requireContext()).isDialogShow = false)
                    .show();
        });
        return binding.getRoot();
    }

    public void initList() {
        new Thread(() -> {
            if (App.pingServer()) {
                try {
                    String[] strings = App.iService.exec("busybox ps -A -o pid,args|grep RUNNER-proc:|grep -v grep").split("\n");
                    int[] data = new int[strings.length];
                    String[] data_name = new String[strings.length];
                    int i = 0;
                    for (String proc : strings) {
                        if (!proc.isEmpty()) {
                            String[] pI = proc.replaceAll(" +", " ").trim().split(" ");
                            if (pI[2].matches("^RUNNER-proc:.*")) {
                                data[i] = Integer.parseInt(pI[0]);
                                data_name[i] = pI[2].split(":", 2)[1];
                                i++;
                            }
                        }
                    }
                    requireActivity().runOnUiThread(() -> listView.setAdapter(new ProcessAdapter((MainActivity) requireContext(), data, data_name, this)));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            } else {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show());
            }
        }).start();
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
    }
}