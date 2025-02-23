package yangFenTuoZi.runner.plus.utils;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateUtils {
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
            String version_name;
            int version_code;
            String update_url;
            String update_msg;
            try {
                version_name = jsonObject.getString("version_name");
                version_code = jsonObject.getInt("version_code");
                update_url = jsonObject.getString("update_url");
                update_msg = jsonObject.getString("update_msg");
            } catch (JSONException e) {
                UpdateException e1 = new UpdateException(e);
                e1.setWhat(UpdateException.WHAT_JSON_FORMAT_ERROR);
                throw e1;
            }
            this.version_name = version_name;
            this.version_code = version_code;
            this.update_url = update_url;
            this.update_msg = update_msg;
        }

        public UpdateInfo(String jsonInfo) throws UpdateException {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(jsonInfo);
            } catch (JSONException jsonException) {
                UpdateException e = new UpdateException(jsonException);
                e.setWhat(UpdateException.WHAT_CAN_NOT_PARSE_JSON);
                throw e;
            }
            String version_name;
            int version_code;
            String update_url;
            String update_msg;
            try {
                version_name = jsonObject.getString("version_name");
                version_code = jsonObject.getInt("version_code");
                update_url = jsonObject.getString("update_url");
                update_msg = jsonObject.getString("update_msg");
            } catch (JSONException e) {
                UpdateException e1 = new UpdateException(e);
                e1.setWhat(UpdateException.WHAT_JSON_FORMAT_ERROR);
                throw e1;
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
                return Update(mirror_url + beta_url);
            } else if (ping(original_url + beta_url)) {
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