/*
 * Copyright (C) 2012 Steven Luo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yangfentuozi.runner.term;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;
import yangfentuozi.runner.term.util.TermSettings;

public class TermView extends EmulatorView {
    private KeyboardVisibilityListener mKeyboardListener;
    private boolean mWasKeyboardVisible = false;
    
    public interface KeyboardVisibilityListener {
        void onKeyboardVisibilityChanged(boolean visible);
    }
    
    public TermView(Context context, TermSession session, DisplayMetrics metrics) {
        super(context, session, metrics);
        setupKeyboardListener();
    }
    
    private void setupKeyboardListener() {
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            getWindowVisibleDisplayFrame(r);

            // 使用视图自己的高度，而不是 root view 的高度
            // 这样可以正确处理有 Toolbar 和 BottomNavigation 的情况
            int viewHeight = getHeight();
            
            // 获取视图在屏幕上的位置
            int[] location = new int[2];
            getLocationOnScreen(location);
            int viewTop = location[1];
            
            // 计算键盘高度：视图底部到可见区域底部的距离
            int viewBottom = viewTop + viewHeight;
            int keypadHeight = viewBottom - r.bottom;

            // 键盘可见的判断：键盘高度超过视图高度的 15%
            boolean isKeyboardVisible = keypadHeight > viewHeight * 0.15;

            Log.d("TermView", "GlobalLayout - viewHeight: " + viewHeight +
                ", viewTop: " + viewTop +
                ", viewBottom: " + viewBottom +
                ", r.bottom: " + r.bottom +
                ", keypadHeight: " + keypadHeight +
                ", isKeyboardVisible: " + isKeyboardVisible +
                ", wasVisible: " + mWasKeyboardVisible);

            if (isKeyboardVisible != mWasKeyboardVisible) {
                mWasKeyboardVisible = isKeyboardVisible;
                Log.d("TermView", "Keyboard visibility changed to: " + isKeyboardVisible);
                if (mKeyboardListener != null) {
                    mKeyboardListener.onKeyboardVisibilityChanged(isKeyboardVisible);
                }
            }
        });
    }
    
    public void setKeyboardVisibilityListener(KeyboardVisibilityListener listener) {
        mKeyboardListener = listener;
        Log.d("TermView", "Keyboard listener set: " + (listener != null ? "exists" : "null"));
    }

    public void updatePrefs(TermSettings settings, ColorScheme scheme) {
        if (scheme == null) {
            scheme = new ColorScheme(settings.getColorScheme());
        }

        setTextSize(settings.getFontSize());
        setUseCookedIME(settings.useCookedIME());
        setColorScheme(scheme);
        // TODO 彻底移除它
        setBackKeyCharacter(0);
        setAltSendsEsc(false);
        setControlKeyCode(-1);
        setFnKeyCode(-1);
        setMouseTracking(false);

        setTermType(settings.getTermType());
    }

    public void updatePrefs(TermSettings settings) {
        updatePrefs(settings, null);
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().toString() + '(' + getTermSession() + ')';
    }
}
