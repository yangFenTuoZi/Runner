package yangFenTuoZi.runner.plus.ui.fragment.runner

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder
import yangFenTuoZi.runner.plus.base.BaseFragment
import yangFenTuoZi.runner.plus.databinding.DialogEditBinding
import yangFenTuoZi.runner.plus.databinding.FragmentRunnerBinding
import yangFenTuoZi.runner.plus.service.data.CommandInfo

class RunnerFragment : BaseFragment() {
    private var _binding: FragmentRunnerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CommandAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunnerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CommandAdapter(mContext, this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mContext)
            fixEdgeEffect(true, true)
            addItemSpacing(0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            addEdgeSpacing(16f, 4f, 16f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            adapter = this@RunnerFragment.adapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            Handler().postDelayed({
                initList()
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1000)
        }

        binding.add.setOnClickListener {
            if (mContext.isDialogShow) return@setOnClickListener
            showAddCommandDialog(-1)
        }
        initList()
    }

    fun showAddCommandDialog(toPosition: Int) {
        val dialogBinding = DialogEditBinding.inflate(LayoutInflater.from(mContext))

        dialogBinding.apply {
            dialogUidGid.visibility = View.GONE
            dialogChid.setOnCheckedChangeListener { _, isChecked ->
                dialogUidGid.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            dialogName.requestFocus()
            dialogName.postDelayed({
                (mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(dialogName, InputMethodManager.SHOW_IMPLICIT)
            }, 200)
        }

        try {
            BaseDialogBuilder(mContext)
                .setTitle(R.string.dialog_edit)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.dialog_finish) { _, _ ->
                    if (!Runner.pingServer()) {
                        Toast.makeText(
                            mContext,
                            R.string.home_status_service_not_running,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setPositiveButton
                    }

                    val newCommand = CommandInfo().apply {
                        command = dialogBinding.dialogCommand.text.toString()
                        name = dialogBinding.dialogName.text.toString()
                        keepAlive = dialogBinding.dialogKeepItAlive.isChecked
                        useChid = dialogBinding.dialogChid.isChecked
                        ids =
                            if (dialogBinding.dialogChid.isChecked) dialogBinding.dialogIds.text.toString() else null
                    }

                    if (toPosition == -1)
                        adapter.add(newCommand)
                    else
                        adapter.addUnderOne(toPosition + 1, newCommand)
                }
                .show()
        } catch (_: BaseDialogBuilder.DialogShowException) {
        }
    }

    private fun initList() {
        if (!Runner.pingServer()) {
            Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show()
            return
        }
        adapter.updateData()
    }

    override fun onContextItemSelected(item: android.view.MenuItem): Boolean {
        return adapter.onContextItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        getToolbar().setOnClickListener {
            binding.recyclerView.smoothScrollToPosition(0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.close()
        _binding = null
    }
}