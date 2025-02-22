package yangFenTuoZi.runner.plus.cli;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

public class CmdInfo implements Parcelable {

    public int rowid;
    public String name;
    public String command;
    public boolean keepAlive;
    public boolean useChid;
    public String ids;

    public CmdInfo() {
    }

    @SuppressLint("Range")
    public CmdInfo(Cursor cursor) {
        if (cursor == null) return;
        name = decBase64(cursor.getString(cursor.getColumnIndex("name")));
        command = decBase64(cursor.getString(cursor.getColumnIndex("command")));
        keepAlive = cursor.getInt(cursor.getColumnIndex("keepAlive")) == 1;
        useChid = cursor.getInt(cursor.getColumnIndex("useChid")) == 1;
        ids = cursor.getString(cursor.getColumnIndex("ids"));
    }

    public CmdInfo(JSONObject jsonObject) {
        importFromJSON(jsonObject);
    }

    public CmdInfo(String json) {
        try {
            importFromJSON(new JSONObject(json));
        } catch (JSONException ignored) {
        }
    }

    public CmdInfo(Parcel source) {
        super();
        rowid = source.readInt();
        name = source.readString();
        command = source.readString();
        keepAlive = source.readInt() == 1;
        useChid = source.readInt() == 1;
        ids = source.readString();
    }

    public void importFromJSON(JSONObject jsonObject) {
        try {
            rowid = jsonObject.getInt("rowid");
            name = jsonObject.getString("name");
            command = jsonObject.getString("command");
            keepAlive = jsonObject.getBoolean("keepAlive");
            useChid = jsonObject.getBoolean("useChid");
            ids = jsonObject.getString("ids");
        } catch (Exception ignored) {
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(rowid);
        dest.writeString(name);
        dest.writeString(command);
        dest.writeInt(keepAlive ? 1 : 0);
        dest.writeInt(useChid ? 1 : 0);
        dest.writeString(ids);
    }

    public static final Parcelable.Creator<CmdInfo> CREATOR = new Parcelable.Creator<>() {
        public CmdInfo createFromParcel(Parcel source) {
            return new CmdInfo(source);
        }

        public CmdInfo[] newArray(int size) {
            return new CmdInfo[size];
        }
    };

    @NonNull
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("rowid", rowid);
            jsonObject.put("name", name);
            jsonObject.put("command", command);
            jsonObject.put("keepAlive", keepAlive);
            jsonObject.put("useChid", useChid);
            jsonObject.put("ids", ids);
        } catch (Exception ignored) {
        }
        return jsonObject;
    }

    @NonNull
    @Override
    public String toString() {
        try {
            return toJSONObject().toString();
        } catch (Exception e) {
            return "";
        }
    }

    @NonNull
    public String toString(int indentSpaces) {
        try {
            return toJSONObject().toString(indentSpaces);
        } catch (Exception e) {
            return "";
        }
    }

    @NonNull
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put("rowid", rowid);
        values.put("name", encBase64(name));
        values.put("command", encBase64(command));
        values.put("keepAlive", keepAlive ? 1 : 0);
        values.put("useChid", useChid ? 1 : 0);
        values.put("ids", ids);
        return values;
    }

    private String encBase64(String text) {
        return new String(Base64.getEncoder().encode(text.getBytes()));
//        return text;
    }

    private String decBase64(String text) {
        return new String(Base64.getDecoder().decode(text.getBytes()));
//        return text;
    }
}
