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

package yangfentuozi.runner.term;

import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.TermSession;
import rikka.rish.IRishService;
import rikka.rish.RishConstants;
import yangfentuozi.runner.term.util.TermSettings;

/**
 * A terminal session that connects to the server via RishService.
 * This allows the terminal to run with elevated privileges through Shizuku.
 * <p>
 * The data flow is:
 * 1. Client creates pipes for stdin/stdout
 * 2. Server (RishService) creates PTY and forks process
 * 3. Server transfers data between PTY and pipes
 * 4. Client transfers data between pipes and EmulatorView
 */
public class RishTermSession extends TermSession {
    private static final String TAG = "RishTermSession";

    private Thread mWatcherThread;
    private IRishService mRishService;
    private FileDescriptor[] mStdin;
    private FileDescriptor[] mStdout;
    private int pid;

    private final long createdAt;
    private String mProcessExitMessage;
    private boolean mProcessExited = false;

    private static final int PROCESS_EXITED = 1;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    TermSettings mSettings;

    public RishTermSession(String[] args, String workingDir, TermSettings settings) throws IOException {
        super(false);  // Don't exit on EOF
        updatePrefs(settings);

        this.createdAt = System.currentTimeMillis();

        try {
            initializeSession(args, workingDir);
        } catch (RemoteException | ErrnoException e) {
            Log.e(TAG, "Failed to initialize session", e);
            throw new IOException("Failed to initialize terminal session", e);
        }
    }

    public void updatePrefs(TermSettings settings) {
        mSettings = settings;
        setColorScheme(new ColorScheme(settings.getColorScheme()));
        setDefaultUTF8Mode(settings.defaultToUTF8Mode());
    }

    private void initializeSession(String[] args, String workingDir) throws RemoteException, ErrnoException, IOException {
        if (RishBinderHolder.service == null) {
            throw new IOException("RishService not available");
        }

        mRishService = RishBinderHolder.service;
        if (mRishService == null) {
            throw new IOException("Failed to get RishService interface");
        }

        // Create pipes for stdin/stdout communication
        // mStdin[0] = read end, mStdin[1] = write end
        // mStdout[0] = read end, mStdout[1] = write end
        mStdin = Os.pipe();
        mStdout = Os.pipe();

        Log.d(TAG, "Created pipes - stdin[" + mStdin[0] + "," + mStdin[1] + "] stdout[" + mStdout[0] + "," + mStdout[1] + "]");

        // Setup terminal input/output streams
        // TermSession writes to mStdin[1] (which server reads from mStdin[0])
        // TermSession reads from mStdout[0] (which server writes to mStdout[1])
        setTermOut(new ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.dup(mStdin[1])));
        setTermIn(new ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(mStdout[0])));

        // Get current environment and add TERM variable
        String termType = mSettings != null ? mSettings.getTermType() : "xterm-256color";
        String[] env = System.getenv().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        
        // Add or replace TERM environment variable
        boolean termSet = false;
        for (int i = 0; i < env.length; i++) {
            if (env[i].startsWith("TERM=")) {
                env[i] = "TERM=" + termType;
                termSet = true;
                break;
            }
        }
        if (!termSet) {
            String[] newEnv = new String[env.length + 1];
            System.arraycopy(env, 0, newEnv, 0, env.length);
            newEnv[env.length] = "TERM=" + termType;
            env = newEnv;
        }
        
        Log.d(TAG, "TERM environment variable set to: " + termType);

        // Create host on server side with TTY mode enabled for all streams
        byte tty = (byte) (RishConstants.ATTY_IN | RishConstants.ATTY_OUT | RishConstants.ATTY_ERR);

        Log.d(TAG, "Creating host with args: " + String.join(" ", args) + ", workingDir: " + workingDir);

        try {
            // Pass read end of stdin and write end of stdout to server
            pid = mRishService.createHost(
                    args,
                    env,
                    workingDir,
                    tty,
                    ParcelFileDescriptor.dup(mStdin[0]),
                    ParcelFileDescriptor.dup(mStdout[1]),
                    null  // stderr is merged with stdout when using TTY
            );
            Log.d(TAG, "Host created successfully");
        } finally {
            // Close our copies of the file descriptors that server now owns
            closeFd(mStdin, 0);
            closeFd(mStdout, 1);
        }

        // Start watcher thread to monitor process exit
        // We'll check the exit code periodically
        mWatcherThread = new Thread(() -> {
            try {
                Log.i(TAG, "Waiting for process to exit...");
                int exitCode = Integer.MAX_VALUE;
                // Poll for exit code (getExitCode returns Integer.MAX_VALUE while process is running)
                while (isRunning() && exitCode == Integer.MAX_VALUE) {
                    try {
                        exitCode = mRishService.getExitCode(pid);
                        if (exitCode != Integer.MAX_VALUE) {
                            // Process has exited
                            break;
                        }
                        Thread.sleep(200);  // Poll every 200ms
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (RemoteException e) {
                        Log.w(TAG, "Failed to get exit code, service might be dead", e);
                        break;
                    }
                }

                Log.i(TAG, "Process exited with code: " + exitCode);
                if (isRunning()) {
                    int finalExitCode = exitCode;
                    mHandler.post(() -> onProcessExit(finalExitCode));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in watcher thread", e);
                if (isRunning()) {
                    mHandler.post(() -> onProcessExit(Integer.MIN_VALUE));
                }
            }
        });
        mWatcherThread.setName("RishTermSession watcher");
    }

    @Override
    public void initializeEmulator(int columns, int rows) {
        Log.d(TAG, "initializeEmulator: " + columns + "x" + rows);
        super.initializeEmulator(columns, rows);

        // Start the watcher thread
        if (mWatcherThread != null && !mWatcherThread.isAlive()) {
            mWatcherThread.start();
        }

        // Update window size on server
        updateWindowSize(rows, columns);
    }

    @Override
    public void updateSize(int columns, int rows) {
        Log.d(TAG, "updateSize: " + columns + "x" + rows);
        // Inform the server of our new size
        updateWindowSize(rows, columns);
        super.updateSize(columns, rows);
    }

    private void updateWindowSize(int rows, int cols) {
        if (mRishService != null) {
            try {
                // Pack winsize struct into a long
                // struct winsize { unsigned short ws_row, ws_col, ws_xpixel, ws_ypixel; }
                long size = ((long) rows & 0xFFFF) |
                        (((long) cols & 0xFFFF) << 16) |
                        (0L << 32) |  // ws_xpixel
                        (0L << 48);   // ws_ypixel
                mRishService.setWindowSize(pid, size);
                Log.d(TAG, "Window size updated: " + rows + "x" + cols);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to update window size", e);
            }
        }
    }

    public String getTitle(String defaultTitle) {
        String title = getTitle();
        if (title != null && !title.isEmpty()) {
            return title;
        } else {
            return defaultTitle;
        }
    }

    protected void onProcessExit(int result) {
        mProcessExited = true;
        if (mProcessExitMessage != null) {
            byte[] msg = ("\r\n[" + String.format(mProcessExitMessage, result == 0 ? "" : (" (error " + result + ")")) + "]").getBytes(StandardCharsets.UTF_8);
            appendToEmulator(msg, 0, msg.length);
            notifyUpdate();
        }
    }

    public void setProcessExitMessage(String message) {
        mProcessExitMessage = message;
    }

    @Override
    public void write(byte[] data, int offset, int count) {
        if (mProcessExited) {
            for (int i = offset; i < offset + count; i++) {
                if (data[i] == '\r' || data[i] == '\n') {
                    Log.d(TAG, "Enter key pressed after process exit, closing session");
                    mHandler.post(this::finish);
                    return;
                }
            }
            return;
        }
        super.write(data, offset, count);
    }

    @Override
    public void write(int codePoint) {
        if (mProcessExited) {
            if (codePoint == '\r' || codePoint == '\n') {
                Log.d(TAG, "Enter key pressed after process exit, closing session");
                mHandler.post(this::finish);
                return;
            }
            return;
        }
        super.write(codePoint);
    }

    @Override
    public void finish() {
        Log.d(TAG, "finish() called");
        super.finish();

        // Clean up file descriptors
        closeFd(mStdin, 1);
        closeFd(mStdout, 0);

        Log.d(TAG, "Session finished");
    }

    public long getCreatedAt() {
        return createdAt;
    }

    private static void closeFd(FileDescriptor[] fileDescriptor, int i) {
        if (fileDescriptor == null || i >= fileDescriptor.length || fileDescriptor[i] == null) {
            return;
        }
        try {
            Os.close(fileDescriptor[i]);
            Log.d(TAG, "Closed fd at index " + i);
        } catch (ErrnoException e) {
            Log.w(TAG, "Failed to close file descriptor at index " + i, e);
        }
    }
}
