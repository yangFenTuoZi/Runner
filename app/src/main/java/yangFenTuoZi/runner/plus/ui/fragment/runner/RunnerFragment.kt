package yangFenTuoZi.runner.plus.ui.fragment.runner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.recyclerview.fixEdgeEffect
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseFragment
import yangFenTuoZi.runner.plus.databinding.DialogEditBinding
import yangFenTuoZi.runner.plus.databinding.FragmentRunnerBinding
import yangFenTuoZi.runner.plus.service.data.CommandInfo
import yangFenTuoZi.runner.plus.ui.activity.PackActivity
import yangFenTuoZi.runner.plus.utils.ThrowableKT.toErrorDialog

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

        adapter = CommandAdapter(mContext)

        val itemTouchHelper = ItemTouchHelper(CommandItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mContext)
            fixEdgeEffect(false, true)
            adapter = this@RunnerFragment.adapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            Handler().postDelayed({
                initList()
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1000)
        }

        setupAddButton()
        initList()
    }

    private fun setupAddButton() {
        binding.add.setOnClickListener {
            if (mContext.isDialogShow) return@setOnClickListener
            showAddCommandDialog()
        }
    }

    private fun showAddCommandDialog() {
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

        mContext.isDialogShow = true
        MaterialAlertDialogBuilder(mContext)
            .setTitle(mContext.getString(R.string.dialog_edit))
            .setView(dialogBinding.root)
            .setPositiveButton(mContext.getString(R.string.dialog_finish)) { _, _ ->
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
                    ids = if (dialogBinding.dialogChid.isChecked) dialogBinding.dialogIds.text.toString() else null
                }

                adapter.add(newCommand)
            }
            .setOnDismissListener {
                mContext.isDialogShow = false
            }
            .show()
    }

    private fun initList() {
        if (!Runner.pingServer()) {
            Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show()
            return
        }
        adapter.updateData()
    }

    override fun onContextItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            CommandAdapter.LONG_CLICK_COPY_NAME -> {
                if (!Runner.pingServer()) {
                    Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show()
                    return false
                }
                try {
                    val name = adapter.commands[item.groupId].name ?: return false
                    (mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("c", name))
                    Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + name, Toast.LENGTH_SHORT).show()
                    return true
                } catch (e: Exception) {
                    e.toErrorDialog(mContext)
                }
            }
            CommandAdapter.LONG_CLICK_COPY_COMMAND -> {
                if (!Runner.pingServer()) {
                    Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show()
                    return false
                }
                try {
                    val command = adapter.commands[item.groupId].command ?: return false
                    (mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("c", command))
                    Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + command, Toast.LENGTH_SHORT).show()
                    return true
                } catch (e: Exception) {
                    e.toErrorDialog(mContext)
                }
            }
            CommandAdapter.LONG_CLICK_NEW -> {
                showAddCommandDialog()
                return true
            }
            CommandAdapter.LONG_CLICK_PACK -> {
                val intent = Intent(context, PackActivity::class.java)
                intent.putExtra("id", item.groupId)
                startActivity(intent)
                return true
            }
            CommandAdapter.LONG_CLICK_DEL -> {
                if (!Runner.pingServer()) {
                    Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show()
                    return false
                }
                adapter.remove(item.groupId)
                return true
            }
        }
        return super.onContextItemSelected(item)
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