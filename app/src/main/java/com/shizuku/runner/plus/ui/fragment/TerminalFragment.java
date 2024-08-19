package com.shizuku.runner.plus.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.shizuku.runner.plus.databinding.FragmentTerminalBinding;

public class TerminalFragment extends Fragment {

    private FragmentTerminalBinding binding;
    private Context mContext;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTerminalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mContext = getContext();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}