package com.shizuku.runner.plus.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

public class TextViewX extends androidx.appcompat.widget.AppCompatTextView {
    private final Context mContext;

    public TextViewX(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public TextViewX(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public TextViewX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
    }
}
