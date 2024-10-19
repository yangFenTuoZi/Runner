package yangFenTuoZi.runner.plus.ui.dialog;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.ShizukuShellLoader;
import yangFenTuoZi.runner.plus.receiver.OnServiceConnectListener;
import yangFenTuoZi.runner.plus.server.IService;

import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class StartServerDialog extends MaterialAlertDialogBuilder {

    int port;
    int a;
    TextView t1;
    TextView t2;
    Thread h1;
    boolean br = false;
    AlertDialog alertDialog;
    Activity mContext;
    OnServiceConnectListener onServiceConnectListener = new OnServiceConnectListener() {
        @Override
        public void onServiceConnect(IService iService) {
            alertDialog.setCancelable(true);
            Toast.makeText(mContext, "Server starts successfully and the window will close after 5 seconds.", Toast.LENGTH_LONG).show();
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                mContext.runOnUiThread(alertDialog::cancel);
            }).start();
        }
    };

    public StartServerDialog(@NonNull Activity context, int a) {
        super(context);
        mContext = context;
        setView(R.layout.dialog_exec);
        setTitle(mContext.getString(R.string.exec_running));
        setOnDismissListener(dialog -> {
            onDestroy();
        });
        this.a = a;
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
        App.addOnServiceConnectListener(onServiceConnectListener);
        alertDialog.setCancelable(false);
        t1 = alertDialog.findViewById(R.id.exec_title);
        if (t1 != null) {
            t1.setVisibility(View.GONE);
        }
        t2 = alertDialog.findViewById(R.id.exec_msg);
        h1 = new Thread(() -> {
            br = false;
            port = getUsablePort(8400);
            if (port == -1) h1.interrupt();
            int exitValue;

            String shell = switch (a) {
                case 0 -> "su";
                case 1 ->
                        "app_process -Djava.class.path=" + mContext.getApplicationInfo().sourceDir + " /system/bin " + ShizukuShellLoader.class.getName();
                default -> "sh";
            };
            try {
                Process p = Runtime.getRuntime().exec("sh");
                OutputStream out = p.getOutputStream();
                out.write((shell + " 2>&1\n").getBytes());
                out.flush();
                out.write(("sh " + mContext.getExternalFilesDir("") + "/server_starter.sh in_app \"" + mContext.getApplicationInfo().sourceDir + "\"\n").getBytes());
                out.flush();
                out.write("exit\n".getBytes());
                out.flush();
                out.write("exit\n".getBytes());
                out.flush();
                out.close();
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String inline;
                    while ((inline = br.readLine()) != null && !StartServerDialog.this.br) {
                        if (inline.contains("Socket server start."))
                            MainActivity.sendSomethingToServerBySocket("sendBinderToApp");
                        String finalInline = inline;
                        mContext.runOnUiThread(() -> t2.append(finalInline + "\n"));
                    }
                    br.close();
                } catch (Exception ignored) {
                }
                p.waitFor();
                exitValue = p.exitValue();
            } catch (Exception e) {
                exitValue = -1;
            }
            if (t1 != null) {
                mContext.runOnUiThread(() -> t1.setVisibility(View.VISIBLE));
            }
            int finalExitValue = exitValue;
            mContext.runOnUiThread(() -> {
                mContext.runOnUiThread(() -> t1.append(mContext.getString(R.string.exec_return, finalExitValue, mContext.getString(switch (finalExitValue) {
                    case 0 -> R.string.exec_normal;
                    case 127 -> R.string.exec_command_not_found;
                    case 130 -> R.string.exec_ctrl_c_error;
                    case 139 -> R.string.exec_segmentation_error;
                    default -> R.string.exec_other_error;
                }))));
                alertDialog.setTitle(mContext.getString(R.string.exec_finish));
            });
            br = true;
            alertDialog.setCancelable(true);
        });
        h1.start();
        return alertDialog;
    }

    public void onDestroy() {
        br = true;
        h1.interrupt();
        App.removeOnServiceConnectListener(onServiceConnectListener);
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