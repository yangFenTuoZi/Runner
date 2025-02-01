package yangFenTuoZi.runner.plus.ui.dialog;

import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.adapters.ProcAdapter;
import yangFenTuoZi.runner.plus.cli.CmdInfo;
import yangFenTuoZi.runner.plus.databinding.DialogExecBinding;
import yangFenTuoZi.runner.plus.ui.activity.BaseActivity;

public class ExecDialogBuilder extends BaseDialogBuilder {

    int pid, port;
    CmdInfo cmdInfo;
    Thread h1, h2;
    boolean br = false, br2 = false;
    ServerSocket serverSocket;
    BaseActivity mContext;
    DialogExecBinding binding;

    public ExecDialogBuilder(@NonNull BaseActivity context, CmdInfo cmdInfo) throws DialogShowException {
        super(context);
        mContext = context;
        binding = DialogExecBinding.inflate(LayoutInflater.from(mContext));
        setView(binding.getRoot());
        setTitle(getString(R.string.exec_running));
        setOnDismissListener(dialog -> onDestroy());
        this.cmdInfo = cmdInfo;
    }

    @Override
    public AlertDialog show() {
        super.show();
        binding.execMsg.requestFocus();
        binding.execMsg.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                getAlertDialog().cancel();
            return false;
        });
        //子线程执行命令，否则UI线程执行就会导致UI卡住动不了
        String cmd;
        if (cmdInfo.useChid)
            cmd = "chid " + cmdInfo.ids + " " + cmdInfo.command;
        else
            cmd = cmdInfo.command;
        if (App.pingServer()) {
            h1 = new Thread(() -> {
                try {
                    port = getUsablePort(8400);
                    if (port == -1) return;
                    h2 = new Thread(() -> {
                        try {
                            serverSocket = new ServerSocket(port);
                            while (!br) {
                                Socket socket = serverSocket.accept();
                                Thread thread = new Thread(() -> {
                                    try {
                                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                        String inline;
                                        boolean pid_ = false;
                                        while ((inline = br.readLine()) != null) {
                                            String finalInline = inline;
                                            if (pid_) {
                                                runOnUiThread(() -> binding.execMsg.append(finalInline + "\n"));
                                            } else {
                                                try {
                                                    int p = Integer.parseInt(finalInline);
                                                    runOnUiThread(() -> binding.execTitle.append(getString(R.string.exec_pid, p) + "\n"));
                                                    pid_ = true;
                                                    pid = p;
                                                } catch (Exception e) {
                                                    runOnUiThread(() -> binding.execMsg.append(finalInline + "\n"));
                                                }
                                            }
                                        }
                                        br.close();
                                        socket.close();
                                    } catch (Exception ignored) {
                                    }
                                });
                                thread.start();
                            }
                            serverSocket.close();
                        } catch (Exception e) {
                            Log.e(getClass().getName(), Objects.requireNonNull(e.getMessage()));
                        }
                    });
                    new Thread(() -> {
                        try {
                            while (true) {
                                if (!App.pingServer()) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                                        binding.execTitle.append(getString(R.string.exec_return, -1, getString(R.string.exec_other_error)));
                                        getAlertDialog().setTitle(getString(R.string.exec_finish));
                                        br2 = true;
                                    });
                                    onDestroy();
                                    break;
                                }
                                if (br) break;
                                wait(100);
                            }
                        } catch (Exception ignored) {
                        }
                    }).start();
                    h2.start();
                    int exitValue = App.iService.execX(cmd, cmdInfo.name, port);
                    runOnUiThread(() -> {
                        binding.execTitle.append(getString(R.string.exec_return, exitValue, getString(switch (exitValue) {
                            case 0 -> R.string.exec_normal;
                            case 127 -> R.string.exec_command_not_found;
                            case 130 -> R.string.exec_ctrl_c_error;
                            case 139 -> R.string.exec_segmentation_error;
                            default -> R.string.exec_other_error;
                        })));
                        getAlertDialog().setTitle(getString(R.string.exec_finish));
                    });
                    br = true;
                    br2 = true;
                } catch (RemoteException ignored) {
                }
            });
            h1.start();
        }
        return getAlertDialog();
    }

    public void onDestroy() {
        br = true;
        h2.interrupt();
        h1.interrupt();
        if (App.pingServer()) {
            try {
                serverSocket.close();
                if (!cmdInfo.keepAlive && !br2) {
                    new Thread(() -> {
                        try {
                            if (ProcAdapter.killPID(pid)) {
                                runOnUiThread(() -> Toast.makeText(mContext, R.string.process_the_killing_process_succeeded, Toast.LENGTH_SHORT).show());
                            } else
                                runOnUiThread(() -> Toast.makeText(mContext, R.string.process_failed_to_kill_the_process, Toast.LENGTH_SHORT).show());
                        } catch (Exception ignored) {
                        }
                    }).start();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int getUsablePort(int port) {
        boolean flag = false;
        try {
            Socket socket = new Socket("localhost", port);
            flag = true;
            socket.close();
        } catch (IOException ignored) {
        }
        if (!flag && port == 65536)
            return -1;
        return flag ? getUsablePort(port + 1) : port;
    }

}