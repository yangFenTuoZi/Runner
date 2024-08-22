package com.shizuku.runner.plus.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.shizuku.runner.plus.adapters.ProcessAdapter;
import com.shizuku.runner.plus.databinding.FragmentProcessesBinding;
import com.shizuku.runner.plus.ui.activity.MainActivity;

import java.io.File;

import rikka.core.util.ResourceUtils;

public class ProcessesFragment extends Fragment {

    private FragmentProcessesBinding binding;
    private ListView listView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProcessesBinding.inflate(inflater, container, false);
        listView = binding.procList;
        return binding.getRoot();
    }

    public void initList() {
        int[] data;
        String[] ls = new File(requireContext().getApplicationInfo().dataDir + "/shared_prefs").list();
        if (ls != null) {
            int[] s = new int[ls.length];
            int i = 0;
            for (String s1 : ls) {
                if (s1.matches("^proc_[1-9]\\d*.xml$")) {
                    s[i] = Integer.parseInt(s1.substring(5, s1.length()-4));
                    i++;
                }
            }
            data = new int[i];
            System.arraycopy(s, 0, data, 0, i);
        } else
            data = new int[0];
        listView.setAdapter(new ProcessAdapter((MainActivity) requireContext(), data, this));
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