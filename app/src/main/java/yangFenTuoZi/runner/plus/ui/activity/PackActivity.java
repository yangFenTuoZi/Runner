package yangFenTuoZi.runner.plus.ui.activity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.ActivityPackBinding;

public class PackActivity extends BaseActivity {
    private ActivityPackBinding binding;
    private SharedPreferences sp;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        sp = getSharedPreferences(String.valueOf(id), 0);
        binding.packName.setText(sp.getString("name", ""));
        binding.packPackageName.setText("runner.app." + sp.getString("name", ""));
        binding.packVersionName.setText("1.0");
        binding.packVersionCode.setText("1");
    }
}