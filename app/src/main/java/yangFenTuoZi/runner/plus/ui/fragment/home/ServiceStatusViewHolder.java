package yangFenTuoZi.runner.plus.ui.fragment.home;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import rikka.html.text.HtmlCompat;
import rikka.recyclerview.BaseViewHolder;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.Runner;
import yangFenTuoZi.runner.plus.databinding.HomeItemContainerBinding;
import yangFenTuoZi.runner.plus.databinding.HomeServiceStatusBinding;

public class ServiceStatusViewHolder extends BaseViewHolder<Object> {

    public static final Creator<Object> CREATOR = (inflater, parent) -> {
        HomeItemContainerBinding outer = HomeItemContainerBinding.inflate(inflater, parent, false);
        HomeServiceStatusBinding inner = HomeServiceStatusBinding.inflate(inflater, outer.getRoot(), true);
        return new ServiceStatusViewHolder(inner, outer.getRoot());
    };

    private final HomeServiceStatusBinding binding;
    private final TextView textView;
    private final TextView summaryView;
    private final ImageView iconView;

    public ServiceStatusViewHolder(HomeServiceStatusBinding binding, View root) {
        super(root);
        this.binding = binding;
        this.textView = binding.text1;
        this.summaryView = binding.text2;
        this.iconView = binding.icon;
    }

    @Override
    public void onBind() {
        Context context = itemView.getContext();
        boolean ok = Runner.pingServer();
        int version = Runner.serviceVersion;

        iconView.setImageDrawable(ContextCompat.getDrawable(context, ok ?
                R.drawable.ic_check_circle_outline_24 :
                R.drawable.ic_error_outline_24));

        String title;
        String summary;

        if (ok) {
            title = context.getString(R.string.home_status_service_is_running);
            summary = context.getString(R.string.home_status_service_version, version);
        } else {
            title = context.getString(R.string.home_status_service_not_running);
            summary = "";
        }

        textView.setText(HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE));
        summaryView.setText(HtmlCompat.fromHtml(summary, HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE));

        if (TextUtils.isEmpty(summaryView.getText())) {
            summaryView.setVisibility(View.GONE);
        } else {
            summaryView.setVisibility(View.VISIBLE);
        }
    }
}