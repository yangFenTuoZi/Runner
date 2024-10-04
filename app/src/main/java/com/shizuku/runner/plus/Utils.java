package com.shizuku.runner.plus;

import android.annotation.SuppressLint;
import android.app.IApplicationThread;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Utils {
    public static class UpdateUtils {
        public static class UpdateException extends RuntimeException {
            public final static int WHAT_CAN_NOT_CONNECT_UPDATE_SERVER = 0;
            public final static int WHAT_CAN_NOT_PARSE_JSON = 1;
            public final static int WHAT_JSON_FORMAT_ERROR = 2;

            private int what;

            public UpdateException() {
                super();
            }

            public UpdateException(String message) {
                super(message);
            }

            public UpdateException(String message, Throwable cause) {
                super(message, cause);
            }

            public UpdateException(Throwable cause) {
                super(cause);
            }

            public void setWhat(int what) {
                this.what = what;
            }

            public int getWhat() {
                return what;
            }
        }

        public static class UpdateInfo {
            public String version_name;
            public int version_code;
            public String update_url;
            public String update_msg;

            public UpdateInfo(String version_name, int version_code, String update_url, String update_msg) {
                this.version_name = version_name;
                this.version_code = version_code;
                this.update_url = update_url;
                this.update_msg = update_msg;
            }

            public UpdateInfo(JSONObject jsonObject) throws UpdateException {
                String version_name = jsonObject.getString("version_name");
                int version_code = jsonObject.getIntValue("version_code");
                String update_url = jsonObject.getString("update_url");
                String update_msg = jsonObject.getString("update_msg");
                if (version_name == null || version_code == 0 || update_url == null || update_msg == null) {
                    UpdateException e = new UpdateException();
                    e.setWhat(UpdateException.WHAT_JSON_FORMAT_ERROR);
                    throw e;
                }
                this.version_name = version_name;
                this.version_code = version_code;
                this.update_url = update_url;
                this.update_msg = update_msg;
            }

            public UpdateInfo(String jsonInfo) throws UpdateException {
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(jsonInfo);
                } catch (JSONException jsonException) {
                    UpdateException e = new UpdateException(jsonException);
                    e.setWhat(UpdateException.WHAT_CAN_NOT_PARSE_JSON);
                    throw e;
                }
                String version_name = jsonObject.getString("version_name");
                int version_code = jsonObject.getIntValue("version_code");
                String update_url = jsonObject.getString("update_url");
                String update_msg = jsonObject.getString("update_msg");
                if (version_name == null || version_code == 0 || update_url == null || update_msg == null) {
                    UpdateException e = new UpdateException(new NullPointerException());
                    e.setWhat(UpdateException.WHAT_JSON_FORMAT_ERROR);
                    throw e;
                }
                this.version_name = version_name;
                this.version_code = version_code;
                this.update_url = update_url;
                this.update_msg = update_msg;
            }
        }

        private final static int timeOut = 3000;
        private final static String mirror_url = "https://raw.gitmirror.com/";
        private final static String original_url = "https://raw.githubusercontent.com/";
        private final static String beta_url = "yangFenTuoZi/Runner/master/update_beta.json";
        private final static String stable_url = "yangFenTuoZi/Runner/master/update_stable.json";

        public static boolean ping(String url) {
            try {
                URL url_ = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) url_.openConnection();
                try {
                    connection.setRequestMethod("HEAD");
                    connection.setConnectTimeout(500);
                    connection.setReadTimeout(500);
                    return true;
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                return false;
            }
        }

        public static String wget(String url) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(timeOut);
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() == 200) {
                    StringBuilder result = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String s;
                    while ((s = bufferedReader.readLine()) != null)
                        result.append(s).append("\n");
                    return result.toString();
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        private static UpdateInfo Update(String url) throws UpdateException {
            if (!ping(url)) {
                UpdateException e = new UpdateException();
                e.setWhat(UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER);
                throw e;
            }
            String jsonInfo = wget(url);
            if (jsonInfo == null) {
                UpdateException e = new UpdateException();
                e.setWhat(UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER);
                throw e;
            }
            return new UpdateInfo(jsonInfo);
        }

        public static UpdateInfo Update(boolean isBeta) throws UpdateException {
            if (isBeta) {
                if (ping(mirror_url + beta_url)) {
                    Log.d("Utils$UpdateUtils$Update(boolean)", "mirror");
                    return Update(mirror_url + beta_url);
                } else if (ping(original_url + beta_url)) {
                    Log.d("Utils$UpdateUtils$Update(boolean)", "original");
                    return Update(original_url + beta_url);
                } else {
                    UpdateException e = new UpdateException();
                    e.setWhat(UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER);
                    throw e;
                }
            } else {
                if (ping(mirror_url + stable_url)) {
                    return Update(mirror_url + stable_url);
                } else if (ping(original_url + stable_url)) {
                    return Update(original_url + stable_url);
                } else {
                    UpdateException e = new UpdateException();
                    e.setWhat(UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER);
                    throw e;
                }
            }
        }
    }

    public static class BackupUtils {
        public static class RestoreException extends RuntimeException {
            public final static int WHAT_VER_IS_LOW = 0;
            public final static int WHAT_IS_NOT_APP_DATA = 1;
            public final static int WHAT_DATA_ERROR = 2;
            public final static int WHAT_JSON_PARSE_ERROR = 3;

            private int what;

            public RestoreException() {
                super();
            }

            public RestoreException(String message) {
                super(message);
            }

            public RestoreException(String message, Throwable cause) {
                super(message, cause);
            }

            public RestoreException(Throwable cause) {
                super(cause);
            }

            public void setWhat(int what) {
                this.what = what;
            }

            public int getWhat() {
                return what;
            }
        }

        public static JSONObject backup(Context mContext, PreferenceFragmentCompat mSettingsFragment) {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("data", 0);
            String string = sharedPreferences.getString("data", "");
            JSONObject json = new JSONObject();
            json.put("APP_PACKAGE_NAME", BuildConfig.APPLICATION_ID);
            json.put("APP_VERSION_NAME", BuildConfig.VERSION_NAME);
            json.put("APP_VERSION_CODE", String.valueOf(BuildConfig.VERSION_CODE));
            if (!string.isEmpty()) {
                String[] s = string.split(",");
                JSONArray jsonArray = json.putArray("data");
                int i = 1;
                for (String s1 : s) {
                    jsonArray.add(i);
                    JSONObject jsonObject = json.putObject(String.valueOf(i));
                    SharedPreferences sharedPreferences1 = mContext.getSharedPreferences(s1, 0);

                    //name
                    String name = sharedPreferences1.getString("name", "");
                    if (!name.isEmpty()) jsonObject.put("name", name);

                    //command
                    String command = sharedPreferences1.getString("command", "");
                    if (!command.isEmpty()) jsonObject.put("command", command);

                    //keep in alive
                    jsonObject.put("keep_in_alive", sharedPreferences1.getBoolean("keep_in_alive", false));

                    //chid
                    boolean chid = sharedPreferences1.getBoolean("chid", false);
                    if (chid) {
                        jsonObject.put("chid", true);
                        String ids = sharedPreferences1.getString("ids", "");
                        if (!ids.isEmpty()) jsonObject.put("ids", ids);
                    }
                    i++;
                }
            }
            return json;
        }

        public static void restore(Context mContext, JSONObject json, boolean isCovered) throws RestoreException {
            try {
                if (((String) json.get("APP_PACKAGE_NAME")).isEmpty()
                        || !json.get("APP_PACKAGE_NAME").equals(BuildConfig.APPLICATION_ID)
                        || ((String) json.get("APP_VERSION_NAME")).isEmpty()
                        || ((String) json.get("APP_VERSION_CODE")).isEmpty()) {
                    RestoreException exception = new RestoreException();
                    exception.setWhat(RestoreException.WHAT_IS_NOT_APP_DATA);
                    throw exception;
                }
            } catch (Exception e) {
                RestoreException exception = new RestoreException(e);
                exception.setWhat(RestoreException.WHAT_IS_NOT_APP_DATA);
                throw exception;
            }
            if (isCovered) {
                for (File file1 : Objects.requireNonNull(mContext.getDataDir().listFiles()))
                    if (file1.isFile()) file1.delete();
            }
            List<String> list = new ArrayList<>();
            JSONArray jsonArray = json.getJSONArray("data");
            if (jsonArray == null) {
                RestoreException exception = new RestoreException();
                exception.setWhat(RestoreException.WHAT_JSON_PARSE_ERROR);
                throw exception;
            }
            for (int i = 1; i <= jsonArray.size(); i++) {
                JSONObject jsonObject = json.getJSONObject(String.valueOf(jsonArray.get(i - 1)));
                if (jsonObject == null) {
                    RestoreException exception = new RestoreException();
                    exception.setWhat(RestoreException.WHAT_JSON_PARSE_ERROR);
                    throw exception;
                }
                list.add(String.valueOf(jsonArray.get(i - 1)));

                SharedPreferences.Editor editor = mContext.getSharedPreferences(String.valueOf(jsonArray.get(i - 1)), 0).edit();

                //name
                String name = jsonObject.getString("name");
                if (!name.isEmpty()) editor.putString("name", name);

                //command
                String command = jsonObject.getString("command");
                if (!command.isEmpty()) editor.putString("command", command);

                //keep in alive
                editor.putBoolean("keep_in_alive", jsonObject.getBooleanValue("keep_in_alive", false));

                //chid
                boolean chid = jsonObject.getBooleanValue("chid", false);
                if (chid) {
                    editor.putBoolean("chid", true);
                    String ids = jsonObject.getString("ids");
                    if (!ids.isEmpty()) jsonObject.put("ids", ids);
                }

                editor.apply();
                i++;
            }
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("data", 0);
            sharedPreferences.edit().putString("data", String.join(",", list)).apply();

        }
    }

    public static class Encode {

        private static final char[] CHS = {
                'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'A', 'S', 'D', 'F', 'G', 'H',
                'J', 'K', 'L', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', 'q', 'w', 'e', 'r', 't', 'y',
                'u', 'i', 'o', 'p', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'z', 'x', 'c',
                'v', 'b', 'n', 'm', '0', '9', '1', '8', '2', '7', '3', '6', '4', '5', '~', '!'};

        private static final Map<Byte, Byte> CHS_MAP = new HashMap<>(64);

        static {
            for (int i = 0; i < CHS.length; i++) {
                CHS_MAP.put((byte) CHS[i], (byte) i);
            }
        }

        public static String encode(String s) {
            byte[] bytes = s.getBytes();

            int groupCount = bytes.length / 3;
            int remainCount = bytes.length % 3;

            int encLength;
            if (remainCount == 0) {
                encLength = groupCount * 4;
            } else {
                encLength = (groupCount + 1) * 4;
            }

            int index, encI;
            byte b1, b2, b3;

            byte[] encBuf = new byte[encLength];

            for (int i = 0; i < groupCount; i++) {
                index = i * 3;

                b1 = bytes[index];
                b2 = bytes[index + 1];
                b3 = bytes[index + 2];

                encI = i * 4;
                encBuf[encI] = (byte) CHS[(b1 >> 2) & 0x3F];
                encBuf[encI + 1] = (byte) CHS[((b1 << 4) & 0x30) | ((b2 >> 4) & 0x0F)];
                encBuf[encI + 2] = (byte) CHS[((b2 << 2) & 0x3C) | ((b3 >> 6) & 0x03)];
                encBuf[encI + 3] = (byte) CHS[b3 & 0x3F];
            }

            if (remainCount == 2) {
                index = groupCount * 3;
                b1 = bytes[index];
                b2 = bytes[index + 1];
                b3 = 0;

                encI = groupCount * 4;
                encBuf[encI] = (byte) CHS[(b1 >> 2) & 0x3F];
                encBuf[encI + 1] = (byte) CHS[((b1 << 4) & 0x30) | ((b2 >> 4) & 0x0F)];
                encBuf[encI + 2] = (byte) CHS[((b2 << 2) & 0x3C) | ((0) & 0x03)];
                encBuf[encI + 3] = '=';
            }

            if (remainCount == 1) {
                index = groupCount * 3;
                b1 = bytes[index];
                b2 = 0;

                encI = groupCount * 4;
                encBuf[encI] = (byte) CHS[(b1 >> 2) & 0x3F];
                encBuf[encI + 1] = (byte) CHS[((b1 << 4) & 0x30) | ((0) & 0x0F)];
                encBuf[encI + 2] = encBuf[encI + 3] = '=';
            }

            return a(new String(encBuf));
        }

        public static String decode(String s) {
            byte[] encBytes = b(s).getBytes();

            int encLen = encBytes.length;
            int groupCount = encLen / 4;

            int decLen;
            int remainCount;
            if (encBytes[encLen - 1] == '=') {
                if (encBytes[encLen - 2] == '=') {
                    decLen = groupCount * 3 - 2;
                    remainCount = 1;
                } else {
                    decLen = groupCount * 3 - 1;
                    remainCount = 2;
                }
            } else {
                decLen = groupCount * 3;
                remainCount = 0;
            }

            int index, index2;
            byte[] decBytes = new byte[decLen];
            byte b1, b2, b3;

            for (int i = 0; i < groupCount - 1; i++) {
                decodeGroup(encBytes, decBytes, i);
            }

            if (remainCount == 2) {
                index = (groupCount - 1) * 4;
                b1 = CHS_MAP.get(encBytes[index]);
                b2 = CHS_MAP.get(encBytes[index + 1]);
                b3 = CHS_MAP.get(encBytes[index + 2]);

                index2 = (groupCount - 1) * 3;
                decBytes[index2] = (byte) (((b1 << 2) & 0xFF) | ((b2 >> 4) & 0xFF));
                decBytes[index2 + 1] = (byte) (((b2 << 4) & 0xFF) | ((b3 >> 2) & 0xFF));
            }

            if (remainCount == 1) {
                index = (groupCount - 1) * 4;
                b1 = CHS_MAP.get(encBytes[index]);
                b2 = CHS_MAP.get(encBytes[index + 1]);
                index2 = (groupCount - 1) * 3;
                decBytes[index2] = (byte) (((b1 << 2) & 0xFF) | ((b2 >> 4) & 0xFF));
            }

            if (remainCount == 0) {
                decodeGroup(encBytes, decBytes, groupCount - 1);
            }

            return new String(decBytes);
        }

        private static void decodeGroup(byte[] encBytes, byte[] decBytes, int i) {
            byte b1, b2, b3, b4;

            int index = i * 4;

            b1 = CHS_MAP.get(encBytes[index]);
            b2 = CHS_MAP.get(encBytes[index + 1]);
            b3 = CHS_MAP.get(encBytes[index + 2]);
            b4 = CHS_MAP.get(encBytes[index + 3]);

            int index2 = i * 3;
            decBytes[index2] = (byte) (((b1 << 2) & 0xFF) | ((b2 >> 4) & 0xFF));
            decBytes[index2 + 1] = (byte) (((b2 << 4) & 0xFF) | ((b3 >> 2) & 0xFF));
            decBytes[index2 + 2] = (byte) (((b3 << 6) & 0xFF) | (b4 & 0xFF));
        }

        public static String a(String a) {
            StringBuilder result = new StringBuilder();
            for (int i = 1; i <= a.length(); i++) {
                if (i % 7 == 0) {
                    result.append(switch (new Random().nextInt(5)) {
                        case 1 -> "+";
                        case 2 -> "&";
                        case 3 -> "$";
                        case 4 -> "\\";
                        default -> "-";
                    });
                }
                result.append(a.charAt(i - 1));
            }
            return result.toString();
        }

        public static String b(String a) {
            return a
                    .replaceAll("-", "")
                    .replaceAll("\\+", "")
                    .replaceAll("&", "")
                    .replaceAll("\\$", "")
                    .replaceAll("\\\\", "");
        }
    }
}
