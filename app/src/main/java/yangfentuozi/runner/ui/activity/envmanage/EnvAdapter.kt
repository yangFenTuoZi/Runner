package yangfentuozi.runner.ui.activity.envmanage

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseDialogBuilder
import yangfentuozi.runner.databinding.DialogEditEnvBinding
import yangfentuozi.runner.databinding.DialogEditEnvEBinding
import yangfentuozi.runner.databinding.HomeItemContainerBinding
import yangfentuozi.runner.databinding.ItemEnvBinding
import yangfentuozi.runner.service.data.EnvInfo
import yangfentuozi.runner.util.ThrowableUtil.toErrorDialog

class EnvAdapter(private val mContext: EnvManageActivity) :
    RecyclerView.Adapter<EnvAdapter.ViewHolder>() {
    var dataList: List<EnvInfo> = emptyList()
    private lateinit var mHandler: Handler

    init {
        Thread {
            Looper.prepare()
            Looper.myLooper()?.let { mHandler = Handler(it) }
            Looper.loop()
        }.start()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData() {
        mContext.runOnUiThread { mContext.binding.swipeRefreshLayout.isRefreshing = true }
        mHandler.post {
            try {
                dataList = Runner.service?.allEnv?.asList() ?: emptyList<EnvInfo>()

                for (entry in dataList) {
                    Log.d("EnvAdapter", "get env: " + entry.key + "=" + entry.value)
                }
                mContext.runOnUiThread {
                    notifyDataSetChanged()
                    mContext.binding.swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
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
        mHandler.post {
            try {
                Runner.service?.deleteEnv(dataList[position].key)
                updateData()
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
    }

    fun add(info: EnvInfo) {
        mHandler.post {
            try {
                Runner.service?.insertEnv(info.key, info.value)
                updateData()
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
    }

    fun update(fromKey: String, fromValue: String, toKey: String, toValue: String) {
        mHandler.post {
            try {
                Runner.service?.updateEnv(EnvInfo().apply {
                    key = fromKey
                    value = fromValue
                }, EnvInfo().apply {
                    key = toKey
                    value = toValue
                })
                updateData()
            } catch (e: RemoteException) {
                e.toErrorDialog(mContext)
            }
        }
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
                    fixEdgeEffect(true, true)
                    addItemSpacing(0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP)
                    addEdgeSpacing(16f, 4f, 12f, 0f, TypedValue.COMPLEX_UNIT_DIP)
                    adapter = mAdapter
                }
                MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.edit)
                    .setView(dialogBinding.root)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (!Runner.pingServer()) {
                            Toast.makeText(
                                mContext,
                                R.string.service_not_running,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setPositiveButton
                        }
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
                    if (!Runner.pingServer()) {
                        Toast.makeText(
                            mContext,
                            R.string.service_not_running,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setPositiveButton
                    }

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
                if (!Runner.pingServer()) {
                    Toast.makeText(mContext, R.string.service_not_running, Toast.LENGTH_SHORT)
                        .show()
                    return false
                }
                remove(item.groupId)
                return true
            }
        }
        return true
    }
}