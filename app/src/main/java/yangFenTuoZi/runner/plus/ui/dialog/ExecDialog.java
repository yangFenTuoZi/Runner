package yangFenTuoZi.runner.plus.ui.dialog;

import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.adapters.ProcessAdapter;
import yangFenTuoZi.runner.plus.server.Server;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class ExecDialog extends MaterialAlertDialogBuilder {

    int pid, port;
    Intent intent;
    TextView t1;
    TextView t2;
    Thread h1, h2;
    boolean br = false, br2 = false;
    AlertDialog alertDialog;
    ServerSocket serverSocket;
    MainActivity mContext;

    public ExecDialog(@NonNull MainActivity context, Intent intent) {
        super(context);
        mContext = context;
        setView(R.layout.dialog_exec);
        setTitle(mContext.getString(R.string.exec_running));
        setOnDismissListener(dialog -> {
            onDestroy();
            mContext.isDialogShow = false;
        });
        this.intent = intent;
    }

    @NonNull
    @Override
    public AlertDialog create() {
        alertDialog = super.create();
        return alertDialog;
    }

    @Override
    public AlertDialog show() {
        super.show();
        t1 = alertDialog.findViewById(R.id.exec_title);
        t2 = alertDialog.findViewById(R.id.exec_msg);
        Objects.requireNonNull(t2).requestFocus();
        t2.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                alertDialog.cancel();
            return false;
        });
        //子线程执行命令，否则UI线程执行就会导致UI卡住动不了
        String cmd;
        if (intent.getBooleanExtra("chid", false))
            cmd = "chid " + intent.getStringExtra("ids") + " " + intent.getStringExtra("command");
        else
            cmd = intent.getStringExtra("command");
        if (App.pingServer()) {
            h1 = new Thread(() -> {
                try {
                    port = getUsablePort(8400);
                    if (port == -1) h1.interrupt();
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
                                                mContext.runOnUiThread(() -> t2.append(finalInline + "\n"));
                                            } else {
                                                try {
                                                    int p = Integer.parseInt(finalInline);
                                                    mContext.runOnUiThread(() -> t1.append(mContext.getString(R.string.exec_pid, p) + "\n"));
                                                    pid_ = true;
                                                    pid = p;
                                                } catch (Exception e) {
                                                    mContext.runOnUiThread(() -> t2.append(finalInline + "\n"));
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
                                    mContext.runOnUiThread(() -> {
                                        Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
                                        t1.append(mContext.getString(R.string.exec_return, -1, mContext.getString(R.string.exec_other_error)));
                                        alertDialog.setTitle(mContext.getString(R.string.exec_finish));
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
                    int exitValue = App.iService.execX(cmd, intent.getStringExtra("name"), port);
                    mContext.runOnUiThread(() -> {
                        t1.append(mContext.getString(R.string.exec_return, exitValue, mContext.getString(switch (exitValue) {
                            case 0 -> R.string.exec_normal;
                            case 127 -> R.string.exec_command_not_found;
                            case 130 -> R.string.exec_ctrl_c_error;
                            case 139 -> R.string.exec_segmentation_error;
                            default -> R.string.exec_other_error;
                        })));
                        alertDialog.setTitle(mContext.getString(R.string.exec_finish));
                    });
                    br = true;
                    br2 = true;
                } catch (RemoteException ignored) {
                }
            });
            h1.start();
        }
        return alertDialog;
    }

    public void onDestroy() {
        br = true;
        h2.interrupt();
        h1.interrupt();
        if (App.pingServer()) {
            try {
                serverSocket.close();
                if (!intent.getBooleanExtra("keep_in_alive", false) && !br2) {
                    new Thread(() -> {
                        try {
                            if (ProcessAdapter.killPID(pid)) {
                                mContext.runOnUiThread(() -> Toast.makeText(mContext, R.string.process_the_killing_process_succeeded, Toast.LENGTH_SHORT).show());
                            } else
                                mContext.runOnUiThread(() -> Toast.makeText(mContext, R.string.process_failed_to_kill_the_process, Toast.LENGTH_SHORT).show());
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