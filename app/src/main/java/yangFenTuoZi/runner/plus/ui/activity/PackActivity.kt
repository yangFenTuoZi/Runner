package yangFenTuoZi.runner.plus.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.base.BaseActivity;
import yangFenTuoZi.runner.plus.databinding.ActivityPackBinding;

public class PackActivity extends BaseActivity {
    private ActivityPackBinding binding;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.appBar.setLiftable(true);
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar;
        if ((actionBar = getSupportActionBar()) != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int id = getIntent().getIntExtra("id", -1);
        if (id == -1) {
            Toast.makeText(this, R.string.exec_finish, Toast.LENGTH_LONG).show();
            finish();
        }

        binding.packName.setText(getIntent().getStringExtra("name"));
        binding.packPackageName.setText("runner.app." + getIntent().getStringExtra("name"));
        binding.packVersionName.setText("1.0");
        binding.packVersionCode.setText("1");
    }

}