package yangFenTuoZi.runner.plus.ui.dialog

import android.content.DialogInterface
import android.os.RemoteException
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseActivity
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder
import yangFenTuoZi.runner.plus.databinding.DialogExecBinding
import yangFenTuoZi.runner.plus.service.callback.IExecResultCallback
import yangFenTuoZi.runner.plus.service.data.CommandInfo
import yangFenTuoZi.runner.plus.ui.fragment.proc.ProcAdapter
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
        setTitle(getString(R.string.exec_running))
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
        val cmd = if (cmdInfo.useChid) "chid " + cmdInfo.ids + " " + cmdInfo.command
        else cmdInfo.command
        if (Runner.pingServer()) {
            h1 = Thread {
                try {
                    h2 = Thread {
                        var pid1 = false
                        callback = object : IExecResultCallback.Stub() {
                            override fun onOutput(outputs: String) {
                                if (pid1) {
                                    runOnUiThread {
                                        binding.execMsg.append(outputs + "\n")
                                    }
                                } else {
                                    try {
                                        val p = outputs.toInt()
                                        runOnUiThread {
                                            binding.execTitle.append(getString(R.string.exec_pid, p) + "\n")
                                        }
                                        pid1 = true
                                        pid = p
                                    } catch (_: Exception) {
                                        runOnUiThread {
                                            binding.execMsg.append(outputs + "\n")
                                        }
                                    }
                                }
                            }

                            override fun onExit(exitValue: Int) {
                                runOnUiThread {
                                    binding.execTitle.append(
                                        getString(
                                            R.string.exec_return, exitValue, getString(
                                                when (exitValue) {
                                                    0 -> R.string.exec_normal
                                                    127 -> R.string.exec_command_not_found
                                                    130 -> R.string.exec_ctrl_c_error
                                                    139 -> R.string.exec_segmentation_error
                                                    else -> R.string.exec_other_error
                                                }
                                            )
                                        )
                                    )
                                    getAlertDialog()!!.setTitle(getString(R.string.exec_finish))
                                }
                                br = true
                                br2 = true
                            }
                        }
                        try {
                            Runner.service?.exec(cmd, cmdInfo.ids, cmdInfo.name, callback)
                        } catch (e: RemoteException) {
                            Log.e(javaClass.getName(), Objects.requireNonNull<String?>(e.message))
                        }
                    }
                    Thread {
                        try {
                            while (true) {
                                if (!Runner.pingServer()) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            mContext,
                                            R.string.home_status_service_not_running,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        binding.execTitle.append(
                                            getString(
                                                R.string.exec_return,
                                                -1,
                                                getString(R.string.exec_other_error)
                                            )
                                        )
                                        getAlertDialog()!!.setTitle(getString(R.string.exec_finish))
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
                        try {
                            if (ProcAdapter.killPID(pid)) {
                                runOnUiThread {
                                    Toast.makeText(
                                        mContext,
                                        R.string.process_the_killing_process_succeeded,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else runOnUiThread {
                                Toast.makeText(
                                    mContext,
                                    R.string.process_failed_to_kill_the_process,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (_: Exception) {
                        }
                    }.start()
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        callback = null
    }
}
