package yangFenTuoZi.runner.plus.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.RemoteException
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
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
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder.DialogShowException
import yangFenTuoZi.runner.plus.databinding.DialogEditBinding
import yangFenTuoZi.runner.plus.service.CommandInfo
import yangFenTuoZi.runner.plus.ui.activity.MainActivity
import yangFenTuoZi.runner.plus.ui.dialog.ExecDialogBuilder
import yangFenTuoZi.runner.plus.utils.ExceptionUtils.toErrorDialog
import java.lang.String
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.BooleanArray
import kotlin.CharSequence
import kotlin.Int
import kotlin.booleanArrayOf
import kotlin.toString

class CmdAdapter(private val mContext: MainActivity, count: Int) :
    RecyclerView.Adapter<CmdAdapter.ViewHolder?>() {
    private var count = 0
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor() // 用于异步加载数据

    init {
        updateData(count)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(count: Int) {
        this.count = count
        notifyDataSetChanged()

        try {
            Runner.service.closeCursor()
        } catch (e: RemoteException) {
            e.toErrorDialog(mContext)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cmd, parent, false)
        val holder = ViewHolder(view)
        view.tag = holder
        view.setOnKeyListener(View.OnKeyListener { v: View?, i: Int, keyEvent: KeyEvent? -> false })
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 异步加载数据

        executorService.execute(Runnable {
            try {
                val info = Runner.service.query(holder.position())
                mContext.runOnUiThread(Runnable { init(holder, info) }) // 回到主线程更新 UI
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        })
    }

    override fun getItemCount(): Int {
        return count
    }

    fun remove(position: Int) {
        try {
            Runner.service.delete(position + 1)
            count--
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount - position)
        } catch (e: RemoteException) {
            e.toErrorDialog(mContext)
        }
    }

    fun add(info: CommandInfo) {
        try {
            info.rowid = count++
            Runner.service.insert(info)
            notifyItemChanged(count)
        } catch (e: RemoteException) {
            e.toErrorDialog(mContext)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var text_name: TextView = view.findViewById<TextView?>(R.id.item_name)
        var text_command: TextView = view.findViewById<TextView?>(R.id.item_command)
        var item_button: MaterialButton = view.findViewById<MaterialButton?>(R.id.item_button)
        var layout: MaterialCardView = view.findViewById<MaterialCardView?>(R.id.item_root)

        fun position(): Int {
            return getBindingAdapterPosition()
        }
    }

    fun init(holder: ViewHolder, info: CommandInfo) {
        val empty = isEmpty(info)

        //如果用户还没设置命令内容，则点击时将编辑命令，否则点击将运行命令
        holder.item_button.setOnClickListener(View.OnClickListener { view: View? ->
            if (mContext.isDialogShow) return@OnClickListener
            if (Runner.pingServer()) {
                try {
                    ExecDialogBuilder(mContext, info).show()
                } catch (_: DialogShowException) {
                }
            } else Toast.makeText(
                mContext,
                R.string.home_status_service_not_running,
                Toast.LENGTH_SHORT
            ).show()
        })

        //点击编辑
        holder.layout.setOnClickListener(View.OnClickListener { view: View? ->
            if (mContext.isDialogShow) return@OnClickListener
            val binding = DialogEditBinding.inflate(LayoutInflater.from(mContext))

            binding.dialogChid.setChecked(info.useChid)
            binding.dialogChid.setOnCheckedChangeListener { buttonView, isChecked ->
                binding.dialogUidGid.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            binding.dialogKeepItAlive.setChecked(info.keepAlive)
            binding.dialogUidGid.visibility = if (info.useChid) View.VISIBLE else View.GONE
            binding.dialogName.setText(info.name)
            binding.dialogCommand.setText(info.command)

            binding.dialogIds.setText(info.ids)
            binding.dialogName.requestFocus()
            binding.dialogName.postDelayed({
                (mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                    binding.dialogName,
                    0
                )
            }, 200)
            mContext.isDialogShow = true
            MaterialAlertDialogBuilder(mContext)
                .setTitle(mContext.getString(R.string.dialog_edit))
                .setView(binding.getRoot())
                .setPositiveButton(
                    mContext.getString(R.string.dialog_finish),
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        if (!Runner.pingServer()) {
                            Toast.makeText(
                                mContext,
                                R.string.home_status_service_not_running,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OnClickListener
                        }
                        info.rowid = holder.position()
                        info.command = String.valueOf(binding.dialogCommand.getText())
                        info.name = String.valueOf(binding.dialogName.getText())
                        info.keepAlive = binding.dialogKeepItAlive.isChecked
                        info.useChid = binding.dialogChid.isChecked
                        info.ids =
                            if (binding.dialogChid.isChecked) binding.dialogIds.getText()
                                .toString() else null
                        try {
                            Runner.service.update(info)
                        } catch (_: RemoteException) {
                        }
                        if (!empty[0] && isEmpty(info)[0]) remove(holder.position())
                        else {
                            init(holder, info)
                        }
                    })
                .setOnDismissListener(DialogInterface.OnDismissListener { dialog: DialogInterface? ->
                    mContext.isDialogShow = false
                }).show()
        })

        holder.text_name.text = if (empty[1]) getItalicText("__NAME__") else info.name
        holder.text_command.text = if (empty[2]) getItalicText("__CMD__") else info.command


        //如果不为空则设置长按菜单
        holder.layout.setOnCreateContextMenuListener(OnCreateContextMenuListener { menu: ContextMenu?, v: View?, menuInfo: ContextMenuInfo? ->
            menu!!.add(
                holder.position(),
                long_click_copy_name,
                0,
                mContext.getString(R.string.long_click_copy_name)
            )
            menu.add(
                holder.position(),
                long_click_copy_command,
                0,
                mContext.getString(R.string.long_click_copy_command)
            )
            menu.add(
                holder.position(),
                long_click_new,
                0,
                mContext.getString(R.string.long_click_new)
            )
            menu.add(
                holder.position(),
                long_click_pack,
                0,
                mContext.getString(R.string.long_click_pack)
            )
            menu.add(
                holder.position(),
                long_click_del,
                0,
                mContext.getString(R.string.long_click_del)
            )
        })
    }

    fun getItalicText(text: kotlin.String): CharSequence {
        val builder = SpannableStringBuilder(text)
        builder.setSpan(
            StyleSpan(Typeface.ITALIC),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return builder
    }

    fun close() {
        executorService.shutdown()
    }

    companion object {
        const val long_click_copy_name: Int = 0
        const val long_click_copy_command: Int = 1
        const val long_click_new: Int = 2
        const val long_click_pack: Int = 3
        const val long_click_del: Int = 4

        //判断是否为空
        fun isEmpty(info: CommandInfo): BooleanArray {
            val exist_c = info.command == null || info.command.isEmpty()
            val exist_n = info.name == null || info.name.isEmpty()
            return booleanArrayOf(exist_c && exist_n, exist_n, exist_c)
        }
    }
}