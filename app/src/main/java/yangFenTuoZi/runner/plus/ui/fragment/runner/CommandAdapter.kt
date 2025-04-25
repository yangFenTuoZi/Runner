package yangFenTuoZi.runner.plus.ui.fragment.runner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.RemoteException
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder
import yangFenTuoZi.runner.plus.databinding.DialogEditBinding
import yangFenTuoZi.runner.plus.service.data.CommandInfo
import yangFenTuoZi.runner.plus.ui.activity.MainActivity
import yangFenTuoZi.runner.plus.ui.dialog.ExecDialogBuilder
import yangFenTuoZi.runner.plus.utils.ThrowableKT.toErrorDialog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CommandAdapter(private val mContext: MainActivity) :
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cmd, parent, false)
        return ViewHolder(view).apply {
            view.setOnKeyListener { _, _, _ -> false }
        }
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
//                updateData() // 刷新整个列表
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

    fun add(info: CommandInfo) {
        executorService.execute {
            try {
                Runner.service?.insert(info) // 添加到末尾
//                updateData() // 刷新整个列表
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
//                updateData() // 刷新整个列表
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.item_name)
        val textCommand: TextView = view.findViewById(R.id.item_command)
        val itemButton: MaterialButton = view.findViewById(R.id.item_button)
        val layout: MaterialCardView = view.findViewById(R.id.item_root)
    }

    private fun init(holder: ViewHolder, info: CommandInfo) {
        val empty = isEmpty(info)
        val position = holder.bindingAdapterPosition

        holder.itemButton.setOnClickListener {
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

        holder.layout.setOnClickListener {
            if (mContext.isDialogShow) return@setOnClickListener
            showEditDialog(holder, info, empty)
        }

        holder.textName.text = if (empty[1]) getItalicText("__NAME__") else info.name
        holder.textCommand.text = if (empty[2]) getItalicText("__CMD__") else info.command

//        holder.layout.setOnCreateContextMenuListener { menu, _, _ ->
//            menu.add(
//                position,
//                LONG_CLICK_COPY_NAME,
//                0,
//                mContext.getString(R.string.long_click_copy_name)
//            )
//            menu.add(
//                position,
//                LONG_CLICK_COPY_COMMAND,
//                0,
//                mContext.getString(R.string.long_click_copy_command)
//            )
//            menu.add(position, LONG_CLICK_NEW, 0, mContext.getString(R.string.long_click_new))
//            menu.add(position, LONG_CLICK_PACK, 0, mContext.getString(R.string.long_click_pack))
//            menu.add(position, LONG_CLICK_DEL, 0, mContext.getString(R.string.long_click_del))
//        }
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

        mContext.isDialogShow = true
        MaterialAlertDialogBuilder(mContext)
            .setTitle(mContext.getString(R.string.dialog_edit))
            .setView(binding.root)
            .setPositiveButton(mContext.getString(R.string.dialog_finish)) { _, _ ->
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
            .setOnDismissListener {
                mContext.isDialogShow = false
            }
            .show()
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
}