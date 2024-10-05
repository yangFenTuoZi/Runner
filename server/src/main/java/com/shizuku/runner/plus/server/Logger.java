package com.shizuku.runner.plus.server;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Logger {
    private static final String VERBOSE = "V";
    private static final String DEBUG = "D";
    private static final String INFO = "I";
    private static final String WARN = "W";
    private static final String ERROR = "E";
    private String TAG;
    private LocalDate lastLogDate;
    private FileWriter fileWriter;
    private File logDir;

    public Logger(String TAG, File logDir) {
        lastLogDate = LocalDate.now();
        try {
            if (logDir.isFile())
                return;
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            this.TAG = TAG;
            this.logDir = logDir;
            File logFile = new File(logDir + "/" + lastLogDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log");
            changeLogFile(logFile);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public void v(String message) {
        writeLog(VERBOSE, message);
    }

    public void d(String message) {
        writeLog(DEBUG, message);
    }

    public void i(String message) {
        writeLog(INFO, message);
    }

    public void w(String message) {
        writeLog(WARN, message);
    }

    public void e(String message) {
        writeLog(ERROR, message);
    }

    public void v(String message, Object... args) {
        writeLog(VERBOSE, String.format(message, args));
    }

    public void d(String message, Object... args) {
        writeLog(DEBUG, String.format(message, args));
    }

    public void i(String message, Object... args) {
        writeLog(INFO, String.format(message, args));
    }

    public void w(String message, Object... args) {
        writeLog(WARN, String.format(message, args));
    }

    public void e(String message, Object... args) {
        writeLog(ERROR, String.format(message, args));
    }

    private void writeLog(String priority, String message) {
        try {
            switch (priority) {
                case VERBOSE -> android.util.Log.v(TAG, message);
                case DEBUG -> android.util.Log.d(TAG, message);
                case INFO -> android.util.Log.i(TAG, message);
                case WARN -> android.util.Log.w(TAG, message);
                case ERROR -> android.util.Log.e(TAG, message);
            }
            LocalDate date = LocalDate.now();
            if (!Objects.equals(lastLogDate.format(DateTimeFormatter.ofPattern("dd")), date.format(DateTimeFormatter.ofPattern("dd")))) {
                changeLogFile(new File(logDir, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log"));
            }
            if (!isClose()) {
                String log = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] [" + TAG + "] [" + priority + "] " + message;
                fileWriter.write(log + "\n");
                fileWriter.flush();
                System.out.println(log);
                lastLogDate = date;
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public void printf(String message) {
        try {
            LocalDate date = LocalDate.now();
            if (!Objects.equals(lastLogDate.format(DateTimeFormatter.ofPattern("dd")), date.format(DateTimeFormatter.ofPattern("dd")))) {
                changeLogFile(new File(logDir, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log"));
            }
            if (!isClose()) {
                fileWriter.write(message);
                fileWriter.flush();
                System.out.printf(message);
                lastLogDate = date;
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
                fileWriter = null;
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public boolean isClose() {
        return fileWriter == null;
    }

    private void changeLogFile(File file) throws IOException {
        if (fileWriter != null)
            close();
        if (!file.exists())
            file.createNewFile();
        fileWriter = new FileWriter(file, true);
    }
}
