package yangFenTuoZi.runner.plus.ui.fragment.proc;

import static yangFenTuoZi.runner.plus.utils.ExceptionUtils.throwableToDialog;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import rikka.recyclerview.RecyclerViewKt;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.Runner;
import yangFenTuoZi.runner.plus.adapters.ProcAdapter;
import yangFenTuoZi.runner.plus.base.BaseFragment;
import yangFenTuoZi.runner.plus.databinding.FragmentProcBinding;

public class ProcFragment extends BaseFragment {

    private FragmentProcBinding binding;
    private RecyclerView recyclerView;
    public SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> new Handler().postDelayed(() -> {
        initList();
        binding.swipeRefreshLayout.setRefreshing(false);
    },1000);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProcBinding.inflate(inflater, container, false);
        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);

        binding.swipeRefreshLayout.setOnRefreshListener(onRefreshListener);

        binding.procKillAll.setOnClickListener(v -> {
            if (Runner.pingServer()) {
                try {
                    if (Objects.requireNonNull(binding.recyclerView.getAdapter()).getItemCount() == 0) {
                        Toast.makeText(mContext, R.string.process_there_are_no_running_processes, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NullPointerException ignored) {
                }
            } else {
                Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show();
                return;
            }
            if (mContext.isDialogShow)
                return;
            mContext.isDialogShow = true;
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.process_kill_all_processes)
                    .setPositiveButton(android.R.string.ok, ((dialog, which) -> new Thread(() -> {
                        if (Runner.pingServer()) {
                            try {
                                String[] strings = Runner.service.exec("busybox ps -A -o pid,args|grep RUNNER-proc:|grep -v grep").split("\n");
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
                                ProcAdapter.killPIDs(data);
                            } catch (RemoteException e) {
                                throwableToDialog(mContext, e);
                            }
                            initList();
                        } else {
                            runOnUiThread(() -> Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show());
                        }
                    }).start()))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setOnDismissListener(dialog -> mContext.isDialogShow = false)
                    .show();
        });
        return binding.getRoot();
    }

    public void initList() {
        new Thread(() -> {
            if (Runner.pingServer()) {
                try {
                    String[] strings = Runner.service.exec("busybox ps -A -o pid,args|grep RUNNER-proc:|grep -v grep").split("\n");
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
                    runOnUiThread(() -> binding.recyclerView.setAdapter(new ProcAdapter(mContext, data, data_name, this)));
                } catch (RemoteException e) {
                    throwableToDialog(mContext, e);
                }
            } else {
                runOnUiThread(() -> Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show());
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
        View.OnClickListener l = v -> recyclerView.smoothScrollToPosition(0);
        getToolbar().setOnClickListener(l);
        initList();
    }

    public FragmentProcBinding getBinding() {
        return binding;
    }
}