package yangfentuozi.runner.ui.fragment.runner

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.RemoteException
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseDialogBuilder
import yangfentuozi.runner.databinding.DialogEditBinding
import yangfentuozi.runner.databinding.HomeItemContainerBinding
import yangfentuozi.runner.databinding.ItemCmdBinding
import yangfentuozi.runner.service.data.CommandInfo
import yangfentuozi.runner.ui.activity.MainActivity
import yangfentuozi.runner.ui.activity.PackActivity
import yangfentuozi.runner.ui.dialog.ExecDialogBuilder
import yangfentuozi.runner.util.ThrowableUtil.toErrorDialog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CommandAdapter(private val mContext: MainActivity, private val mFragment: RunnerFragment) :
    RecyclerView.Adapter<CommandAdapter.ViewHolder>() {
    var commands: Array<CommandInfo> = emptyArray()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        updateData()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData() {
        executorService.execute {
            try {
                val newCommands = Runner.service?.readAll() ?: emptyArray()
                mContext.runOnUiThread {
                    commands = newCommands
                    notifyDataSetChanged()
                }
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val outer =
            HomeItemContainerBinding.inflate(LayoutInflater.from(parent.context)!!, parent, false)
        val inner =
            ItemCmdBinding.inflate(LayoutInflater.from(parent.context), outer.getRoot(), true)
        return ViewHolder(inner, outer)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = commands.getOrNull(position) ?: return
        init(holder, info)
    }

    override fun getItemCount(): Int = commands.size

    fun remove(position: Int) {
        executorService.execute {
            try {
                Runner.service?.delete(position)
                mContext.runOnUiThread {
                    commands = commands.toMutableList().apply {
                        removeAt(position)
                    }.toTypedArray()
                    notifyItemRemoved(position)
                }
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
    }

    fun addUnderOne(position: Int, info: CommandInfo) {
        executorService.execute {
            try {
                Runner.service?.insertInto(info, position)
                mContext.runOnUiThread {
                    commands = commands.toMutableList().apply { add(position, info) }.toTypedArray()
                    notifyItemInserted(position)
                }
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
    }

    fun add(info: CommandInfo) {
        executorService.execute {
            try {
                Runner.service?.insert(info)
                mContext.runOnUiThread {
                    commands = commands.toMutableList().apply { add(info) }.toTypedArray()
                    notifyItemInserted(commands.size - 1)
                }
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
    }

    fun move(fromPosition: Int, toPosition: Int) {
        executorService.execute {
            try {
                Runner.service?.move(fromPosition, toPosition)
                mContext.runOnUiThread {
                    commands = commands.toMutableList().apply {
                        val item = removeAt(fromPosition)
                        add(toPosition, item)
                    }.toTypedArray()
                    notifyItemMoved(fromPosition, toPosition)
                }
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
    }

    class ViewHolder(bindingInner: ItemCmdBinding, bindingOuter: HomeItemContainerBinding) :
        RecyclerView.ViewHolder(bindingOuter.root) {
        val mBindingInner: ItemCmdBinding = bindingInner
        val mBindingOuter: HomeItemContainerBinding = bindingOuter
    }

    private fun init(holder: ViewHolder, info: CommandInfo) {
        val empty = isEmpty(info)
        val position = holder.bindingAdapterPosition

        holder.mBindingInner.itemButton.setOnClickListener {
            if (mContext.isDialogShow) return@setOnClickListener
            if (Runner.pingServer()) {
                try {
                    ExecDialogBuilder(mContext, info).show()
                } catch (_: BaseDialogBuilder.DialogShowException) {
                }
            } else {
                Toast.makeText(
                    mContext,
                    R.string.home_status_service_not_running,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        holder.mBindingOuter.root.setOnClickListener {
            if (mContext.isDialogShow) return@setOnClickListener
            showEditDialog(holder, info, empty)
        }

        holder.mBindingInner.itemName.text = if (empty[1]) getItalicText("__NAME__") else info.name
        holder.mBindingInner.itemCommand.text =
            if (empty[2]) getItalicText("__CMD__") else info.command

        holder.mBindingOuter.root.setOnCreateContextMenuListener { menu, _, _ ->
            menu.add(position, LONG_CLICK_COPY_NAME, 0, R.string.long_click_copy_name)
            menu.add(position, LONG_CLICK_COPY_COMMAND, 0, R.string.long_click_copy_command)
            menu.add(position, LONG_CLICK_NEW, 0, R.string.long_click_new)
            menu.add(position, LONG_CLICK_PACK, 0, R.string.long_click_pack)
            menu.add(position, LONG_CLICK_DEL, 0, R.string.long_click_del)
        }
    }

    private fun showEditDialog(holder: ViewHolder, info: CommandInfo, empty: BooleanArray) {
        val binding = DialogEditBinding.inflate(LayoutInflater.from(mContext))
        val position = holder.bindingAdapterPosition

        binding.apply {
            dialogChid.isChecked = info.useChid
            dialogChid.setOnCheckedChangeListener { _, isChecked ->
                dialogUidGid.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            dialogKeepItAlive.isChecked = info.keepAlive
            dialogUidGid.visibility = if (info.useChid) View.VISIBLE else View.GONE
            dialogName.setText(info.name)
            dialogCommand.setText(info.command)
            dialogIds.setText(info.ids)
            dialogName.requestFocus()

            (mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(dialogName, InputMethodManager.SHOW_IMPLICIT)
        }

        try {
            BaseDialogBuilder(mContext)
                .setTitle(R.string.dialog_edit)
                .setView(binding.root)
                .setPositiveButton(R.string.dialog_finish) { _, _ ->
                    if (!Runner.pingServer()) {
                        Toast.makeText(
                            mContext,
                            R.string.home_status_service_not_running,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setPositiveButton
                    }

                    val updatedInfo = CommandInfo().apply {
                        name = binding.dialogName.text.toString()
                        command = binding.dialogCommand.text.toString()
                        keepAlive = binding.dialogKeepItAlive.isChecked
                        useChid = binding.dialogChid.isChecked
                        ids =
                            if (binding.dialogChid.isChecked) binding.dialogIds.text.toString() else null
                    }

                    executorService.execute {
                        try {
                            Runner.service?.edit(updatedInfo, position)
                            mContext.runOnUiThread {
                                if (!empty[0] && isEmpty(updatedInfo)[0]) {
                                    remove(position)
                                } else {
                                    updateData()
                                }
                            }
                        } catch (e: RemoteException) {
                            e.toErrorDialog(mContext)
                        }
                    }
                }
                .show()
        } catch (_: BaseDialogBuilder.DialogShowException) {
        }

    }

    private fun getItalicText(text: String): CharSequence {
        return SpannableStringBuilder(text).apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun close() {
        executorService.shutdown()
    }

    companion object {
        const val LONG_CLICK_COPY_NAME = 0
        const val LONG_CLICK_COPY_COMMAND = 1
        const val LONG_CLICK_NEW = 2
        const val LONG_CLICK_PACK = 3
        const val LONG_CLICK_DEL = 4

        fun isEmpty(info: CommandInfo): BooleanArray {
            val existC = info.command.isNullOrEmpty()
            val existN = info.name.isNullOrEmpty()
            return booleanArrayOf(existC && existN, existN, existC)
        }
    }

    fun onContextItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            LONG_CLICK_COPY_NAME -> {
                if (!Runner.pingServer()) {
                    Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show()
                    return false
                }
                try {
                    val name = commands[item.groupId].name ?: return false
                    (mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("c", name))
                    Toast.makeText(mContext, mContext.getString(R.string.home_copy_command) + "\n" + name, Toast.LENGTH_SHORT).show()
                    return true
                } catch (e: Exception) {
                    e.toErrorDialog(mContext)
                }
            }
            LONG_CLICK_COPY_COMMAND -> {
                if (!Runner.pingServer()) {
                    Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show()
                    return false
                }
                try {
                    val command = commands[item.groupId].command ?: return false
                    (mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("c", command))
                    Toast.makeText(mContext, mContext.getString(R.string.home_copy_command) + "\n" + command, Toast.LENGTH_SHORT).show()
                    return true
                } catch (e: Exception) {
                    e.toErrorDialog(mContext)
                }
            }
            LONG_CLICK_NEW -> {
                mFragment.showAddCommandDialog(item.groupId)
                return true
            }
            LONG_CLICK_PACK -> {
                val intent = Intent(mContext, PackActivity::class.java)
                intent.putExtra("id", item.groupId)
                mContext.startActivity(intent)
                return true
            }
            LONG_CLICK_DEL -> {
                if (!Runner.pingServer()) {
                    Toast.makeText(mContext, R.string.home_status_service_not_running, Toast.LENGTH_SHORT).show()
                    return false
                }
                remove(item.groupId)
                return true
            }
        }
        return true
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        ItemTouchHelper(ItemTouchHelperCallback()).attachToRecyclerView(recyclerView)
    }

    private inner class ItemTouchHelperCallback() : ItemTouchHelper.Callback() {

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            // 允许上下拖动和左右滑动
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlags = 0
//            ItemTouchHelper.START or ItemTouchHelper.END
            return makeMovementFlags(dragFlags, swipeFlags)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            move(fromPosition, toPosition) // 调用适配器的移动方法
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // 不处理滑动删除
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true // 启用长按拖动
        }

    }
}