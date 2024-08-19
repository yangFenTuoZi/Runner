package com.shizuku.runner.plus.terminal;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.ui.widget.TerminalTextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class TerminalView extends LinearLayout {
    boolean br;
    public InputStream mInputStream;
    private Thread thread_read;
    private final Context mContext;
    private TerminalTextView mTextView;
    private Button BUTTON_ESC, BUTTON_TAB, BUTTON_PgUp, BUTTON_HOME, BUTTON_UP, BUTTON_END, BUTTON_CTRL, BUTTON_ALT, BUTTON_PgDn, BUTTON_LEFT, BUTTON_DOWN, BUTTON_RIGHT;

    public TerminalView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public TerminalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public TerminalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    protected TerminalView.Handler_append mHandler = new TerminalView.Handler_append(this);

    public static class Handler_append extends Handler {
        private final TerminalView mOuter;

        public Handler_append(TerminalView terminalView) {
            mOuter = terminalView;
        }

        @Override
        public void handleMessage(Message msg) {
            mOuter.mTextView.append((String) msg.obj);
        }
    }

    public void start() {
        if (mInputStream == null) return;
        br = false;
        thread_read = new Thread(() -> {
            try {
                BufferedReader mReader = new BufferedReader(new InputStreamReader(mInputStream));
                String inline;
                while ((inline = mReader.readLine()) != null) {
                    if (br) break;
                    Message msg = new Message();
                    msg.obj = inline.isEmpty() ? "\n" : inline + "\n";
                    mHandler.sendMessage(msg);
                }
                mReader.close();
            } catch (Exception ignored) {
            }
        });
    }

    private void init() {
        initLayout();
    }

    private void initLayout() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.terminal, this, false);
        addView(view);
        mTextView = findViewById(R.id.terminal_view_text);
        BUTTON_ESC = findViewById(R.id.terminal_view_esc);
        BUTTON_TAB = findViewById(R.id.terminal_view_tab);
        BUTTON_PgUp = findViewById(R.id.terminal_view_pgup);
        BUTTON_HOME = findViewById(R.id.terminal_view_home);
        BUTTON_UP = findViewById(R.id.terminal_view_up);
        BUTTON_END = findViewById(R.id.terminal_view_end);
        BUTTON_CTRL = findViewById(R.id.terminal_view_ctrl);
        BUTTON_ALT = findViewById(R.id.terminal_view_alt);
        BUTTON_PgDn = findViewById(R.id.terminal_view_pgdn);
        BUTTON_LEFT = findViewById(R.id.terminal_view_left);
        BUTTON_DOWN = findViewById(R.id.terminal_view_down);
        BUTTON_RIGHT = findViewById(R.id.terminal_view_right);
    }

    public void stop() {
        br = true;
        new Handler().postDelayed(() -> {
            try {
                thread_read.interrupt();
            } catch (Exception ignored) {
            }
        }, 1000);
    }
}
