package yangfentuozi.runner.app.ui.fragment.runner

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import yangfentuozi.runner.R
import yangfentuozi.runner.app.base.BaseDialogBuilder
import yangfentuozi.runner.app.data.DataRepository
import yangfentuozi.runner.app.ui.activity.MainActivity
import yangfentuozi.runner.app.ui.dialog.ExecDialogBuilder
import yangfentuozi.runner.databinding.DialogEditBinding
import yangfentuozi.runner.databinding.HomeItemContainerBinding
import yangfentuozi.runner.databinding.ItemCmdBinding
import yangfentuozi.runner.shared.data.CommandInfo

class CommandAdapter(private val mContext: MainActivity, private val mFragment: RunnerFragment) :
    RecyclerView.Adapter<CommandAdapter.ViewHolder>() {
    private val dataRepository = DataRepository.Companion.getInstance(mContext)
    var commands: MutableList<CommandInfo> = mutableListOf()

    init {
        updateData()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData() {
        mFragment.binding.swipeRefreshLayout.isRefreshing = true
        commands = dataRepository.getAllCommands().toMutableList()
        notifyDataSetChanged()
        mFragment.binding.swipeRefreshLayout.isRefreshing = false
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
        dataRepository.deleteCommand(position)
        commands.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addUnderOne(position: Int, info: CommandInfo) {
        // This function is not directly supported by the new DAO, will implement later if needed.
        // For now, just add to the end.
        dataRepository.addCommand(info)
        commands.add(position, info)
        notifyItemInserted(position)
    }

    fun add(info: CommandInfo) {
        dataRepository.addCommand(info)
        commands.add(info)
        notifyItemInserted(commands.size - 1)
    }

    fun move(fromPosition: Int, toPosition: Int) {
        dataRepository.moveCommand(fromPosition, toPosition)
        val item = commands.removeAt(fromPosition)
        commands.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
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
            if (mContext.isDialogShowing) return@setOnClickListener
            try {
                ExecDialogBuilder(mContext, info).show()
            } catch (_: BaseDialogBuilder.DialogShowingException) {
            }
        }

        holder.mBindingOuter.root.setOnClickListener {
            if (mContext.isDialogShowing) return@setOnClickListener
            showEditDialog(holder, info, empty)
        }

        holder.mBindingInner.itemName.text = if (empty[1]) getItalicText("__NAME__") else info.name
        holder.mBindingInner.itemCommand.text =
            if (empty[2]) getItalicText("__CMD__") else info.command

        holder.mBindingOuter.root.setOnCreateContextMenuListener { menu, _, _ ->
            menu.add(position, LONG_CLICK_COPY_NAME, 0, R.string.copy_name)
            menu.add(position, LONG_CLICK_COPY_COMMAND, 0, R.string.copy_command)
            menu.add(position, LONG_CLICK_NEW, 0, R.string.create_below)
            menu.add(position, LONG_CLICK_DEL, 0, R.string.delete)
        }
    }

    private fun showEditDialog(holder: ViewHolder, info: CommandInfo, empty: BooleanArray) {
        val binding = DialogEditBinding.inflate(LayoutInflater.from(mContext))
        val position = holder.bindingAdapterPosition

        binding.apply {
            reducePerm.isChecked = info.reducePerm
            reducePerm.setOnCheckedChangeListener { _, isChecked ->
                targetPermParent.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            keepAlive.isChecked = info.keepAlive
            targetPermParent.visibility = if (info.reducePerm) View.VISIBLE else View.GONE
            name.setText(info.name)
            command.setText(info.command)
            targetPerm.setText(info.targetPerm)
            name.requestFocus()

            (mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(name, InputMethodManager.SHOW_IMPLICIT)
        }

        try {
            BaseDialogBuilder(mContext)
                .setTitle(R.string.edit)
                .setView(binding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val updatedInfo = CommandInfo().apply {
                        name = binding.name.text.toString()
                        command = binding.command.text.toString()
                        keepAlive = binding.keepAlive.isChecked
                        reducePerm = binding.reducePerm.isChecked
                        targetPerm =
                            if (binding.reducePerm.isChecked) binding.targetPerm.text.toString() else null
                    }

                    dataRepository.updateCommand(updatedInfo, position)

                    if (!empty[0] && isEmpty(updatedInfo)[0]) {
                        remove(position)
                    } else {
                        commands[position] = updatedInfo
                        notifyItemChanged(position)
                    }
                }
                .show()
        } catch (_: BaseDialogBuilder.DialogShowingException) {
        }

    }

    private fun getItalicText(text: String): CharSequence {
        return SpannableStringBuilder(text).apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun close() {
        // executorService is removed
    }

    companion object {
        const val LONG_CLICK_COPY_NAME = 0
        const val LONG_CLICK_COPY_COMMAND = 1
        const val LONG_CLICK_NEW = 2
        const val LONG_CLICK_DEL = 3

        fun isEmpty(info: CommandInfo): BooleanArray {
            val existC = info.command.isNullOrEmpty()
            val existN = info.name.isNullOrEmpty()
            return booleanArrayOf(existC && existN, existN, existC)
        }
    }

    fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            LONG_CLICK_COPY_NAME -> {
                val name = commands[item.groupId].name ?: return false
                (mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("c", name))
                Toast.makeText(mContext, mContext.getString(R.string.copied_info) + "\n" + name, Toast.LENGTH_SHORT).show()
                return true
            }
            LONG_CLICK_COPY_COMMAND -> {
                val command = commands[item.groupId].command ?: return false
                (mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("c", command))
                Toast.makeText(mContext, mContext.getString(R.string.copied_info) + "\n" + command, Toast.LENGTH_SHORT).show()
                return true
            }
            LONG_CLICK_NEW -> {
                mFragment.showAddCommandDialog(item.groupId)
                return true
            }
            LONG_CLICK_DEL -> {
                remove(item.groupId)
                return true
            }
        }
        return true
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
//        ItemTouchHelper(ItemTouchHelperCallback()).attachToRecyclerView(recyclerView)
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