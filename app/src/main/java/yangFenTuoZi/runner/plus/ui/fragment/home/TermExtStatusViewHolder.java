package yangFenTuoZi.runner.plus.ui.fragment.home;

import android.view.View;

import rikka.recyclerview.BaseViewHolder;
import yangFenTuoZi.runner.plus.Runner;
import yangFenTuoZi.runner.plus.databinding.HomeItemContainerBinding;
import yangFenTuoZi.runner.plus.databinding.HomeShizukuPermRequestBinding;

public class TermExtStatusViewHolder extends BaseViewHolder<Object> {

    public static final Creator<Object> CREATOR = (inflater, parent) -> {
        HomeItemContainerBinding outer = HomeItemContainerBinding.inflate(inflater, parent, false);
        HomeShizukuPermRequestBinding inner = HomeShizukuPermRequestBinding.inflate(inflater, outer.getRoot(), true);
        return new TermExtStatusViewHolder(inner, outer.getRoot());
    };

    public TermExtStatusViewHolder(HomeShizukuPermRequestBinding binding, View root) {
        super(root);
        binding.button1.setOnClickListener(v -> {
            Runner.requestPermission();
        });
    }
}