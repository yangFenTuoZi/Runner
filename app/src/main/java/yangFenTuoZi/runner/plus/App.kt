package yangFenTuoZi.runner.plus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import yangFenTuoZi.runner.plus.ui.activity.CrashReportActivity;
import yangFenTuoZi.runner.plus.utils.ThemeUtils;

public class App extends Application implements Thread.UncaughtExceptionHandler {

    private final DynamicColorsOptions mDynamicColorsOptions = new DynamicColorsOptions.Builder().build();
    private final List<Activity> activities = new LinkedList<>();
    public int isDark = -1;
    private static App INSTANCE;

    public static App getInstance() {
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        Runner.init();
        boolean isDark = ThemeUtils.isDark(this);
        this.isDark = isDark ? 1 : 0;
        setTheme(ThemeUtils.getTheme(isDark));
        DynamicColors.applyToActivitiesIfAvailable(this, mDynamicColorsOptions);
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    public void removeActivity(Activity activity) {
        activities.remove(activity);
    }

    public void finishApp() {
        for (Activity activity : activities)
            activity.finish();
        activities.clear();
    }

    public DynamicColorsOptions getDynamicColorsOptions() {
        return mDynamicColorsOptions;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Runner.remove();
    }

    @SuppressLint("WrongConstant")
    private void crashHandler(@NonNull Thread t, @NonNull Throwable e) {

        String fileName = "runnerCrash-" + System.currentTimeMillis() + ".log";
        File file;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file = new File(dir, fileName);
        } else {
            file = getExternalFilesDir(fileName);
        }
        assert file != null;

        startActivity(new Intent(this, CrashReportActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("crash_info", Log.getStackTraceString(e))
                .putExtra("crash_file", file.getAbsolutePath()));
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        new Thread(() -> {
            Looper.prepare();
            crashHandler(t, e);
            Looper.loop();
        }).start();
    }
}
