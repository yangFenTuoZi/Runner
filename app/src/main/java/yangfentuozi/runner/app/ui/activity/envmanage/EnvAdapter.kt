package yangfentuozi.runner.app.ui.activity.envmanage

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import yangfentuozi.runner.R
import yangfentuozi.runner.app.base.BaseDialogBuilder
import yangfentuozi.runner.app.data.DataRepository
import yangfentuozi.runner.databinding.DialogEditEnvBinding
import yangfentuozi.runner.databinding.DialogEditEnvEBinding
import yangfentuozi.runner.databinding.HomeItemContainerBinding
import yangfentuozi.runner.databinding.ItemEnvBinding
import yangfentuozi.runner.shared.data.EnvInfo

class EnvAdapter(private val mContext: EnvManageActivity) :
    RecyclerView.Adapter<EnvAdapter.ViewHolder>() {
    private val dataRepository = DataRepository.Companion.getInstance(mContext)
    var dataList: MutableList<EnvInfo> = mutableListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun updateData() {
        mContext.binding.swipeRefreshLayout.isRefreshing = true
        dataList = dataRepository.getAllEnvs().toMutableList()
        notifyDataSetChanged()
        mContext.binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val outer =
            HomeItemContainerBinding.inflate(LayoutInflater.from(mContext)!!, parent, false)
        val inner =
            ItemEnvBinding.inflate(LayoutInflater.from(mContext), outer.getRoot(), true)
        return ViewHolder(inner, outer)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = dataList[holder.bindingAdapterPosition]

        val position = holder.bindingAdapterPosition

        holder.mBindingOuter.root.setOnClickListener {
            if (mContext.isDialogShowing) return@setOnClickListener
            showEditDialog(info)
        }

        holder.mBindingInner.itemName.text = info.key
        holder.mBindingInner.itemContent.text = info.value

        holder.mBindingOuter.root.setOnCreateContextMenuListener { menu, _, _ ->
            menu.add(position, LONG_CLICK_DEL, 0, R.string.delete)
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun remove(position: Int) {
        dataRepository.deleteEnv(dataList[position].key!!)
        dataList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun add(info: EnvInfo) {
        dataRepository.addEnv(info.key!!, info.value!!)
        dataList.add(info)
        notifyItemInserted(dataList.size - 1)
    }

    fun update(fromKey: String?, fromValue: String?, toKey: String?, toValue: String?) {
        dataRepository.updateEnv(fromKey!!, fromValue!!, toKey!!, toValue!!)
        updateData()
    }

    class ViewHolder(bindingInner: ItemEnvBinding, bindingOuter: HomeItemContainerBinding) :
        RecyclerView.ViewHolder(bindingOuter.root) {
        val mBindingInner: ItemEnvBinding = bindingInner
        val mBindingOuter: HomeItemContainerBinding = bindingOuter
    }

    private fun showEditDialog(info: EnvInfo) {

        val binding = DialogEditEnvBinding.inflate(LayoutInflater.from(mContext))

        binding.apply {
            key.setText(info.key)
            value.setText(info.value)
            more.setOnClickListener {
                val dialogBinding = DialogEditEnvEBinding.inflate(mContext.layoutInflater)
                val mAdapter = ItemAdapter(mContext, binding.value.text.toString())
                dialogBinding.recyclerView.apply {
                    layoutManager = LinearLayoutManager(mContext)
                    fixEdgeEffect()
                    addItemSpacing(0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP)
                    addEdgeSpacing(16f, 4f, 12f, 0f, TypedValue.COMPLEX_UNIT_DIP)
                    adapter = mAdapter
                }
                MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.edit)
                    .setView(dialogBinding.root)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        binding.value.setText(mAdapter.save())
                        updateData()
                    }
                    .show()

            }
        }

        try {
            BaseDialogBuilder(mContext)
                .setTitle(R.string.edit)
                .setView(binding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    update(
                        fromKey = info.key,
                        fromValue = info.value,
                        toKey = binding.key.text.toString(),
                        toValue = binding.value.text.toString()
                    )
                }
                .show()
        } catch (_: BaseDialogBuilder.DialogShowingException) {
        }

    }

    companion object {
        const val LONG_CLICK_DEL = 114514
    }

    fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            LONG_CLICK_DEL -> {
                remove(item.groupId)
                return true
            }
        }
        return true
    }
}