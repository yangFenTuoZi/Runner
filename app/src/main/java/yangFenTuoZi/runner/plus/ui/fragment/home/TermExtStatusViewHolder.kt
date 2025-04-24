package yangFenTuoZi.runner.plus.ui.fragment.home;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import rikka.recyclerview.BaseViewHolder;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.Runner;
import yangFenTuoZi.runner.plus.databinding.HomeItemContainerBinding;
import yangFenTuoZi.runner.plus.databinding.HomeTermExtStatusBinding;
import yangFenTuoZi.runner.plus.service.TermExtVersion;

public class TermExtStatusViewHolder extends BaseViewHolder<Object> {

    public static final Creator<Object> CREATOR = (inflater, parent) -> {
        HomeItemContainerBinding outer = HomeItemContainerBinding.inflate(inflater, parent, false);
        HomeTermExtStatusBinding inner = HomeTermExtStatusBinding.inflate(inflater, outer.getRoot(), true);
        return new TermExtStatusViewHolder(inner, outer.getRoot());
    };

    private HomeTermExtStatusBinding binding;
    private final TextView title;
    private final TextView summaryView;
    private final Button install;
    private final Button remove;

    public TermExtStatusViewHolder(HomeTermExtStatusBinding binding, View root) {
        super(root);
        this.binding = binding;
        this.title = binding.text1;
        this.summaryView = binding.text2;
        this.install = binding.button1;
        this.remove = binding.button2;
    }

    @Override
    public void onBind() {
        Context context = itemView.getContext();
        if (!Runner.pingServer())
            return;

        try {
            TermExtVersion version = Runner.service.getTermExtVersion();
            if (version == null || version.versionCode == -1) {
                // not install
                title.setText(R.string.term_ext_title);
                summaryView.setVisibility(View.GONE);
                install.setText(R.string.install);
                remove.setVisibility(View.GONE);
            } else {
                // installed
                title.setText(R.string.term_ext_title_installed);
                summaryView.setVisibility(View.VISIBLE);
                summaryView.setText(context.getString(R.string.term_ext_version, version.versionName, version.versionCode, version.abi));
                install.setText(R.string.reinstall);
                remove.setVisibility(View.VISIBLE);
            }
        } catch (RemoteException e) {
            Log.e("TermExtStatusViewHolder", "get term ext version error", e);
        }
    }
}