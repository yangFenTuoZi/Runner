package yangFenTuoZi.runner.plus.ui.fragment.home

import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import rikka.recyclerview.BaseViewHolder
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.databinding.HomeItemContainerBinding
import yangFenTuoZi.runner.plus.databinding.HomeTermExtStatusBinding

class TermExtStatusViewHolder(binding: HomeTermExtStatusBinding, root: View) :
    BaseViewHolder<Any?>(root) {
    private val title: TextView = binding.text1
    private val summaryView: TextView = binding.text2
    private val install: Button = binding.button1
    private val remove: Button = binding.button2

    override fun onBind() {
        val context = this@TermExtStatusViewHolder.itemView.context
        if (!Runner.pingServer()) return

        try {
            val version = Runner.service?.getTermExtVersion()
            if (version == null || version.versionCode == -1) {
                // not install
                title.setText(R.string.term_ext_title)
                summaryView.visibility = View.GONE
                install.setText(R.string.install)
                remove.visibility = View.GONE
            } else {
                // installed
                title.setText(R.string.term_ext_title_installed)
                summaryView.visibility = View.VISIBLE
                summaryView.text = context.getString(
                    R.string.term_ext_version,
                    version.versionName,
                    version.versionCode,
                    version.abi
                )
                install.setText(R.string.reinstall)
                remove.visibility = View.VISIBLE
            }
        } catch (e: RemoteException) {
            Log.e("TermExtStatusViewHolder", "get term ext version error", e)
        }
    }

    companion object {
        val CREATOR: Creator<Any?> = Creator { inflater: LayoutInflater?, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(
                inflater!!, parent, false
            )
            val inner = HomeTermExtStatusBinding.inflate(inflater, outer.getRoot(), true)
            TermExtStatusViewHolder(inner, outer.getRoot())
        }
    }
}