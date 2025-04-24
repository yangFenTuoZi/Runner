package yangFenTuoZi.runner.plus.ui.fragment.terminal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import yangFenTuoZi.runner.plus.base.BaseFragment;
import yangFenTuoZi.runner.plus.databinding.FragmentTerminalBinding;

public class TerminalFragment extends BaseFragment {

    private FragmentTerminalBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTerminalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
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
    }
}