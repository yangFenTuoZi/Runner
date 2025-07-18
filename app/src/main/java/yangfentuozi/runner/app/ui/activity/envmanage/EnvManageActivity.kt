package yangfentuozi.runner.app.ui.activity.envmanage

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.app.base.BaseDialogBuilder
import yangfentuozi.runner.databinding.ActivityEnvManageBinding
import yangfentuozi.runner.databinding.DialogEditEnvBinding
import yangfentuozi.runner.shared.data.EnvInfo

class EnvManageActivity : BaseActivity() {
    private lateinit var mBinding: ActivityEnvManageBinding
    private val mAdapter: EnvAdapter = EnvAdapter(this)
    val binding get() = mBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityEnvManageBinding.inflate(layoutInflater)
        setContentView(mBinding.getRoot())

        mBinding.recyclerView.apply {
            adapter = mAdapter
            fixEdgeEffect(true, true)
            addItemSpacing(top = 4f, bottom = 4f, unit = TypedValue.COMPLEX_UNIT_DIP)
            addEdgeSpacing(
                top = 4f,
                bottom = 4f,
                left = 16f,
                right = 16f,
                unit = TypedValue.COMPLEX_UNIT_DIP
            )
        }
        mBinding.appBar.setLiftable(true)
        setSupportActionBar(mBinding.toolbar)
        val actionBar: ActionBar?
        if ((supportActionBar.also {
                actionBar = it
            }) != null) actionBar!!.setDisplayHomeAsUpEnabled(true)

        mBinding.add.setOnClickListener {
            showAddDialog()
        }

        mBinding.toolbar.setOnClickListener { v: View? ->
            mBinding.recyclerView.smoothScrollToPosition(
                0
            )
        }
    }

    fun showAddDialog() {
        val dialogBinding = DialogEditEnvBinding.inflate(LayoutInflater.from(this))

        try {
            BaseDialogBuilder(this)
                .setTitle(R.string.edit)
                .setView(dialogBinding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    mAdapter.add(EnvInfo().apply {
                        key = dialogBinding.key.text.toString()
                        value = dialogBinding.value.text.toString()
                    })
                }
                .show()
        } catch (_: BaseDialogBuilder.DialogShowingException) {
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return mAdapter.onContextItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        Runner.refreshStatus()
        mAdapter.updateData()
    }
}