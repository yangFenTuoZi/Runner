package yangFenTuoZi.runner.plus.ui.fragment.proc

import android.content.DialogInterface
import android.os.Bundle
import android.os.RemoteException
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
import rikka.recyclerview.fixEdgeEffect
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder
import yangFenTuoZi.runner.plus.base.BaseFragment
import yangFenTuoZi.runner.plus.databinding.FragmentProcBinding
import yangFenTuoZi.runner.plus.service.callback.IExecResultCallback
import yangFenTuoZi.runner.plus.util.ThrowableUtil.toErrorDialog

class ProcFragment : BaseFragment() {
    var binding: FragmentProcBinding? = null
        private set
    private var recyclerView: RecyclerView? = null
    var onRefreshListener: OnRefreshListener = OnRefreshListener {
        lifecycleScope.launch {
            delay(1000)
            initList()
            binding?.swipeRefreshLayout?.isRefreshing = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentProcBinding.inflate(inflater, container, false)
        recyclerView = binding!!.recyclerView
        recyclerView!!.setLayoutManager(LinearLayoutManager(mContext))
        recyclerView!!.fixEdgeEffect(false, true)

        binding!!.swipeRefreshLayout.setOnRefreshListener(onRefreshListener)

        binding!!.procKillAll.setOnClickListener { v ->
            if (Runner.pingServer()) {
                try {
                    if (binding!!.recyclerView.adapter
                            ?.itemCount == 0
                    ) {
                        return@setOnClickListener
                    }
                } catch (_: NullPointerException) {
                }
            } else {
                Toast.makeText(
                    mContext,
                    R.string.home_status_service_not_running,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            try {
                BaseDialogBuilder(mContext)
                    .setTitle(R.string.process_kill_all_processes)
                    .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                        Thread {
                            if (Runner.pingServer()) {
                                try {
                                    val outs = StringBuilder()
                                    Runner.service?.exec("busybox ps -A -o pid,args|grep RUNNER-proc:|grep -v grep", null, "Task-GetProcList", object : IExecResultCallback.Stub() {
                                        @Throws(RemoteException::class)
                                        override fun onOutput(outputs: String?) {
                                            outs.append(outputs)
                                        }

                                        @Throws(RemoteException::class)
                                        override fun onExit(exitValue: Int) {
                                            if (exitValue != 0) {
                                                runOnUiThread {
                                                    Toast.makeText(
                                                        mContext,
                                                        R.string.failed_to_get_the_process_list,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            val strings = outs.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                            val data = IntArray(strings.size)
                                            var i = 0
                                            for (proc in strings) {
                                                if (!proc.isEmpty()) {
                                                    val pI = proc.replace(" +".toRegex(), " ")
                                                        .trim { it <= ' ' }.split(" ".toRegex())
                                                        .dropLastWhile { it.isEmpty() }
                                                        .toTypedArray()
                                                    if (pI[2].matches("^RUNNER-proc:.*".toRegex())) {
                                                        data[i] = pI[0].toInt()
                                                        i++
                                                    }
                                                }
                                            }
                                            ProcAdapter.killPIDs(data)
                                        }
                                    })
                                } catch (e: RemoteException) {
                                    e.toErrorDialog(mContext)
                                }
                                initList()
                            } else {
                                runOnUiThread {
                                    Toast.makeText(
                                        mContext,
                                        R.string.home_status_service_not_running,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }.start()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (_: BaseDialogBuilder.DialogShowException) {
            }
        }
        return binding!!.getRoot()
    }

    fun initList() {
        Thread {
            if (Runner.pingServer()) {
                try {
                    val outs = StringBuilder()
                    Runner.service?.exec("busybox ps -A -o pid,args|grep RUNNER-proc:|grep -v grep", null, "Task-GetProcList", object : IExecResultCallback.Stub() {
                        @Throws(RemoteException::class)
                        override fun onOutput(outputs: String?) {
                            outs.append(outputs)
                        }

                        @Throws(RemoteException::class)
                        override fun onExit(exitValue: Int) {
                            if (exitValue != 0) {
                                runOnUiThread {
                                    Toast.makeText(
                                        mContext,
                                        R.string.failed_to_get_the_process_list,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            val strings = outs.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            val data = IntArray(strings.size)
                            val dataName = arrayOfNulls<String>(strings.size)
                            var i = 0
                            for (proc in strings) {
                                if (!proc.isEmpty()) {
                                    val pI = proc.replace(" +".toRegex(), " ").trim { it <= ' ' }
                                        .split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                    if (pI[2].matches("^RUNNER-proc:.*".toRegex())) {
                                        data[i] = pI[0].toInt()
                                        dataName[i] =
                                            pI[2].split(":".toRegex(), limit = 2).toTypedArray()[1]
                                        i++
                                    }
                                }
                            }
                            runOnUiThread {
                                binding!!.recyclerView.setAdapter(
                                    ProcAdapter(
                                        mContext,
                                        data,
                                        dataName,
                                        this@ProcFragment
                                    )
                                )
                            }
                        }
                    })
                } catch (e: RemoteException) {
                    e.toErrorDialog(mContext)
                }
            } else {
                runOnUiThread {
                    Toast.makeText(
                        mContext,
                        R.string.home_status_service_not_running,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onStart() {
        super.onStart()
        val l = View.OnClickListener { v: View? -> recyclerView!!.smoothScrollToPosition(0) }
        getToolbar().setOnClickListener(l)
        initList()
    }
}