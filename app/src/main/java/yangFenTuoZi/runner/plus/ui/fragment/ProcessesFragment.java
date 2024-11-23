package yangFenTuoZi.runner.plus.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.adapters.ProcessAdapter;
import yangFenTuoZi.runner.plus.databinding.FragmentProcessesBinding;

public class ProcessesFragment extends BaseFragment {

    private FragmentProcessesBinding binding;
    public SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> new Handler().postDelayed(() -> {
        initList();
        binding.swipeRefreshLayout.setRefreshing(false);
    },1000);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProcessesBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        binding.swipeRefreshLayout.setOnRefreshListener(onRefreshListener);

        setupToolbar(binding.toolbar, binding.clickView, R.string.title_processes);
        binding.toolbar.setNavigationIcon(null);
        binding.appBar.setLiftable(true);
        binding.procKillAll.setOnClickListener(v -> {
            if (App.pingServer()) {
                try {
                    if (Objects.requireNonNull(binding.recyclerView.getAdapter()).getItemCount() == 0) {
                        Toast.makeText(mContext, R.string.process_there_are_no_running_processes, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NullPointerException ignored) {
                }
            } else {
                Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                return;
            }
            if (mContext.isDialogShow)
                return;
            mContext.isDialogShow = true;
            new MaterialAlertDialogBuilder(mContext)
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
                            runOnUiThread(() -> Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show());
                        }
                    }).start()))
                    .setOnDismissListener(dialog -> mContext.isDialogShow = false)
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
                    runOnUiThread(() -> binding.recyclerView.setAdapter(new ProcessAdapter(mContext, data, data_name, this)));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            } else {
                runOnUiThread(() -> Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show());
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

    public FragmentProcessesBinding getBinding() {
        return binding;
    }
}