package yangfentuozi.runner.ui.fragment.proc

import android.content.DialogInterface
import android.os.Bundle
import android.os.RemoteException
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseDialogBuilder
import yangfentuozi.runner.base.BaseFragment
import yangfentuozi.runner.databinding.FragmentProcBinding
import yangfentuozi.runner.util.ThrowableUtil.toErrorDialog

class ProcFragment : BaseFragment() {
    private lateinit var binding: FragmentProcBinding
    private var recyclerView: RecyclerView? = null
    private lateinit var adapter: ProcAdapter
    var onRefreshListener: OnRefreshListener = OnRefreshListener {
        lifecycleScope.launch {
            delay(1000)
            adapter.updateData()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentProcBinding.inflate(inflater, container, false)
        recyclerView = binding.recyclerView
        recyclerView!!.setLayoutManager(LinearLayoutManager(mContext))
        recyclerView!!.fixEdgeEffect(false, true)

        adapter = ProcAdapter(mContext)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mContext)
            fixEdgeEffect(true, true)
            addItemSpacing(0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            addEdgeSpacing(16f, 4f, 16f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            adapter = this@ProcFragment.adapter
        }
        binding.swipeRefreshLayout.setOnRefreshListener(onRefreshListener)

        binding.procKillAll.setOnClickListener { v ->
            if (Runner.pingServer()) {
                try {
                    if (binding.recyclerView.adapter
                            ?.itemCount == 0
                    ) {
                        return@setOnClickListener
                    }
                } catch (_: NullPointerException) {
                }
            } else {
                Toast.makeText(
                    mContext,
                    R.string.service_not_running,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            try {
                BaseDialogBuilder(mContext)
                    .setTitle(R.string.kill_all_processes)
                    .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                        Thread {
                            if (Runner.pingServer()) {
                                try {
                                    adapter.killAll()
                                } catch (e: RemoteException) {
                                    e.toErrorDialog(mContext)
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(
                                        mContext,
                                        R.string.service_not_running,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }.start()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (_: BaseDialogBuilder.DialogShowingException) {
            }
        }
        return binding.getRoot()
    }

    override fun onStart() {
        super.onStart()
        val l = View.OnClickListener { v: View? -> recyclerView!!.smoothScrollToPosition(0) }
        getToolbar().setOnClickListener(l)
        adapter.updateData()
    }
}