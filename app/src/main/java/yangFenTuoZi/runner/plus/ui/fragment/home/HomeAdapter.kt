package yangFenTuoZi.runner.plus.ui.fragment.home

import rikka.recyclerview.IdBasedRecyclerViewAdapter
import yangFenTuoZi.runner.plus.Runner

class HomeAdapter : IdBasedRecyclerViewAdapter(ArrayList<Any?>()) {
    init {
        updateData()
        setHasStableIds(true)
    }

    fun updateData() {
        clear()
        addItem<Any?>(ServiceStatusViewHolder.CREATOR, null, ID_SERVICE_STATUS)
        addItem<Any?>(ShizukuStatusViewHolder.CREATOR, null, ID_SHIZUKU_STATUS)

        if (!Runner.shizukuPermission) {
            addItem<Any?>(GrantShizukuPermViewHolder.CREATOR, null, ID_GRANT_SHIZUKU_PERM)
        }

        if (Runner.pingServer()) {
            addItem<Any?>(TermExtStatusViewHolder.CREATOR, null, ID_TERM_EXT_STATUS)
        }

        //        addItem(LearnMoreViewHolder.CREATOR, null, ID_LEARN_MORE);
        notifyDataSetChanged()
    }

    companion object {
        private const val ID_SERVICE_STATUS = 0L
        private const val ID_SHIZUKU_STATUS = 1L
        private const val ID_GRANT_SHIZUKU_PERM = 2L
        private const val ID_TERM_EXT_STATUS = 3L
    }
}