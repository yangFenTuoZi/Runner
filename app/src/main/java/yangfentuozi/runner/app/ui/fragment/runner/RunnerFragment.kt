package yangfentuozi.runner.app.ui.fragment.runner

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import yangfentuozi.runner.R
import yangfentuozi.runner.app.base.BaseDialogBuilder
import yangfentuozi.runner.app.base.BaseFragment
import yangfentuozi.runner.databinding.DialogEditBinding
import yangfentuozi.runner.databinding.FragmentRunnerBinding
import yangfentuozi.runner.shared.data.CommandInfo

class RunnerFragment : BaseFragment() {
    private lateinit var mBinding: FragmentRunnerBinding
    private lateinit var adapter: CommandAdapter
    val binding get() = mBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentRunnerBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CommandAdapter(mMainActivity, this)

        mBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mMainActivity)
            fixEdgeEffect()
            addItemSpacing(0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            addEdgeSpacing(16f, 4f, 16f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            adapter = this@RunnerFragment.adapter
        }

        mBinding.swipeRefreshLayout.setOnRefreshListener { adapter.updateData() }

        mBinding.add.setOnClickListener {
            if (mMainActivity.isDialogShowing) return@setOnClickListener
            showAddCommandDialog(-1)
        }
        adapter.updateData()
    }

    fun showAddCommandDialog(toPosition: Int) {
        val dialogBinding = DialogEditBinding.inflate(LayoutInflater.from(mMainActivity))

        dialogBinding.apply {
            targetPermParent.visibility = View.GONE
            reducePerm.setOnCheckedChangeListener { _, isChecked ->
                targetPermParent.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            name.requestFocus()
            name.postDelayed({
                (mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(name, InputMethodManager.SHOW_IMPLICIT)
            }, 200)
        }

        try {
            BaseDialogBuilder(mMainActivity)
                .setTitle(R.string.edit)
                .setView(dialogBinding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val newCommand = CommandInfo().apply {
                        command = dialogBinding.command.text.toString()
                        name = dialogBinding.name.text.toString()
                        keepAlive = dialogBinding.keepAlive.isChecked
                        reducePerm = dialogBinding.reducePerm.isChecked
                        targetPerm =
                            if (dialogBinding.reducePerm.isChecked) dialogBinding.targetPerm.text.toString() else null
                    }

                    if (toPosition == -1) {
                        adapter.add(newCommand)
                    } else {
                        adapter.addUnderOne(toPosition + 1, newCommand)
                    }
                }
                .show()
        } catch (_: BaseDialogBuilder.DialogShowingException) {
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return adapter.onContextItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        toolbar.setOnClickListener {
            mBinding.recyclerView.smoothScrollToPosition(0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.close()
    }
}