/*
 * Copyright (C) 2007 The Android Open Source Project
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

package yangfentuozi.runner.app.ui.fragment.terminal.util;

import android.content.SharedPreferences;
import android.content.res.Resources;

import yangfentuozi.runner.app.ui.fragment.terminal.R;

/**
 * Terminal emulator settings
 */
public class TermSettings {
    private SharedPreferences mPrefs;

    private int mFontSize;
    private int mColorId;
    private boolean mUTF8ByDefault;
    private int mUseCookedIME;
    private String mTermType;

    private static final String FONTSIZE_KEY = "fontsize";
    private static final String COLOR_KEY = "color";
    private static final String UTF8_KEY = "utf8_by_default";
    private static final String IME_KEY = "ime";
    private static final String TERMTYPE_KEY = "termtype";

    public static final int WHITE = 0xffffffff;
    public static final int BLACK = 0xff000000;
    public static final int BLUE = 0xff344ebd;
    public static final int GREEN = 0xff00ff00;
    public static final int AMBER = 0xffffb651;
    public static final int RED = 0xffff0113;
    public static final int HOLO_BLUE = 0xff33b5e5;
    public static final int SOLARIZED_FG = 0xff657b83;
    public static final int SOLARIZED_BG = 0xfffdf6e3;
    public static final int SOLARIZED_DARK_FG = 0xff839496;
    public static final int SOLARIZED_DARK_BG = 0xff002b36;
    public static final int LINUX_CONSOLE_WHITE = 0xffaaaaaa;

    // foreground color, background color
    public static final int[][] COLOR_SCHEMES = {
            {BLACK, WHITE},
            {WHITE, BLACK},
            {WHITE, BLUE},
            {GREEN, BLACK},
            {AMBER, BLACK},
            {RED, BLACK},
            {HOLO_BLUE, BLACK},
            {SOLARIZED_FG, SOLARIZED_BG},
            {SOLARIZED_DARK_FG, SOLARIZED_DARK_BG},
            {LINUX_CONSOLE_WHITE, BLACK}
    };

    public TermSettings(Resources res, SharedPreferences prefs) {
        readDefaultPrefs(res);
        readPrefs(prefs);
    }

    private void readDefaultPrefs(Resources res) {
        mFontSize = Integer.parseInt(res.getString(R.string.pref_fontsize_default));
        mColorId = Integer.parseInt(res.getString(R.string.pref_color_default));
        mUTF8ByDefault = res.getBoolean(R.bool.pref_utf8_by_default_default);
        mUseCookedIME = Integer.parseInt(res.getString(R.string.pref_ime_default));
        mTermType = res.getString(R.string.pref_termtype_default);
    }

    public void readPrefs(SharedPreferences prefs) {
        mPrefs = prefs;
        mFontSize = readIntPref(FONTSIZE_KEY, mFontSize, 288);
        mColorId = readIntPref(COLOR_KEY, mColorId, COLOR_SCHEMES.length - 1);
        mUTF8ByDefault = readBooleanPref(UTF8_KEY, mUTF8ByDefault);
        mUseCookedIME = readIntPref(IME_KEY, mUseCookedIME, 1);
        mTermType = readStringPref(TERMTYPE_KEY, mTermType);
        mPrefs = null;  // we leak a Context if we hold on to this
    }

    private int readIntPref(String key, int defaultValue, int maxValue) {
        int val;
        try {
            val = Integer.parseInt(
                    mPrefs.getString(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            val = defaultValue;
        }
        val = Math.max(0, Math.min(val, maxValue));
        return val;
    }

    private String readStringPref(String key, String defaultValue) {
        return mPrefs.getString(key, defaultValue);
    }

    private boolean readBooleanPref(String key, boolean defaultValue) {
        return mPrefs.getBoolean(key, defaultValue);
    }

    public int getFontSize() {
        return mFontSize;
    }

    public int[] getColorScheme() {
        return COLOR_SCHEMES[mColorId];
    }

    public boolean defaultToUTF8Mode() {
        return mUTF8ByDefault;
    }

    public boolean useCookedIME() {
        return (mUseCookedIME != 0);
    }

    public String getTermType() {
        return mTermType;
    }
}
