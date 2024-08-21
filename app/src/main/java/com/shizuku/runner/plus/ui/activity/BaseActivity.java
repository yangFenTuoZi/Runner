package com.shizuku.runner.plus.ui.activity;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.shizuku.runner.plus.R;

import rikka.core.util.ResourceUtils;
import rikka.material.app.MaterialActivity;

public class BaseActivity extends MaterialActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ResourceUtils.isNightMode(getResources().getConfiguration()))
            setTheme(rikka.material.R.style.Theme_Material3_DynamicColors_Dark_Rikka);
        else
            setTheme(rikka.material.R.style.Theme_Material3_DynamicColors_Light_Rikka);
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
    }


    @Override
    public void onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars();
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }
}
