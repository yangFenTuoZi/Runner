package com.shizuku.runner.plus.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;

import rikka.core.util.ResourceUtils;
import rikka.material.app.MaterialActivity;

public class BaseActivity extends MaterialActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //设置主题
        if (ResourceUtils.isNightMode(getResources().getConfiguration()))
            setTheme(rikka.material.R.style.Theme_Material3_DynamicColors_Dark_Rikka);
        else
            setTheme(rikka.material.R.style.Theme_Material3_DynamicColors_Light_Rikka);
        super.onCreate(savedInstanceState);

        //隐藏actionBar栏
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
    }


    @Override
    public void onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars();

        //设置状态栏导航栏透明
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }
}
