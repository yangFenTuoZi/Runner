package yangFenTuoZi.runner.plus.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.base.BaseActivity
import yangFenTuoZi.runner.plus.databinding.ActivityPackBinding

class PackActivity : BaseActivity() {
    private var binding: ActivityPackBinding? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPackBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        binding!!.appBar.setLiftable(true)
        setSupportActionBar(binding!!.toolbar)
        val actionBar: ActionBar?
        if ((supportActionBar.also {
                actionBar = it
            }) != null) actionBar!!.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.main,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        val id = intent.getIntExtra("id", -1)
        if (id == -1) {
            Toast.makeText(this, R.string.exec_finish, Toast.LENGTH_LONG).show()
            finish()
        }

        binding!!.packName.setText(intent.getStringExtra("name"))
        binding!!.packPackageName.setText("runner.app." + intent.getStringExtra("name"))
        binding!!.packVersionName.setText("1.0")
        binding!!.packVersionCode.setText("1")
    }
}