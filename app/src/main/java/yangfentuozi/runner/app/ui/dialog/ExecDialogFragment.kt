package yangfentuozi.runner.app.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
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
import yangfentuozi.runner.server.callback.IExitCallback
import yangfentuozi.runner.shared.data.CommandInfo
import java.io.IOException

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
    private var breakRead = false
    private var callback: IExitCallback? = object : IExitCallback.Stub() {
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
            isDied = true
            stopAndClean()
        }
    }
    private var pipe: Array<ParcelFileDescriptor>? = ParcelFileDescriptor.createPipe()
    private var readThread: Thread? = Thread {
        try {
            val reader = ParcelFileDescriptor.AutoCloseInputStream(pipe?.get(0)).bufferedReader()
            var line: String?
            while (reader.readLine().also { line = it } != null && !breakRead) {
                onMessage(line)
            }
        } catch (e: IOException) {
            if (breakRead) return@Thread
            onMessage("! Pipe read error:\n" + e.stackTraceToString())
        } finally {
            try {
                pipe?.get(0)?.close()
                pipe?.get(1)?.close()
            } catch (_: IOException) {
            }
        }
    }
    private var firstLine = true
    private fun onMessage(message: String?) {
        if (isDismissed) return
        if (firstLine) {
            val p = message?.toInt() ?: -1
            mHandler.post {
                binding.execTitle.append("${getString(R.string.pid_info, p)}\n")
            }
            firstLine = false
            pid = p
        } else {
            mHandler.post {
                binding.execMsg.append(message + "\n")
            }
        }
    }

    fun stopAndClean() {
        Thread {
            breakRead = true
            callback = null
            Thread.sleep(100)
            readThread?.interrupt()
            pipe?.get(0)?.close()
            pipe?.get(1)?.close()
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogExecBinding.inflate(layoutInflater)
        binding.execMsg.typeface = Typeface.createFromAsset(requireContext.assets, "Mono.ttf")
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
                readThread?.start()
                Runner.service?.exec(cmdInfo.command, cmdInfo.targetPerm, cmdInfo.name, callback, pipe!![1])
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
        stopAndClean()
        mOnDismissListener?.onDismiss(dialog)
    }

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?) {
        mOnDismissListener = onDismissListener
    }
}