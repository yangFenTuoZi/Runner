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
import yangFenTuoZi.runner.plus.adapters.ProcAdapter
import yangFenTuoZi.runner.plus.base.BaseActivity
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder
import yangFenTuoZi.runner.plus.databinding.DialogExecBinding
import yangFenTuoZi.runner.plus.service.CommandInfo
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.Objects

class ExecDialogBuilder(context: BaseActivity, cmdInfo: CommandInfo) : BaseDialogBuilder(context) {
    var pid: Int = 0
    var port: Int = 0
    var cmdInfo: CommandInfo
    var h1: Thread? = null
    var h2: Thread? = null
    var br: Boolean = false
    var br2: Boolean = false
    var serverSocket: ServerSocket? = null
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
            h1 = Thread(Runnable {
                try {
                    port = getUsablePort(8400)
                    if (port == -1) return@Runnable
                    h2 = Thread(Runnable {
                        try {
                            serverSocket = ServerSocket(port)
                            while (!br) {
                                val socket = serverSocket!!.accept()
                                val thread = Thread(Runnable {
                                    try {
                                        val br =
                                            BufferedReader(InputStreamReader(socket.getInputStream()))
                                        var inline: String
                                        var pid1 = false
                                        while ((br.readLine().also { inline = it }) != null) {
                                            val finalInline = inline
                                            if (pid1) {
                                                runOnUiThread(Runnable {
                                                    binding.execMsg.append(
                                                        finalInline + "\n"
                                                    )
                                                })
                                            } else {
                                                try {
                                                    val p = finalInline.toInt()
                                                    runOnUiThread(Runnable {
                                                        binding.execTitle.append(
                                                            getString(R.string.exec_pid, p) + "\n"
                                                        )
                                                    })
                                                    pid1 = true
                                                    pid = p
                                                } catch (_: Exception) {
                                                    runOnUiThread(Runnable {
                                                        binding.execMsg.append(
                                                            finalInline + "\n"
                                                        )
                                                    })
                                                }
                                            }
                                        }
                                        br.close()
                                        socket.close()
                                    } catch (_: Exception) {
                                    }
                                })
                                thread.start()
                            }
                            serverSocket!!.close()
                        } catch (e: Exception) {
                            Log.e(javaClass.getName(), Objects.requireNonNull<String?>(e.message))
                        }
                    })
                    Thread(Runnable {
                        try {
                            while (true) {
                                if (!Runner.pingServer()) {
                                    runOnUiThread(Runnable {
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
                                    })
                                    onDestroy()
                                    break
                                }
                                if (br) break
                                (this as Object).wait(100)
                            }
                        } catch (_: Exception) {
                        }
                    }).start()
                    h2!!.start()
                    val exitValue = Runner.service?.execX(cmd, cmdInfo.name, port)
                    runOnUiThread(Runnable {
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
                    })
                    br = true
                    br2 = true
                } catch (_: RemoteException) {
                }
            })
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
                serverSocket!!.close()
                if (!cmdInfo.keepAlive && !br2) {
                    Thread(Runnable {
                        try {
                            if (ProcAdapter.killPID(pid)) {
                                runOnUiThread(Runnable {
                                    Toast.makeText(
                                        mContext,
                                        R.string.process_the_killing_process_succeeded,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                })
                            } else runOnUiThread(Runnable {
                                Toast.makeText(
                                    mContext,
                                    R.string.process_failed_to_kill_the_process,
                                    Toast.LENGTH_SHORT
                                ).show()
                            })
                        } catch (_: Exception) {
                        }
                    }).start()
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    companion object {
        fun getUsablePort(port: Int): Int {
            var flag = false
            try {
                val socket = Socket("localhost", port)
                flag = true
                socket.close()
            } catch (_: IOException) {
            }
            if (!flag && port == 65536) return -1
            return if (flag) getUsablePort(port + 1) else port
        }
    }
}