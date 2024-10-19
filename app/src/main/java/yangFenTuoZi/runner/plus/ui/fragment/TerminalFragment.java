package yangFenTuoZi.runner.plus.ui.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import yangFenTuoZi.runner.plus.databinding.FragmentTerminalBinding;

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


    @Override
    public void onStart() {
        super.onStart();
        Window window = requireActivity().getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
}