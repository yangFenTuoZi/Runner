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

    // TODO 尝试先write完再flash，最后一个可以是$bin/busybox --install -s $bin/applets\nexit\n
    @Override
    public void releaseFile(String packageName, String libraryPath, String apkPath) throws RemoteException {
        try {
            Process process = Runtime.getRuntime().exec("sh");
            OutputStream out = process.getOutputStream();
            out.write(("bin=/data/local/tmp/" + packageName + "/bin\n").getBytes());
            out.flush();
            out.write(("lib=/data/local/tmp/" + packageName + "/lib\n").getBytes());
            out.flush();
            out.write(("etc=/data/local/tmp/" + packageName + "/etc\n").getBytes());
            out.flush();
            out.write(("home=/data/local/tmp/" + packageName + "/home\n").getBytes());
            out.flush();
            out.write(("mkdir -p $bin/applets\n").getBytes());
            out.flush();
            out.write(("mkdir -p $lib\n").getBytes());
            out.flush();
            out.write(("mkdir -p $etc/profile.d\n").getBytes());
            out.flush();
            out.write(("mkdir -p $home\n").getBytes());
            out.flush();
            out.write(("cp -f " + libraryPath + "/libchid.so $bin/chid\n").getBytes());
            out.flush();
            out.write(("cp -f " + libraryPath + "/libbusybox.so $bin/busybox\n").getBytes());
            out.flush();
            out.write(("cp -f " + libraryPath + "/libbash.so $bin/bash\n").getBytes());
            out.flush();
            out.write(("unzip -q -o -j " + apkPath + " assets/profile -d $etc\n").getBytes());
            out.flush();
            out.write(("$bin/busybox --install -s $bin/applets\n").getBytes());
            out.flush();
            out.write(("exit\n").getBytes());
            out.flush();
            out.close();
            process.waitFor();
        } catch (Exception ignored) {
        }
    }

    @Override
    public int execX(String cmd, String packageName, String pipe, int port) throws RemoteException {
        try {
            Runtime.getRuntime().exec(new String[]{"/data/local/tmp/" + packageName + "/bin/busybox", "mkfifo", pipe}).waitFor();
            Socket socket = new Socket("localhost", port);
            Process p = Runtime.getRuntime().exec("/data/local/tmp/" + packageName + "/bin/bash");
            OutputStream out = p.getOutputStream();
            out.write(("export APP_PACKAGE_NAME=" + packageName + "\n").getBytes());
            out.flush();
            out.write((". /data/local/tmp/$APP_PACKAGE_NAME/etc/profile\n").getBytes());
            out.flush();
            out.write(("/data/local/tmp/" + packageName + "/bin/bash >> " + pipe + " 2>> " + pipe + "\n").getBytes());
            out.flush();
            out.write((cmd + "\n").getBytes());
            out.flush();
            out.write(("exit\n").getBytes());
            out.flush();
            out.write(("exitValue=$?\n").getBytes());
            out.flush();
            out.write(("rm -f " + pipe + "\n").getBytes());
            out.flush();
            out.write(("exit $exitValue\n").getBytes());
            out.flush();
            out.close();
            try {
                // 建立socket连接
                OutputStream os = socket.getOutputStream();

                // 获取并返回p(Process)的pid
                String pid = "";
                String process = p.toString();
                for (String i : process.substring(process.indexOf("["), process.indexOf("]")).substring("[".length()).split(", ")) {
                    if (i.split("=")[0].equals("pid")) pid = i.split("=")[1];
                }
                os.write(String.valueOf(pid).getBytes());
                os.write("\n".getBytes());
                os.flush();

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
}
