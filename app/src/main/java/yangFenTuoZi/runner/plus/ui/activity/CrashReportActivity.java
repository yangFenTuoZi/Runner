package yangFenTuoZi.runner.plus.ui.activity;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.ActivityCrashReportBinding;
import yangFenTuoZi.runner.plus.utils.ExceptionUtils;

public class CrashReportActivity extends BaseActivity {

    private ActivityCrashReportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.appBar.setLiftable(true);
        setupToolbar(binding.toolbar, null, R.string.app_crashed);
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String crashFile = getIntent().getStringExtra("crash_file"), crashInfo = getIntent().getStringExtra("crash_info");

        binding.crashFile.setText(crashFile);

        TextView crashInfoTextView = binding.crashInfo;
        crashInfoTextView.append(getBoldText("VERSION.RELEASE: "));
        crashInfoTextView.append(Build.VERSION.RELEASE);
        crashInfoTextView.append("\n");

        crashInfoTextView.append(getBoldText("VERSION.SDK_INT: "));
        crashInfoTextView.append(String.valueOf(Build.VERSION.SDK_INT));
        crashInfoTextView.append("\n");

        crashInfoTextView.append(getBoldText("BUILD_TYPE: "));
        crashInfoTextView.append(Build.TYPE);
        crashInfoTextView.append("\n");

        crashInfoTextView.append(getBoldText("CPU_ABI: "));
        crashInfoTextView.append(SystemProperties.get("ro.product.cpu.abi"));
        crashInfoTextView.append("\n");

        crashInfoTextView.append(getBoldText("CPU_SUPPORTED_ABIS: "));
        crashInfoTextView.append(Arrays.toString(Build.SUPPORTED_ABIS));
        crashInfoTextView.append("\n\n" + crashInfo);

        try {
            FileOutputStream out = new FileOutputStream(crashFile);
            out.write(crashInfoTextView.getText().toString().getBytes());
            out.close();
        } catch (IOException e) {
            ExceptionUtils.throwableToDialog(this, e);
        }
    }

    private CharSequence getBoldText(String text) {
        SpannableString span = new SpannableString(text);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mApp.finishApp();
    }
}
