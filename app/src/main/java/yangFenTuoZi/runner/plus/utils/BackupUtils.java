package yangFenTuoZi.runner.plus.utils;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.info.Info;
import yangFenTuoZi.runner.plus.server.Server;
import yangFenTuoZi.runner.plus.ui.dialog.ExecDialogBuilder;

public class BackupUtils {
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

    public static void restore(JSONObject json) throws RestoreException {
        String data;
        try {
            if (((String) json.get("APP_PACKAGE_NAME")).isEmpty()
                    || !json.get("APP_PACKAGE_NAME").equals(Info.APPLICATION_ID)
                    || ((String) json.get("APP_VERSION_NAME")).isEmpty()
                    || ((String) json.get("APP_VERSION_CODE")).isEmpty()
                    || (data = (String) json.get("APP_DATABASE")).isEmpty()) {
                RestoreException exception = new RestoreException();
                exception.setWhat(RestoreException.WHAT_IS_NOT_APP_DATA);
                throw exception;
            }
        } catch (Exception e) {
            RestoreException exception = new RestoreException(e);
            exception.setWhat(RestoreException.WHAT_IS_NOT_APP_DATA);
            throw exception;
        }
        try {
            AtomicBoolean br = new AtomicBoolean(false);
            int port = ExecDialogBuilder.getUsablePort(8400);
            ByteArrayInputStream bos = new ByteArrayInputStream(data.getBytes());
            GZIPInputStream gzip = new GZIPInputStream(bos);
            new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    Socket socket = serverSocket.accept();
                    BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                    int len;
                    byte[] b = new byte[1024];
                    while ((len = gzip.read(b)) != -1) {
                        out.write(b, 0, len);
                    }
                    socket.close();
                } catch (IOException ignored) {
                }
                br.set(true);
            }).start();
            if (!App.iService.restoreData(port, Server.getSHA256(data.getBytes()))) {
                RestoreException exception = new RestoreException();
                exception.setWhat(RestoreException.WHAT_DATA_ERROR);
                throw exception;
            }
        } catch (Exception ignored) {
        }
    }
}