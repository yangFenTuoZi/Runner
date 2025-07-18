package yangfentuozi.runner.app.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import yangfentuozi.runner.R
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.databinding.ActivityPackBinding

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

        val id = intent.getIntExtra("id", -1)
        if (id == -1) {
            Toast.makeText(this, R.string.finish, Toast.LENGTH_LONG).show()
            finish()
        }

        binding!!.packName.setText(intent.getStringExtra("name"))
        binding!!.packPackageName.setText("runner.app." + intent.getStringExtra("name"))
        binding!!.packVersionName.setText("1.0")
        binding!!.packVersionCode.setText("1")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}