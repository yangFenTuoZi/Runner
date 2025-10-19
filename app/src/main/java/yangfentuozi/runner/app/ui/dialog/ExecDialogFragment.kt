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
import java.util.concurrent.atomic.AtomicBoolean

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
    private val isStopped = AtomicBoolean(false)
    private var callback: IExitCallback? = null
    private var pipe: Array<ParcelFileDescriptor>? = null
    private var readThread: Thread? = null
    private var firstLine = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogExecBinding.inflate(layoutInflater)
        binding.execMsg.typeface = Typeface.createFromAsset(requireContext.assets, "Mono.ttf")
        alertDialog = BaseDialogBuilder(requireContext).apply {
            setTitle(getString(R.string.executing))
            setView(binding.root)
        }.create()
        return alertDialog
    }

    override fun onStart() {
        super.onStart()
        (requireContext as? IsDialogShowing)?.isDialogShowing = true
        
        // 初始化管道和回调
        pipe = ParcelFileDescriptor.createPipe()
        callback = object : IExitCallback.Stub() {
            override fun onExit(exitValue: Int) {
                if (isDismissed || isStopped.get()) return
                
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
                    isDied = true
                }
                cleanup()
            }
        }
        
        // 启动主线程
        mainThread = Thread {
            if (Runner.pingServer()) {
                executeCommand()
            } else {
                waitForService()
            }
        }.apply { start() }
    }

    private fun waitForService() {
        if (waitServiceTimeout != -1L) {
            mHandler.post {
                alertDialog.setTitle(R.string.waiting_service)
            }

            if (Runner.waitShizuku(waitServiceTimeout)) {
                Runner.refreshStatus()
                Runner.tryBindService()
                if (Runner.waitService(waitServiceTimeout)) {
                    executeCommand()
                } else {
                    showError(getString(R.string.service_not_running))
                }
            } else {
                showError(getString(R.string.shizuku_not_running))
            }
        } else {
            showError(getString(R.string.service_not_running))
        }
    }

    private fun executeCommand() {
        try {
            // 启动服务监控线程
            serviceMonitor = Thread {
                try {
                    while (!isStopped.get()) {
                        if (!Runner.pingServer()) {
                            mHandler.post {
                                showError(getString(R.string.service_not_running))
                                isDied = true
                            }
                            break
                        }
                        Thread.sleep(100)
                    }
                } catch (_: InterruptedException) {
                    // 线程被中断，正常退出
                } catch (_: Exception) {
                }
            }.apply { start() }

            // 启动输出读取线程
            readThread = Thread {
                try {
                    pipe?.get(0)?.let { readPipe ->
                        ParcelFileDescriptor.AutoCloseInputStream(readPipe).bufferedReader().use { reader ->
                            while (!isStopped.get()) {
                                val line = reader.readLine() ?: break
                                handleOutputLine(line)
                            }
                        }
                    }
                } catch (e: IOException) {
                    if (!isStopped.get()) {
                        mHandler.post {
                            binding.execMsg.append("! Pipe read error: ${e.message}\n")
                        }
                    }
                }
            }.apply { start() }

            // 执行命令
            val service = Runner.service
            val writePipe = pipe?.get(1)
            if (service != null && writePipe != null) {
                service.exec(cmdInfo.command, cmdInfo.targetPerm, cmdInfo.name, callback, writePipe)
            } else {
                showError(getString(R.string.service_not_running))
            }
        } catch (e: RemoteException) {
            e.toErrorDialog(requireContext)
        }
    }

    private fun handleOutputLine(line: String) {
        if (isDismissed) return
        
        if (firstLine) {
            val p = line.toIntOrNull() ?: -1
            pid = p
            firstLine = false
            mHandler.post {
                binding.execTitle.append("${getString(R.string.pid_info, p)}\n")
            }
        } else {
            mHandler.post {
                binding.execMsg.append("$line\n")
            }
        }
    }

    private fun showError(errorMsg: String) {
        mHandler.post {
            binding.execTitle.append(
                getString(R.string.return_info, -1, errorMsg)
            )
            alertDialog.setTitle(getString(R.string.error))
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        isDismissed = true
        (requireContext as? IsDialogShowing)?.isDialogShowing = false
        
        // 中断主线程
        mainThread.interrupt()
        
        // 如果进程还在运行且不需要保持存活，则杀死进程
        if (Runner.pingServer() && !cmdInfo.keepAlive && !isDied && pid > 0) {
            try {
                Thread {
                    ProcAdapter.Companion.killPIDs(intArrayOf(pid))
                }.start()
            } catch (e: Exception) {
                // 忽略杀进程错误
            }
        }
        
        cleanup()
        mOnDismissListener?.onDismiss(dialog)
    }

    private fun cleanup() {
        if (!isStopped.compareAndSet(false, true)) {
            return // 已经清理过了
        }

        callback = null
        
        // 中断所有线程
        readThread?.interrupt()
        serviceMonitor?.interrupt()
        
        // 安全地关闭管道
        pipe?.let { pipes ->
            try {
                pipes[0].close()
            } catch (_: IOException) {
            }
            try {
                pipes[1].close()
            } catch (_: IOException) {
            }
        }
        pipe = null
    }

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?) {
        mOnDismissListener = onDismissListener
    }
}