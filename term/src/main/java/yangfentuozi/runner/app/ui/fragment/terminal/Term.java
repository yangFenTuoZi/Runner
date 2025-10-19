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

package yangfentuozi.runner.app.ui.fragment.terminal;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.emulatorview.UpdateCallback;
import yangfentuozi.runner.app.ui.fragment.terminal.util.SessionList;
import yangfentuozi.runner.app.ui.fragment.terminal.util.TermSettings;

/**
 * A terminal emulator fragment.
 */

public class Term extends Fragment implements UpdateCallback, SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * The ViewFlipper which holds the collection of EmulatorView widgets.
     */
    private TermViewFlipper mViewFlipper;

    /**
     * The name of the ViewFlipper in the resources.
     */
    private static final int VIEW_FLIPPER = R.id.view_flipper;

    private SessionList mTermSessions;

    private TermSettings mSettings;

    private final static int SELECT_TEXT_ID = 0;
    private final static int COPY_ALL_ID = 1;
    private final static int PASTE_ID = 2;
    private final static int SEND_CONTROL_KEY_ID = 3;
    private final static int SEND_FN_KEY_ID = 4;

    private boolean mStopServiceOnFinish = false;

    private Intent TSIntent;

    public static final String EXTRA_WINDOW_ID = "jackpal.androidterm.window_id";
    private int onResumeSelectWindow = -1;

    private PowerManager.WakeLock mWakeLock;


    private TermService mTermService;
    private ServiceConnection mTSConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TermDebug.LOG_TAG, "Bound to TermService");
            TermService.TSBinder binder = (TermService.TSBinder) service;
            mTermService = binder.getService();
            populateViewFlipper();
            populateWindowList();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mTermService = null;
        }
    };

    private WindowListAdapter mWinListAdapter;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        mSettings.readPrefs(sharedPreferences);
    }

    private class EmulatorViewGestureListener extends SimpleOnGestureListener {
        private EmulatorView view;

        public EmulatorViewGestureListener(EmulatorView view) {
            this.view = view;
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            // Let the EmulatorView handle taps if mouse tracking is active
            if (view.isMouseTrackingActive()) return false;

            //Check for link at tap location
            String link = view.getURLat(e.getX(), e.getY());
            if (link != null)
                execURL(link);
            else
                doUIToggle((int) e.getX(), (int) e.getY(), view.getVisibleWidth(), view.getVisibleHeight());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            float absVelocityX = Math.abs(velocityX);
            float absVelocityY = Math.abs(velocityY);
            if (absVelocityX > Math.max(1000.0f, 2.0 * absVelocityY)) {
                // Assume user wanted side to side movement
                if (velocityX > 0) {
                    // Left to right swipe -- previous window
                    mViewFlipper.showPrevious();
                } else {
                    // Right to left swipe -- next window
                    mViewFlipper.showNext();
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Should we use keyboard shortcuts?
     */
    private boolean mUseKeyboardShortcuts;

    /**
     * Intercepts keys before the view/terminal gets it.
     */
    private View.OnKeyListener mKeyListener = new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
            return backkeyInterceptor(keyCode, event) || keyboardShortcuts(keyCode, event);
        }

        /**
         * Keyboard shortcuts (tab management, paste)
         */
        private boolean keyboardShortcuts(int keyCode, android.view.KeyEvent event) {
            if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (!mUseKeyboardShortcuts) {
                return false;
            }
            boolean isCtrlPressed = (event.getMetaState() & KeyEvent.META_CTRL_ON) != 0;
            boolean isShiftPressed = (event.getMetaState() & KeyEvent.META_SHIFT_ON) != 0;

            if (keyCode == KeyEvent.KEYCODE_TAB && isCtrlPressed) {
                if (isShiftPressed) {
                    mViewFlipper.showPrevious();
                } else {
                    mViewFlipper.showNext();
                }

                return true;
            } else if (keyCode == KeyEvent.KEYCODE_N && isCtrlPressed && isShiftPressed) {
                doCreateNewWindow();

                return true;
            } else if (keyCode == KeyEvent.KEYCODE_V && isCtrlPressed && isShiftPressed) {
                doPaste();

                return true;
            } else {
                return false;
            }
        }

        /**
         * Make sure the back button always leaves the application.
         */
        private boolean backkeyInterceptor(int keyCode, android.view.KeyEvent event) {
            return false;
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private View mRootView;
    private View mEmptyStateView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TermDebug.LOG_TAG, "onCreate");

        final SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mSettings = new TermSettings(getResources(), mPrefs);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        TSIntent = new Intent(requireContext(), TermService.class);
        requireActivity().startService(TSIntent);

        PowerManager pm = (PowerManager) requireActivity().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TermDebug.LOG_TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.term_activity, container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewFlipper = view.findViewById(VIEW_FLIPPER);
        if (mWinListAdapter != null)
            mWinListAdapter.setTermViewFlipper(mViewFlipper);

        // Setup empty state view
        mEmptyStateView = view.findViewById(R.id.empty_state_view);
        mEmptyStateView.setOnClickListener(v -> {
            // Create new session when empty state view is clicked
            doCreateNewWindow();
        });

        // Setup bottom toolbar
        setupBottomToolbar();

        updatePrefs();
    }

    private void setupBottomToolbar() {
        // First row buttons - using physical key codes
        mRootView.findViewById(R.id.btn_esc).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_ESCAPE));
        mRootView.findViewById(R.id.btn_tab).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_TAB));
        mRootView.findViewById(R.id.btn_pgup).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_PAGE_UP));
        mRootView.findViewById(R.id.btn_home).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_MOVE_HOME));
        mRootView.findViewById(R.id.btn_up).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_DPAD_UP));
        mRootView.findViewById(R.id.btn_end).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_MOVE_END));
        mRootView.findViewById(R.id.btn_toggle_toolbar).setOnClickListener(v -> toggleBottomToolbar());

        // Second row buttons - using physical key codes
        mRootView.findViewById(R.id.btn_ctrl).setOnClickListener(v -> doSendControlKey());
        mRootView.findViewById(R.id.btn_alt).setOnClickListener(v -> doSendAltKey());
        mRootView.findViewById(R.id.btn_pgdn).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_PAGE_DOWN));
        mRootView.findViewById(R.id.btn_left).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_DPAD_LEFT));
        mRootView.findViewById(R.id.btn_down).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_DPAD_DOWN));
        mRootView.findViewById(R.id.btn_right).setOnClickListener(v -> sendKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT));
        mRootView.findViewById(R.id.btn_switch_window).setOnClickListener(v -> showWindowList());

        // Measure and set the bottom toolbar height to TermViewFlipper
        View toolbar = mRootView.findViewById(R.id.bottom_toolbar);
        toolbar.post(() -> {
            int toolbarHeight = toolbar.getHeight();
            if (toolbarHeight > 0) {
                mViewFlipper.setBottomToolbarHeight(toolbarHeight);
            }
        });
    }

    private void toggleBottomToolbar() {
        View toolbar = mRootView.findViewById(R.id.bottom_toolbar);

        if (toolbar.getVisibility() == View.VISIBLE) {
            // Hide toolbar - will be restored on keyboard show or activity restart
            toolbar.setVisibility(View.GONE);
            mViewFlipper.setBottomToolbarHeight(0);
        }
    }

    private void restoreBottomToolbar() {
        View toolbar = mRootView.findViewById(R.id.bottom_toolbar);
        if (toolbar.getVisibility() != View.VISIBLE) {
            toolbar.setVisibility(View.VISIBLE);

            toolbar.post(() -> {
                int toolbarHeight = toolbar.getHeight();
                if (toolbarHeight > 0) {
                    mViewFlipper.setBottomToolbarHeight(toolbarHeight);
                }
            });
        }
    }

    private void sendKey(String key) {
        TermSession session = getCurrentTermSession();
        if (session != null) {
            session.write(key);
        }
    }

    /**
     * Send a key code to the terminal, processed by TermKeyListener.handleKeyCode()
     * This simulates physical key press and respects terminal type settings.
     */
    private void sendKeyCode(int keyCode) {
        EmulatorView view = getCurrentEmulatorView();
        if (view != null) {
            try {
                // Get the key listener from the emulator view
                // This will properly handle the key code based on terminal type
                view.onKeyDown(keyCode, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
                view.onKeyUp(keyCode, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
            } catch (Exception e) {
                Log.w(TermDebug.LOG_TAG, "Failed to send key code " + keyCode, e);
            }
        }
    }

    private void doSendAltKey() {
        EmulatorView view = getCurrentEmulatorView();
        if (view != null) {
            view.sendFnKey();
        }
    }

    private void showWindowList() {
        if (mWinListAdapter != null) {
            mWinListAdapter.onUpdate();
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.window_list)
                    .setAdapter(mWinListAdapter, (dialog, which) -> {
                        mViewFlipper.setDisplayedChild(which);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.new_session, (dialog, which) -> {
                        dialog.dismiss();
                        doCreateNewWindow();
                    })
                    .setPositiveButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private String makePathFromBundle(Bundle extras) {
        if (extras == null || extras.isEmpty()) {
            return "";
        }

        String[] keys = new String[extras.size()];
        keys = extras.keySet().toArray(keys);
        Collator collator = Collator.getInstance(Locale.US);
        Arrays.sort(keys, collator);

        StringBuilder path = new StringBuilder();
        for (String key : keys) {
            String dir = extras.getString(key);
            if (dir != null && !dir.isEmpty()) {
                path.append(dir);
                path.append(":");
            }
        }

        return path.substring(0, path.length() - 1);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!requireActivity().bindService(TSIntent, mTSConnection, Context.BIND_AUTO_CREATE)) {
            throw new IllegalStateException("Failed to bind to TermService!");
        }
    }

    private void populateViewFlipper() {
        if (mTermService != null) {
            mTermSessions = mTermService.getSessions();

            // Don't automatically create a session when empty
            // Instead, show the empty state view
            if (mTermSessions.isEmpty()) {
                updateEmptyStateVisibility();
                mTermSessions.addCallback(this);
                return;
            }

            mTermSessions.addCallback(this);

            for (TermSession session : mTermSessions) {
                EmulatorView view = createEmulatorView(session);
                mViewFlipper.addView(view);
            }

            updatePrefs();

            if (onResumeSelectWindow >= 0) {
                mViewFlipper.setDisplayedChild(onResumeSelectWindow);
                onResumeSelectWindow = -1;
            }
            mViewFlipper.onResume();
            
            updateEmptyStateVisibility();
        }
    }

    private void populateWindowList() {
        if (mTermSessions != null) {
            int position = mViewFlipper.getDisplayedChild();
            if (mWinListAdapter == null) {
                mWinListAdapter = new WindowListAdapter(mTermSessions);
                mWinListAdapter.setTermViewFlipper(mViewFlipper);

//                mActionBar.setListNavigationCallbacks(mWinListAdapter, mWinListItemSelected);
            } else {
                mWinListAdapter.setSessions(mTermSessions);
            }
            mViewFlipper.addCallback(mWinListAdapter);

//            mActionBar.setSelectedNavigationItem(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);

        if (mStopServiceOnFinish) {
            requireActivity().stopService(TSIntent);
        }
        mTermService = null;
        mTSConnection = null;
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    protected static TermSession createTermSession(Context context, TermSettings settings) throws IOException {
        // Try to use RishTermSession if service is available
        if (RishBinderHolder.service == null) {
            throw new IOException("Failed to create RishTermSession");
        }
        try {
            // Use RishTermSession to connect directly to server with TTY
            // Use bash
            String[] args = new String[]{"/data/local/tmp/runner/usr/bin/bash", "--nice-name", "term", "-l"};
//                String[] args = new String[]{"/system/bin/sh", "-l"};
            String workingDir = "/data/local/tmp/runner/home";
            RishTermSession session = new RishTermSession(args, workingDir, settings);
            session.setProcessExitMessage(context.getString(R.string.process_exit_message));
            Log.i(TermDebug.LOG_TAG, "Using RishTermSession (server mode) with " + args[0]);
            return session;
        } catch (Exception e) {
            Log.w(TermDebug.LOG_TAG, "Failed to create RishTermSession", e);
            throw new IOException(e);
        }
    }

    private TermSession createTermSession() throws IOException {
        TermSettings settings = mSettings;
        TermSession session = createTermSession(requireContext(), settings);
        session.setFinishCallback(mTermService);
        return session;
    }

    private TermView createEmulatorView(TermSession session) {
        DisplayMetrics metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        TermView emulatorView = new TermView(requireContext(), session, metrics);

        emulatorView.setExtGestureListener(new EmulatorViewGestureListener(emulatorView));
        emulatorView.setOnKeyListener(mKeyListener);
        registerForContextMenu(emulatorView);

        // Set keyboard visibility listener
        emulatorView.setKeyboardVisibilityListener(visible -> {
            Log.d(TermDebug.LOG_TAG, "TermView keyboard visibility changed: " + visible);
            if (visible) {
                mHandler.post(this::restoreBottomToolbar);
            }
        });

        return emulatorView;
    }

    private TermSession getCurrentTermSession() {
        SessionList sessions = mTermSessions;
        if (sessions == null) {
            return null;
        } else {
            return sessions.get(mViewFlipper.getDisplayedChild());
        }
    }

    private EmulatorView getCurrentEmulatorView() {
        return (EmulatorView) mViewFlipper.getCurrentView();
    }

    private void updatePrefs() {
        mUseKeyboardShortcuts = mSettings.getUseKeyboardShortcutsFlag();

        DisplayMetrics metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mViewFlipper.updatePrefs(mSettings);

        for (View v : mViewFlipper) {
            ((EmulatorView) v).setDensity(metrics);
            ((TermView) v).updatePrefs(mSettings);
        }

        if (mTermSessions != null) {
            for (TermSession session : mTermSessions) {
                ((RishTermSession) session).updatePrefs(mSettings);
            }
        }

        int orientation = mSettings.getScreenOrientation();
        int o = 0;
        if (orientation == 0) {
            o = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        } else if (orientation == 1) {
            o = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else if (orientation == 2) {
            o = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            /* Shouldn't be happened. */
        }
        requireActivity().setRequestedOrientation(o);
    }

    @Override
    public void onPause() {
        super.onPause();

        /* Explicitly close the input method
           Otherwise, the soft keyboard could cover up whatever activity takes
           our place */
        final IBinder token = mViewFlipper.getWindowToken();
        new Thread(() -> {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(token, 0);
        }).start();
    }

    @Override
    public void onStop() {
        mViewFlipper.onPause();
        if (mTermSessions != null) {
            mTermSessions.removeCallback(this);

            if (mWinListAdapter != null) {
                mTermSessions.removeCallback(mWinListAdapter);
                mTermSessions.removeTitleChangedListener(mWinListAdapter);
                mViewFlipper.removeCallback(mWinListAdapter);
            }
        }

        mViewFlipper.removeAllViews();

        requireActivity().unbindService(mTSConnection);

        super.onStop();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        EmulatorView v = (EmulatorView) mViewFlipper.getCurrentView();
        if (v != null) {
            v.updateSize(false);
        }

        if (mWinListAdapter != null) {
            // Force Android to redraw the label in the navigation dropdown
            mWinListAdapter.notifyDataSetChanged();
        }
    }

    private void doCreateNewWindow() {
        if (mTermSessions == null) {
            Log.w(TermDebug.LOG_TAG, "Couldn't create new window because mTermSessions == null");
            return;
        }

        try {
            TermSession session = createTermSession();

            mTermSessions.add(session);

            TermView view = createEmulatorView(session);
            view.updatePrefs(mSettings);

            mViewFlipper.addView(view);
            mViewFlipper.setDisplayedChild(mViewFlipper.getChildCount() - 1);
            
            // Update empty state visibility
            updateEmptyStateVisibility();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to create a session", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmCloseWindow() {
        final MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(requireContext());
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.confirm_window_close_message);
        final Runnable closeWindow = this::doCloseWindow;
        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            mHandler.post(closeWindow);
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void doCloseWindow() {
        if (mTermSessions == null) {
            return;
        }

        EmulatorView view = getCurrentEmulatorView();
        if (view == null) {
            return;
        }
        TermSession session = mTermSessions.remove(mViewFlipper.getDisplayedChild());
        view.onPause();
        session.finish();
        mViewFlipper.removeView(view);
        if (!mTermSessions.isEmpty()) {
            mViewFlipper.showNext();
        }
        
        // Update empty state visibility when closing last session
        updateEmptyStateVisibility();
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.edit_text);
        menu.add(0, SELECT_TEXT_ID, 0, R.string.select_text);
        menu.add(0, COPY_ALL_ID, 0, R.string.copy_all);
        menu.add(0, PASTE_ID, 0, R.string.paste);
        menu.add(0, SEND_CONTROL_KEY_ID, 0, R.string.send_control_key);
        menu.add(0, SEND_FN_KEY_ID, 0, R.string.send_fn_key);
        if (!canPaste()) {
            menu.getItem(PASTE_ID).setEnabled(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return switch (item.getItemId()) {
            case SELECT_TEXT_ID -> {
                getCurrentEmulatorView().toggleSelectingText();
                yield true;
            }
            case COPY_ALL_ID -> {
                doCopyAll();
                yield true;
            }
            case PASTE_ID -> {
                doPaste();
                yield true;
            }
            case SEND_CONTROL_KEY_ID -> {
                doSendControlKey();
                yield true;
            }
            case SEND_FN_KEY_ID -> {
                doSendFnKey();
                yield true;
            }
            default -> super.onContextItemSelected(item);
        };
    }

    // Fragment doesn't handle onKeyUp - moved to Activity level or use OnBackPressedCallback
    // Can be re-implemented using requireActivity().getOnBackPressedDispatcher() if needed

    // Called when the list of sessions changes
    public void onUpdate() {
        SessionList sessions = mTermSessions;
        if (sessions == null) {
            return;
        }

        if (sessions.isEmpty()) {
            // Show empty state instead of finishing
            updateEmptyStateVisibility();
        } else if (sessions.size() < mViewFlipper.getChildCount()) {
            for (int i = 0; i < mViewFlipper.getChildCount(); ++i) {
                EmulatorView v = (EmulatorView) mViewFlipper.getChildAt(i);
                if (!sessions.contains(v.getTermSession())) {
                    v.onPause();
                    mViewFlipper.removeView(v);
                    --i;
                }
            }
            updateEmptyStateVisibility();
        }
    }

    private boolean canPaste() {
        var clip = requireActivity().getSystemService(ClipboardManager.class);
        var clipDescription = clip.getPrimaryClipDescription();
        return clip.hasPrimaryClip() && clipDescription != null && clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
    }

    private void doPreferences() {
        startActivity(new Intent(requireContext(), TermPreferences.class));
    }

    private void doResetTerminal() {
        TermSession session = getCurrentTermSession();
        if (session != null) {
            session.reset();
        }
    }

    private void doCopyAll() {
        requireActivity().getSystemService(ClipboardManager.class)
                .setPrimaryClip(ClipData.newPlainText("terminalText", getCurrentTermSession().getTranscriptText().trim()));
    }

    private void doPaste() {
        if (!canPaste()) {
            return;
        }
        var primaryClip = requireActivity().getSystemService(ClipboardManager.class).getPrimaryClip();
        if (primaryClip != null && primaryClip.getItemCount() > 0) {
            ClipData.Item item = primaryClip.getItemAt(0);
            getCurrentTermSession().write(item.getText().toString());
        }
    }

    private void doSendControlKey() {
        getCurrentEmulatorView().sendControlKey();
    }

    private void doSendFnKey() {
        getCurrentEmulatorView().sendFnKey();
    }

    private void doDocumentKeys() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireContext());
        Resources r = getResources();
        dialog.setTitle(r.getString(R.string.control_key_dialog_title));
        dialog.setMessage(
                formatMessage(mSettings.getControlKeyId(), TermSettings.CONTROL_KEY_ID_NONE,
                        r, R.array.control_keys_short_names,
                        R.string.control_key_dialog_control_text,
                        R.string.control_key_dialog_control_disabled_text, "CTRLKEY")
                        + "\n\n" +
                        formatMessage(mSettings.getFnKeyId(), TermSettings.FN_KEY_ID_NONE,
                                r, R.array.fn_keys_short_names,
                                R.string.control_key_dialog_fn_text,
                                R.string.control_key_dialog_fn_disabled_text, "FNKEY"));
        dialog.show();
    }

    private String formatMessage(int keyId, int disabledKeyId,
                                 Resources r, int arrayId,
                                 int enabledId,
                                 int disabledId, String regex) {
        if (keyId == disabledKeyId) {
            return r.getString(disabledId);
        }
        String[] keyNames = r.getStringArray(arrayId);
        String keyName = keyNames[keyId];
        String template = r.getString(enabledId);
        return template.replaceAll(regex, keyName);
    }

    private void doToggleSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

    }

    private void doToggleWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        } else {
            mWakeLock.acquire();
        }
        requireActivity().invalidateOptionsMenu();
    }

    private void doUIToggle(int x, int y, int width, int height) {
        doToggleSoftKeyboard();
        getCurrentEmulatorView().requestFocus();
    }

    /**
     * Send a URL up to Android to be handled by a browser.
     *
     * @param link The URL to be opened.
     */
    private void execURL(String link) {
        Uri webLink = Uri.parse(link);
        Intent openLink = new Intent(Intent.ACTION_VIEW, webLink);
        PackageManager pm = requireActivity().getPackageManager();
        List<ResolveInfo> handlers = pm.queryIntentActivities(openLink, 0);
        if (!handlers.isEmpty())
            startActivity(openLink);
    }

    /**
     * Update the visibility of empty state view and bottom toolbar based on session count
     */
    private void updateEmptyStateVisibility() {
        if (mEmptyStateView == null || mTermSessions == null) {
            return;
        }

        View toolbar = mRootView.findViewById(R.id.bottom_toolbar);
        
        if (mTermSessions.isEmpty()) {
            // Show empty state, hide toolbar
            mEmptyStateView.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.GONE);
            mViewFlipper.setBottomToolbarHeight(0);
        } else {
            // Hide empty state, show toolbar
            mEmptyStateView.setVisibility(View.GONE);
            toolbar.setVisibility(View.VISIBLE);
            
            // Restore toolbar height
            toolbar.post(() -> {
                int toolbarHeight = toolbar.getHeight();
                if (toolbarHeight > 0) {
                    mViewFlipper.setBottomToolbarHeight(toolbarHeight);
                }
            });
        }
    }
}
