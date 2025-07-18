package yangfentuozi.runner.app.ui.activity.envmanage

import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import yangfentuozi.runner.R
import yangfentuozi.runner.databinding.DialogEditEnvE2Binding
import yangfentuozi.runner.databinding.ItemEnvItemBinding


class ItemAdapter(private val mContext: EnvManageActivity, value: String) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
    var data: String = ""
    var dataList = ArrayList<String>()

    init {
        data = value
        dataList = ArrayList<String>(value.split(":"))

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemEnvItemBinding.inflate(mContext.layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val position = holder.bindingAdapterPosition
        if (position == itemCount - 1) {
            holder.binding.value.visibility = ViewGroup.GONE
            holder.binding.remove.apply {
                contentDescription = mContext.getString(R.string.add)
                icon = mContext.getDrawable(R.drawable.ic_add_24)
                setOnClickListener {
                    dataList.add("")
                    notifyItemInserted(holder.bindingAdapterPosition)
                    mContext.binding.recyclerView.scrollToPosition(dataList.size - 1)
                }
            }
            return
        }
        val info = dataList[position]
        holder.binding.value.apply {
            setText(info)
            keyListener = null
            setOnClickListener {
                val dialogBinding = DialogEditEnvE2Binding.inflate(mContext.layoutInflater)
                MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.edit)
                    .setView(dialogBinding.root)
                    .setNegativeButton(android.R.string.ok) { _, _ ->
                        dataList[position] = dialogBinding.value.text.toString()
                        text = dialogBinding.value.text
                    }
                    .show()
            }
        }
        holder.binding.remove.setOnClickListener {
            holder.binding.root.apply {
                setFocusable(false)
                setFocusableInTouchMode(false)
                clearFocus()

                for (i in 0..<size) {
                    val child: View = getChildAt(i)
                    child.setFocusable(false)
                    child.setFocusableInTouchMode(false)
                    child.clearFocus()
                }
            }
            val position = holder.bindingAdapterPosition
            dataList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount(): Int = dataList.size + 1

    class ViewHolder(var binding: ItemEnvItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun save(): String {
        return dataList.joinToString(":")
    }
}
