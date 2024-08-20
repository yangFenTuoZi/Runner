package com.shizuku.runner.plus.ui.dialog;

import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shizuku.runner.plus.adapters.ProcessAdapter;
import com.shizuku.runner.plus.ui.activity.MainActivity;
import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.ui.widget.TextViewX;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Random;

public class ExecAlertDialog extends MaterialAlertDialogBuilder {

    int pid, port;
    Intent intent;
    TextView t1;
    TextViewX t2;
    Thread h1, h2;
    boolean br = false;
    AlertDialog alertDialog;
    String pipe;
    ServerSocket serverSocket;
    MainActivity mContext;

    public ExecAlertDialog(@NonNull MainActivity context, Intent intent) {
        super(context);
        mContext = context;
        setView(R.layout.dialog_exec);
        setTitle(mContext.getString(R.string.exec_running));
        setOnDismissListener(dialog -> onDestroy());
        this.intent = intent;
    }

    @NonNull
    @Override
    public AlertDialog create() {
        alertDialog = super.create();
        pipe = "/data/local/tmp/" + mContext.getApplicationInfo().packageName + "/home/.pipe" + (new Random().nextInt(89999) + 10000);
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
        if (mContext.iUserService != null) {
            h1 = new Thread(() -> {
                try {
                    br = false;
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
                                        while ((inline = br.readLine()) != null && !ExecAlertDialog.this.br) {
                                            String finalInline = inline;
                                            if (pid_) {
                                                mContext.runOnUiThread(() -> t2.append(finalInline + "\n"));
                                            } else {
                                                mContext.runOnUiThread(() -> t1.append(String.format(mContext.getString(R.string.exec_pid), Integer.parseInt(finalInline))));
                                                pid_ = true;
                                                pid = Integer.parseInt(inline);
                                                mContext.getSharedPreferences("proc_" + pid, 0).edit()
                                                        .putString("name", intent.getStringExtra("name"))
                                                        .putString("pipe", pipe).apply();
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
                                if (mContext.iUserService == null) {
                                    mContext.runOnUiThread(() -> {
                                        Toast.makeText(mContext, R.string.home_service_is_disconnected, Toast.LENGTH_SHORT).show();
                                        t1.append("\n");
                                        t1.append(String.format(mContext.getString(R.string.exec_return), -1, mContext.getString(R.string.exec_other_error)));
                                        alertDialog.setTitle(mContext.getString(R.string.exec_finish));
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
                    int exitValue = mContext.iUserService.execX(cmd, mContext.getApplicationInfo().packageName, pipe, port);
                    int error = switch (exitValue) {
                        case 0 -> R.string.exec_normal;
                        case 127 -> R.string.exec_command_not_found;
                        case 130 -> R.string.exec_ctrl_c_error;
                        case 139 -> R.string.exec_segmentation_error;
                        default -> R.string.exec_other_error;
                    };
                    mContext.runOnUiThread(() -> {
                        t1.append("\n");
                        t1.append(String.format(mContext.getString(R.string.exec_return), exitValue, mContext.getString(error)));
                        alertDialog.setTitle(mContext.getString(R.string.exec_finish));
                    });
                    br = true;
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
        if (mContext.iUserService != null) {
            try {
                serverSocket.close();
                if (!intent.getBooleanExtra("keep_in_alive", false)) {
                    new Thread(() -> {
                        try {
                            if (ProcessAdapter.killPID(pipe, pid, mContext)) {
                                mContext.deleteSharedPreferences("proc_" + pid);
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