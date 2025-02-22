package yangFenTuoZi.runner.plus.ui.activity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import yangFenTuoZi.runner.plus.BuildConfig;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.databinding.ActivityMainBinding;
import yangFenTuoZi.runner.plus.server.Server;
import a.Cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;


public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                    
                    # start server
                    log I "Start server"
                    exitValue=10
                    while [ $exitValue -eq 10 ]; do
                        app_process -Djava.class.path="$(pm path %s)" /system/bin --nice-name=runner_server %s
                        exitValue=$?
                    done
                    exit $exitValue
                    """, "+%H:%M:%S", BuildConfig.APPLICATION_ID, Server.class.getName());
            File starter = new File(getExternalFilesDir(""), "server_starter.sh");
            if (starter.exists()) {
                starter.delete();
                starter.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(starter);
            fileWriter.write(starter_content);
            fileWriter.flush();
            fileWriter.close();
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
                    
                    uid=$(id -u)
                    if [ $(get prop ro.build.version.sdk) -ge 34 ] && [ ! $uid -eq 0 ] && [ ! $uid -eq 2000 ]; then
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
                    
                    app_process -Djava.class.path="$DEX" /system/bin %s "$@"
                    """, Cli.class.getName());
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

        var badge = binding.navView;
    }

    public static void sendSomethingToServerBySocket(String msg) throws IOException {
        Socket socket = new Socket("localhost", Server.PORT);

        OutputStream out = socket.getOutputStream();
        out.write(msg.getBytes());
        out.close();
        socket.close();
    }
}