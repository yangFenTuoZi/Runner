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

            int screenHeight = getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;

            Log.d("TermView", "GlobalLayout - screenHeight: " + screenHeight +
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
