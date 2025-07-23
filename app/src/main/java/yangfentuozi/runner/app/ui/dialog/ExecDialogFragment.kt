package yangfentuozi.runner.app.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.base.BaseDialogBuilder
import yangfentuozi.runner.app.base.BaseDialogBuilder.IsDialogShowing
import yangfentuozi.runner.app.ui.fragment.proc.ProcAdapter
import yangfentuozi.runner.app.util.ThrowableUtil.toErrorDialog
import yangfentuozi.runner.databinding.DialogExecBinding
import yangfentuozi.runner.server.callback.IExecResultCallback
import yangfentuozi.runner.shared.data.CommandInfo

class ExecDialogFragment(private val cmdInfo: CommandInfo, val waitServiceTimeout: Long = -1L) :
    DialogFragment() {

    private val requireContext: Context
        get() = requireContext()

    private val mHandler = Handler(Looper.getMainLooper())
    private lateinit var alertDialog: AlertDialog
    private lateinit var mainThread: Thread
    private lateinit var binding: DialogExecBinding
    private var mOnDismissListener: DialogInterface.OnDismissListener? = null
    private var serviceMonitor: Thread? = null
    private var pid = -1
    private var isDied = false
    private var isDismissed = false
    private var callback: IExecResultCallback? = object : IExecResultCallback.Stub() {
        private var firstLine = true
        override fun onOutput(outputs: String) {
            if (isDismissed) return
            if (firstLine) {
                val p = outputs.toInt()
                mHandler.post {
                    binding.execTitle.append("${getString(R.string.pid_info, p)}\n")
                }
                firstLine = false
                pid = p
            } else {
                mHandler.post {
                    binding.execMsg.append(outputs + "\n")
                }
            }
        }

        override fun onExit(exitValue: Int) {
            if (isDismissed) return
            mHandler.post {
                binding.execTitle.append(
                    getString(
                        R.string.return_info, exitValue, getString(
                            when (exitValue) {
                                0 -> R.string.normal
                                127 -> R.string.command_not_found
                                130 -> R.string.ctrl_c_exit
                                139 -> R.string.segmentation_error
                                else -> R.string.other_error
                            }
                        )
                    )
                )
                alertDialog.setTitle(getString(R.string.finish))
            }
            callback = null
            isDied = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogExecBinding.inflate(layoutInflater)
        alertDialog = BaseDialogBuilder(requireContext).apply {
            setTitle(getString(R.string.executing))
            setView(binding.getRoot())
        }.create()
        return alertDialog
    }

    override fun onStart() {
        super.onStart()
        (requireContext as? IsDialogShowing)?.isDialogShowing = true
        mainThread = Thread {
            if (Runner.pingServer()) {
                exec()
            } else {
                waitService()
            }
        }.apply { start() }
    }

    fun waitService() {
        if (waitServiceTimeout != -1L) {
            mHandler.post {
                alertDialog.setTitle(R.string.waiting_service)
            }

            if (Runner.waitShizuku(waitServiceTimeout)) {
                Runner.refreshStatus()
                Runner.tryBindService()
                if (Runner.waitService(waitServiceTimeout)) {
                    exec()
                } else {
                    mHandler.post {
                        binding.execTitle.append(
                            getString(
                                R.string.return_info,
                                -1,
                                getString(R.string.service_not_running)
                            )
                        )
                        alertDialog.setTitle(getString(R.string.error))
                    }
                }
            } else {
                mHandler.post {
                    binding.execTitle.append(
                        getString(
                            R.string.return_info,
                            -1,
                            getString(R.string.shizuku_not_running)
                        )
                    )
                    alertDialog.setTitle(getString(R.string.error))
                }
            }
        } else {
            mHandler.post {
                binding.execTitle.append(
                    getString(
                        R.string.return_info,
                        -1,
                        getString(R.string.service_not_running)
                    )
                )
                alertDialog.setTitle(getString(R.string.error))
            }
        }
    }

    fun exec() {
        try {
            serviceMonitor = Thread {
                try {
                    while (true) {
                        if (!Runner.pingServer()) {
                            mHandler.post {
                                binding.execTitle.append(
                                    getString(
                                        R.string.return_info,
                                        -1,
                                        getString(R.string.service_not_running)
                                    )
                                )
                                alertDialog.setTitle(getString(R.string.error))
                                isDied = true
                            }
                            break
                        }
                        (this as Object).wait(100)
                    }
                } catch (_: Exception) {
                }
            }.apply { start() }
            try {
                Runner.service?.exec(cmdInfo.command, cmdInfo.targetPerm, cmdInfo.name, callback)
            } catch (e: RemoteException) {
                e.toErrorDialog(requireContext)
            }
        } catch (_: RemoteException) {
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        isDismissed = true
        (requireContext as? IsDialogShowing)?.isDialogShowing = false
        mainThread.interrupt()
        if (Runner.pingServer()) {
            try {
                if (!cmdInfo.keepAlive && !isDied) {
                    Thread {
                        ProcAdapter.Companion.killPIDs(intArrayOf(pid))
                    }.start()
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        callback = null
        mOnDismissListener?.onDismiss(dialog)
    }

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?) {
        mOnDismissListener = onDismissListener
    }
}