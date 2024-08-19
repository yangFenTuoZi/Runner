package com.shizuku.runner.plus.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.shizuku.runner.plus.adapters.ProcessAdapter;
import com.shizuku.runner.plus.databinding.FragmentProcessesBinding;

import java.io.File;

public class ProcessesFragment extends Fragment {

    private FragmentProcessesBinding binding;
    private ListView listView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProcessesBinding.inflate(inflater, container, false);
        listView = binding.procList;
        initList();
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
        listView.setAdapter(new ProcessAdapter(requireContext(), data, this));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}