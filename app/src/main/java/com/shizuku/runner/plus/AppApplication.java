package com.shizuku.runner.plus;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
