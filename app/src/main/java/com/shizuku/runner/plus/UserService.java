package com.shizuku.runner.plus;

import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

public class UserService extends IUserService.Stub {

    @Override
    public void destroy() throws RemoteException {
        System.exit(0);
    }

    @Override
    public void exit() throws RemoteException {
        destroy();
    }

    @Override
    public void releaseFile(String packageName, String libraryPath, String apkPath) throws RemoteException {
        try {
            Process process = Runtime.getRuntime().exec("sh");
            OutputStream out = process.getOutputStream();
            out.write(("packageName=" + packageName + "\n").getBytes());
            out.write(("libraryPath=" + libraryPath + "\n").getBytes());
            out.write(("apkPath=" + apkPath + "\n").getBytes());
            out.flush();
            out.write(("""
                    bin=/data/local/tmp/$packageName/bin
                    lib=/data/local/tmp/$packageName/lib
                    etc=/data/local/tmp/$packageName/etc
                    home=/data/local/tmp/$packageName/home
                    mkdir -p $bin/applets
                    mkdir -p $lib
                    mkdir -p $etc/profile.d
                    mkdir -p $home/.pipe
                    cp -f $libraryPath/libchid.so $bin/chid
                    cp -f $libraryPath/libbusybox.so $bin/busybox
                    cp -f $libraryPath/libbash.so $bin/bash
                    unzip -q -o -j $apkPath assets/profile -d $etc
                    $bin/busybox --install -s $bin/applets
                    """).getBytes());
            out.flush();
            out.write(("for i in $HOME/.pipe/.pipe*;do fuser $i||rm -f $i;done\n").getBytes());
            out.flush();
            out.close();
            process.waitFor();
        } catch (Exception ignored) {
        }
    }

    @Override
    public int execX(String cmd, String name, String packageName, String pipe, int port) throws RemoteException {
        try {
            Runtime.getRuntime().exec(new String[]{"/data/local/tmp/" + packageName + "/bin/busybox", "mkfifo", pipe}).waitFor();
            Socket socket = new Socket("localhost", port);
            Process p = Runtime.getRuntime().exec("/data/local/tmp/" + packageName + "/bin/bash");
            OutputStream out = p.getOutputStream();
            out.write(("export APP_PACKAGE_NAME=" + packageName + "\n").getBytes());
            out.write((". /data/local/tmp/$APP_PACKAGE_NAME/etc/profile\n").getBytes());
            out.write(("exec -a RUNNER-bash:" + name + " /data/local/tmp/" + packageName + "/bin/bash >> " + pipe + " 2>> " + pipe + "\n").getBytes());
            out.flush();
            out.write(("echo $$\n").getBytes());
            out.flush();
            out.write((cmd + "\n").getBytes());
            out.flush();
            out.write(("exit\n").getBytes());
            out.write(("exitValue=$?\n").getBytes());
            out.write(("rm -f " + pipe + "\n").getBytes());
            out.write(("exit $exitValue\n").getBytes());
            out.flush();
            out.close();
            try {
                // 建立socket连接
                OutputStream os = socket.getOutputStream();

                // 实时读取并返回存储shell执行结果的管道
                BufferedReader bufferedReader = new BufferedReader(new FileReader(pipe));
                String inline;
                while ((inline = bufferedReader.readLine()) != null) {
                    os.write(inline.getBytes());
                    os.write("\n".getBytes());
                    os.flush();
                }

                // 读完了要关掉
                os.close();
                bufferedReader.close();
                socket.shutdownOutput();
                socket.close();
            } catch (Exception e) {
                Log.e(getClass().getName(), Objects.requireNonNull(e.getMessage()));
            }
            p.waitFor();
            return p.exitValue();
        } catch (Exception e) {
            Log.e(getClass().getName(), Objects.requireNonNull(e.getMessage()));
            return -1;
        }
    }

    @Override
    public String exec(String cmd, String packageName) throws RemoteException {
        try {
            Process process = Runtime.getRuntime().exec("/data/local/tmp/" + packageName + "/bin/bash");
            OutputStream out = process.getOutputStream();
            out.write(("export APP_PACKAGE_NAME=" + packageName + "\n").getBytes());
            out.flush();
            out.write((". /data/local/tmp/$APP_PACKAGE_NAME/etc/profile\n").getBytes());
            out.flush();
            out.write((cmd + "\n").getBytes());
            out.flush();
            out.write(("exit\n").getBytes());
            out.flush();
            out.close();
            StringBuilder sb = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String inline;
            while ((inline = bufferedReader.readLine()) != null) {
                sb.append(inline);
                sb.append("\n");
            }
            bufferedReader.close();
            process.waitFor();
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFreePIPE(String packageName) throws RemoteException {
        try {
            Process process = Runtime.getRuntime().exec("/data/local/tmp/" + packageName + "/bin/bash");
            OutputStream out = process.getOutputStream();
            out.write(("export APP_PACKAGE_NAME=" + packageName + "\n").getBytes());
            out.flush();
            out.write((". /data/local/tmp/$APP_PACKAGE_NAME/etc/profile\n").getBytes());
            out.flush();
            out.write(("sh /data/local/tmp/$APP_PACKAGE_NAME/etc/profile d_p\n").getBytes());
            out.flush();
            out.write(("exit\n").getBytes());
            out.flush();
            out.close();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
