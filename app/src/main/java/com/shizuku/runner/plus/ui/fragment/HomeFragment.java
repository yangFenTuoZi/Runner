package com.shizuku.runner.plus.ui.fragment;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.shizuku.runner.plus.adapters.CmdAdapter;
import com.shizuku.runner.plus.ui.activity.MainActivity;
import com.shizuku.runner.plus.ui.activity.PackActivity;
import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rikka.core.util.ResourceUtils;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private SharedPreferences sp;
    private ListView listView;

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
        return root;
    }

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
                List<String> list = Arrays.asList(sp.getString("xxx", "").split(","));
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