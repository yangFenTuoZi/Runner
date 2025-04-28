package yangFenTuoZi.runner.plus.ui.fragment.home

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import rikka.html.text.HtmlCompat
import rikka.recyclerview.BaseViewHolder
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.databinding.HomeItemContainerBinding
import yangFenTuoZi.runner.plus.databinding.HomeServiceStatusBinding

class ServiceStatusViewHolder(binding: HomeServiceStatusBinding, root: View) :
    BaseViewHolder<Any?>(root) {
    private val textView: TextView = binding.text1
    private val summaryView: TextView = binding.text2
    private val iconView: ImageView = binding.icon

    init {
        root.setOnClickListener {
            if (Runner.pingServer()) return@setOnClickListener
            Thread {
                Runner.tryBindService()
            }
        }
    }

    override fun onBind() {
        val context = this@ServiceStatusViewHolder.itemView.context
        val ok = Runner.pingServer()
        val version = Runner.serviceVersion

        iconView.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                if (ok) R.drawable.ic_check_circle_outline_24 else R.drawable.ic_error_outline_24
            )
        )

        val title: String?
        val summary: String?

        if (ok) {
            title = context.getString(R.string.home_status_service_is_running)
            summary = context.getString(R.string.home_status_service_version, version)
        } else {
            title = context.getString(R.string.home_status_service_not_running)
            summary = ""
        }

        textView.text = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        summaryView.text = HtmlCompat.fromHtml(
            summary,
            HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE
        )

        if (TextUtils.isEmpty(summaryView.getText())) {
            summaryView.visibility = View.GONE
        } else {
            summaryView.visibility = View.VISIBLE
        }
    }

    companion object {
        val CREATOR: Creator<Any?> = Creator { inflater: LayoutInflater?, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(
                inflater!!, parent, false
            )
            val inner = HomeServiceStatusBinding.inflate(inflater, outer.getRoot(), true)
            ServiceStatusViewHolder(inner, outer.getRoot())
        }
    }
}