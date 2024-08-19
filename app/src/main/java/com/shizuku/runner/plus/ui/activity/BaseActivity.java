package com.shizuku.runner.plus.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.shizuku.runner.plus.R;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Runner);
        super.onCreate(savedInstanceState);
    }
}
