package yangFenTuoZi.runner.plus.ui.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashSet;
import java.util.Set;

import rikka.widget.borderview.BorderRecyclerView;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.DialogAddBinding;
import yangFenTuoZi.runner.plus.databinding.FragmentAuthorizationBinding;

public class AuthorizationFragment extends BaseFragment {

    private FragmentAuthorizationBinding binding;
    public SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> new Handler().postDelayed(() -> {
        initList();
        binding.swipeRefreshLayout.setRefreshing(false);
    },1000);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAuthorizationBinding.inflate(inflater, container, false);
        binding.appBar.setLiftable(true);
        setupToolbar(binding.toolbar, binding.clickView, R.string.title_authorization);
        binding.toolbar.setNavigationIcon(null);

        BorderRecyclerView recyclerView = binding.recyclerView;
        View.OnClickListener l = v -> {
            binding.appBar.setExpanded(true, true);
            recyclerView.smoothScrollToPosition(0);
        };
        binding.toolbar.setOnClickListener(l);
        binding.clickView.setOnClickListener(l);

        binding.swipeRefreshLayout.setOnRefreshListener(onRefreshListener);

        binding.add.setOnClickListener(item -> {
            DialogAddBinding binding = DialogAddBinding.inflate(LayoutInflater.from(mContext));
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.menu_add)
                    .setView(binding.getRoot())
                    .setNegativeButton(R.string.dialog_finish, (v, i) -> {
                        Editable editable = binding.dialogUid.getText();
                        if (editable == null || editable.toString().replaceAll(" ", "").isEmpty())
                            return;
                        SharedPreferences sharedPreferences = mContext.getSharedPreferences("data", 0);
                        Set<String> allow_apps = new HashSet<>(sharedPreferences.getStringSet("allow_apps", new HashSet<>()));
                        allow_apps.add(String.valueOf(Integer.parseInt(editable.toString())));
                        sharedPreferences.edit()
                                .putStringSet("allow_apps", allow_apps)
                                .apply();
                        initList();
                    })
                    .show();
        });
        return binding.getRoot();
    }

    public void initList() {

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