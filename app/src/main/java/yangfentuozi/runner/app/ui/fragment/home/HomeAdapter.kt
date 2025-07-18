package yangfentuozi.runner.app.ui.fragment.home

import android.annotation.SuppressLint
import rikka.recyclerview.IdBasedRecyclerViewAdapter
import rikka.recyclerview.IndexCreatorPool
import yangfentuozi.runner.app.Runner

class HomeAdapter(private val fragment: HomeFragment) : IdBasedRecyclerViewAdapter(ArrayList<Any?>()) {

    private val shizukuPermissionListener = Runner.ShizukuPermissionListener {
        var position = findPositionById(ID_GRANT_SHIZUKU_PERM)
        if (position == -1) {
            if (it) return@ShizukuPermissionListener
            position = 2
            addItemAt<Any?>(position, GrantShizukuPermViewHolder.CREATOR, null, ID_GRANT_SHIZUKU_PERM)
            notifyItemInserted(position)
        } else {
            if (!it) return@ShizukuPermissionListener
            removeItemAt(position)
            notifyItemRemoved(position)
        }
    }

    private val shizukuStatusListener = Runner.ShizukuStatusListener {
        val position = 1
        removeItemAt(position)
        addItemAt<Any?>(position, ShizukuStatusViewHolder.CREATOR, null, ID_SHIZUKU_STATUS)
        notifyItemChanged(position)
    }

    private val serviceStatusListener = Runner.ServiceStatusListener {
        var position = 0
        removeItemAt(position)
        addItemAt<Any?>(position, ServiceStatusViewHolder.CREATOR, null, ID_SERVICE_STATUS)
        notifyItemChanged(position)

        position = findPositionById(ID_TERM_EXT_STATUS)
        if (position == -1) {
            if (!it) return@ServiceStatusListener
            position = if (findPositionById(ID_GRANT_SHIZUKU_PERM) == -1) 2 else 3
            addItemAt<HomeFragment>(position, TermExtStatusViewHolder.CREATOR, fragment, ID_TERM_EXT_STATUS)
            notifyItemInserted(position)
        } else {
            if (it) return@ServiceStatusListener
            removeItemAt(position)
            notifyItemRemoved(position)
        }
    }

    init {
        updateData()
        setHasStableIds(true)
    }

    companion object {
        private const val ID_SERVICE_STATUS = 0L
        private const val ID_SHIZUKU_STATUS = 1L
        private const val ID_GRANT_SHIZUKU_PERM = 2L
        private const val ID_TERM_EXT_STATUS = 3L
    }

    override fun onCreateCreatorPool(): IndexCreatorPool {
        return IndexCreatorPool()
    }

    fun findPositionById(id: Long): Int {
        for (i in 0 until ids.size)
            if (ids[i] == id)
                return i
        return -1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData() {
        clear()
        addItem<Any?>(ServiceStatusViewHolder.CREATOR, null, ID_SERVICE_STATUS)
        addItem<Any?>(ShizukuStatusViewHolder.CREATOR, null, ID_SHIZUKU_STATUS)

        if (!Runner.shizukuPermission) {
            addItem<Any?>(GrantShizukuPermViewHolder.CREATOR, null, ID_GRANT_SHIZUKU_PERM)
        }

        if (Runner.pingServer()) {
            addItem<HomeFragment>(TermExtStatusViewHolder.CREATOR, fragment, ID_TERM_EXT_STATUS)
        }

        notifyDataSetChanged()
    }

    fun registerListeners() {
        Runner.addShizukuPermissionListener(shizukuPermissionListener)
        Runner.addShizukuStatusListener(shizukuStatusListener)
        Runner.addServiceStatusListener(serviceStatusListener)
    }

    fun unregisterListeners() {
        Runner.removeShizukuPermissionListener(shizukuPermissionListener)
        Runner.removeShizukuStatusListener(shizukuStatusListener)
        Runner.removeServiceStatusListener(serviceStatusListener)
    }
}