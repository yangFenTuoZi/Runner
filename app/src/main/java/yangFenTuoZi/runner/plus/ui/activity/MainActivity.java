package yangFenTuoZi.runner.plus.ui.activity;

import android.content.res.AssetManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.ActivityMainBinding;
import yangFenTuoZi.runner.plus.info.Info;
import yangFenTuoZi.runner.plus.server.Server;
import yangFenTuoZi.runner.plus.tools.getAppPath;
import yangFenTuoZi.runner.plus.tools.invokeCli;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;


public class MainActivity extends BaseActivity {

    public boolean isDialogShow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AssetManager assetManager = getAssets();
        InputStream tools;
        try {
            tools = assetManager.open("tools.dex");
            Files.copy(tools, new File(getExternalFilesDir(""), "tools.dex").toPath(), StandardCopyOption.REPLACE_EXISTING);
            tools.close();
        } catch (IOException e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("警告!")
                    .setMessage("请先构建一次 tools 模块\n构建过一次后再构建 app !")
                    .setNegativeButton("退出", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        }
        // server_starter
        try {
            String starter_content = String.format("""
                    #!/system/bin/sh
                    log() {
                        echo "[$(date "%s")] [server_starter] [$1] $2"
                    }
                    log I "Begin"
                    
                    # check uid
                    uid=$(id -u)
                    if [ ! $uid -eq 0 ] && [ ! $uid -eq 2000 ]; then
                        log E "Insufficient permission! Need to be launched by root or shell, but your uid is $uid." >&2
                        exit 255
                    fi
                    
                    if [ "$1" == "in_app" ] && [ ! -z "$2" ]; then
                        APP_PATH="$2"
                    else
                        GET_APP_PATH_DEX="/data/local/tmp/tools.dex"
                        # copy tool.dex
                        tail +48 <"$0" > $GET_APP_PATH_DEX
                    
                        # get app path
                        if [ ! -f "$GET_APP_PATH_DEX" ]; then
                        log E "Cannot find $GET_APP_PATH_DEX." >&2
                        exit 1
                        fi
                        log I "Get app path"
                        APP_PATH="$(app_process -Djava.class.path="$GET_APP_PATH_DEX" /system/bin %s %s)"
                        if [ ! $? -eq 0 ] || [ ! -f "$APP_PATH" ]; then
                            log E "Cannot find the app APK." >&2
                            exit 1
                        fi
                        rm $GET_APP_PATH_DEX
                    fi
                    
                    # start server
                    log I "Start server"
                    export appPath_file="/data/local/tmp/runner/appPath"
                    app_process -Djava.class.path="$APP_PATH" /system/bin --nice-name=runner_server %4$s
                    exitValue=$?
                    while [ $exitValue -eq 10 ]; do
                        appPath=$(cat $appPath_file)
                        [ -f $appPath_file ] && rm $appPath_file
                        app_process -Djava.class.path="$appPath" /system/bin --nice-name=runner_server %4$s restart
                        exitValue=$?
                    done
                    [ -f $appPath_file ] && rm $appPath_file
                    exit $exitValue
                    """, "+%H:%M:%S", getAppPath.class.getName(), Info.APPLICATION_ID, Server.class.getName());
            File starter = new File(getExternalFilesDir(""), "server_starter.sh");
            if (starter.exists()) {
                starter.delete();
                starter.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(starter);
            fileWriter.write(starter_content);
            fileWriter.flush();
            fileWriter.close();
            tools = assetManager.open("tools.dex");
            BufferedInputStream in;
            BufferedOutputStream out;
            in = new BufferedInputStream(tools);
            out = new BufferedOutputStream(new FileOutputStream(starter, true));
            int len;
            byte[] b = new byte[1024];
            while ((len = in.read(b)) != -1) {
                out.write(b, 0, len);
            }
            in.close();
            out.close();
            tools.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // cli
        try {
            String cli_content = String.format("""
                    #!/system/bin/sh
                    DEX="$(dirname "$0")/tools.dex"
                    if [ ! -f "$DEX" ]; then
                        echo "Cannot find $DEX." >&2
                        exit 1
                    fi
                    
                    if [ $(get prop ro.build.version.sdk) -ge 34 ]; then
                      if [ -w $DEX ]; then
                        echo "On Android 14+, app_process cannot load writable dex."
                        echo "Attempting to remove the write permission..."
                        chmod 400 $DEX
                      fi
                      if [ -w $DEX ]; then
                        echo "Cannot remove the write permission of $DEX."
                        echo "You can copy to file to terminal app's private directory (/data/data/<package>, so that remove write permission is possible"
                        exit 1
                      fi
                    fi
                    
                    export APP_PACKAGE_NAME="%s"
                    app_process -Djava.class.path="$DEX" /system/bin %s "$@"
                    """, Info.APPLICATION_ID, invokeCli.class.getName());
            File cli = new File(getExternalFilesDir(""), "runner_cli");
            if (cli.exists()) {
                cli.delete();
                cli.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(cli);
            fileWriter.write(cli_content);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Fragment fragment = Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main));
        NavController navController = ((NavHostFragment) fragment).getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    public static void sendSomethingToServerBySocket(String msg) throws IOException {
        Socket socket = new Socket("localhost", Server.PORT);

        OutputStream out = socket.getOutputStream();
        out.write(msg.getBytes());
        out.close();
        socket.close();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}