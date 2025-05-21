package yangfentuozi.runner.ui.dialog

import android.content.DialogInterface
import android.os.RemoteException
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseActivity
import yangfentuozi.runner.base.BaseDialogBuilder
import yangfentuozi.runner.databinding.DialogExecBinding
import yangfentuozi.runner.service.callback.IExecResultCallback
import yangfentuozi.runner.service.data.CommandInfo
import yangfentuozi.runner.ui.fragment.proc.ProcAdapter
import java.util.Objects

class ExecDialogBuilder(context: BaseActivity, cmdInfo: CommandInfo) : BaseDialogBuilder(context) {
    var pid: Int = 0
    var cmdInfo: CommandInfo
    var h1: Thread? = null
    var h2: Thread? = null
    var br: Boolean = false
    var br2: Boolean = false
    var callback: IExecResultCallback? = null
    var mContext: BaseActivity? = context
    var binding: DialogExecBinding = DialogExecBinding.inflate(LayoutInflater.from(mContext))

    init {
        setView(binding.getRoot())
        setTitle(getString(R.string.executing))
        setOnDismissListener(DialogInterface.OnDismissListener { dialog: DialogInterface? -> onDestroy() })
        this.cmdInfo = cmdInfo
    }

    override fun show(): AlertDialog? {
        super.show()
        binding.execMsg.requestFocus()
        binding.execMsg.setOnKeyListener { view, i, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN) getAlertDialog()!!.cancel()
            false
        }
        //子线程执行命令，否则UI线程执行就会导致UI卡住动不了
        val cmd = if (cmdInfo.reducePerm) "chid " + cmdInfo.targetPerm + " " + cmdInfo.command
        else cmdInfo.command
        if (Runner.pingServer()) {
            h1 = Thread {
                try {
                    h2 = Thread {
                        var pid1 = false
                        callback = object : IExecResultCallback.Stub() {
                            override fun onOutput(outputs: String) {
                                if (pid1) {
                                    runOnMainThread {
                                        binding.execMsg.append(outputs + "\n")
                                    }
                                } else {
                                    val p = outputs.toInt()
                                    runOnMainThread {
                                        binding.execTitle.append(
                                            getString(
                                                R.string.pid_info,
                                                p
                                            ) + "\n"
                                        )
                                    }
                                    pid1 = true
                                    pid = p
                                }
                            }

                            override fun onExit(exitValue: Int) {
                                runOnMainThread {
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
                                    getAlertDialog()!!.setTitle(getString(R.string.finish))
                                }
                                br = true
                                br2 = true
                            }
                        }
                        try {
                            Runner.service?.exec(cmd, cmdInfo.targetPerm, cmdInfo.name, callback)
                        } catch (e: RemoteException) {
                            Log.e(javaClass.getName(), Objects.requireNonNull<String?>(e.message))
                        }
                    }
                    Thread {
                        try {
                            while (true) {
                                if (!Runner.pingServer()) {
                                    runOnMainThread {
                                        Toast.makeText(
                                            mContext,
                                            R.string.service_not_running,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        binding.execTitle.append(
                                            getString(
                                                R.string.return_info,
                                                -1,
                                                getString(R.string.other_error)
                                            )
                                        )
                                        getAlertDialog()!!.setTitle(getString(R.string.finish))
                                        br2 = true
                                    }
                                    onDestroy()
                                    break
                                }
                                if (br) break
                                (this as Object).wait(100)
                            }
                        } catch (_: Exception) {
                        }
                    }.start()
                    h2!!.start()
                } catch (_: RemoteException) {
                }
            }
            h1!!.start()
        }
        return getAlertDialog()
    }

    fun onDestroy() {
        br = true
        h2!!.interrupt()
        h1!!.interrupt()
        if (Runner.pingServer()) {
            try {
                if (!cmdInfo.keepAlive && !br2) {
                    Thread {
                        ProcAdapter.killPIDs(intArrayOf(pid))
                    }.start()
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        callback = null
    }
}
