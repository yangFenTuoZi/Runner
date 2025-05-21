package yangfentuozi.runner.ui.fragment.proc

import android.content.DialogInterface
import android.os.Bundle
import android.os.RemoteException
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var mBinding: FragmentProcBinding
    val binding get() = mBinding
    private var recyclerView: RecyclerView? = null
    private lateinit var adapter: ProcAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentProcBinding.inflate(inflater, container, false)
        recyclerView = mBinding.recyclerView
        recyclerView!!.setLayoutManager(LinearLayoutManager(mMainActivity))
        recyclerView!!.fixEdgeEffect(false, true)

        adapter = ProcAdapter(mMainActivity, this)

        mBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mMainActivity)
            fixEdgeEffect(true, true)
            addItemSpacing(0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            addEdgeSpacing(16f, 4f, 16f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            adapter = this@ProcFragment.adapter
        }
        mBinding.swipeRefreshLayout.setOnRefreshListener {
            adapter.updateData()
        }

        mBinding.procKillAll.setOnClickListener { v ->
            if (Runner.pingServer()) {
                try {
                    if (mBinding.recyclerView.adapter
                            ?.itemCount == 0
                    ) {
                        return@setOnClickListener
                    }
                } catch (_: NullPointerException) {
                }
            } else {
                Toast.makeText(
                    mMainActivity,
                    R.string.service_not_running,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            try {
                BaseDialogBuilder(mMainActivity)
                    .setTitle(R.string.kill_all_processes)
                    .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                        Thread {
                            if (Runner.pingServer()) {
                                try {
                                    adapter.killAll()
                                } catch (e: RemoteException) {
                                    e.toErrorDialog(mMainActivity)
                                }
                            } else {
                                runOnMainThread {
                                    Toast.makeText(
                                        mMainActivity,
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
        return mBinding.getRoot()
    }

    override fun onStart() {
        super.onStart()
        val l = View.OnClickListener { v: View? -> recyclerView!!.smoothScrollToPosition(0) }
        toolbar.setOnClickListener(l)
        adapter.updateData()
    }
}