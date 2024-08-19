package com.shizuku.runner.plus.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;

import com.shizuku.runner.plus.R;

import java.util.Objects;

public class TerminalTextView extends TextViewX {
    private final Context mContext;
    public TerminalTextView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public TerminalTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public TerminalTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        setBackground(null);
        setTypeface(Typeface.MONOSPACE);
        setTextColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setTextCursorDrawable(AppCompatResources.getDrawable(mContext, R.drawable.cursor_style));
        }
    }

    public void setTextSize(float size) {
        super.setTextSize(size);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Drawable drawable = AppCompatResources.getDrawable(mContext, R.drawable.cursor_style);
            Objects.requireNonNull(drawable).setBounds(0,0,(int)size,0);
            setTextCursorDrawable(drawable);
        }
    }
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Drawable drawable = AppCompatResources.getDrawable(mContext, R.drawable.cursor_style);
            Objects.requireNonNull(drawable).setBounds(0,0,(int)size,0);
            setTextCursorDrawable(drawable);
        }
    }

}
