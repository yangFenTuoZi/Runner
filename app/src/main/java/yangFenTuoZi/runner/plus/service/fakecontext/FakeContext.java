package yangFenTuoZi.runner.plus.service.fakecontext;

import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.system.Os;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public final class FakeContext extends ContextWrapper {
    public static String PACKAGE_NAME = Os.getuid() == 0 ? "root" : "com.android.shell";
    private static final FakeContext INSTANCE = new FakeContext();

    public static FakeContext get() {
        return INSTANCE;
    }

    private FakeContext() {
        super(Workarounds.getSystemContext());
    }

    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    @NonNull
    public String getOpPackageName() {
        return PACKAGE_NAME;
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Override
    @NonNull
    public AttributionSource getAttributionSource() {
        AttributionSource.Builder builder = new AttributionSource.Builder(Os.getuid());
        builder.setPackageName(PACKAGE_NAME);
        return builder.build();
    }

    // @Override to be added on SDK upgrade for Android 14
    @SuppressWarnings("unused")
    public int getDeviceId() {
        return 0;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }
}