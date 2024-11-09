package yangFenTuoZi.runner.plus.cli;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public class CmdInfo implements Parcelable {

    public static final String table_name = "cmds";

    public int id;
    public String name;
    public String command;
    public boolean keepAlive;
    public boolean useChid;
    public String ids;

    public CmdInfo() {
    }

    public CmdInfo(Parcel source) {
        super();
        id = source.readInt();
        name = source.readString();
        command = source.readString();
        keepAlive = source.readInt() == 1;
        useChid = source.readInt() == 1;
        ids = source.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
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
    @Override
    public String toString() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            jsonObject.put("command", command);
            jsonObject.put("keepAlive", keepAlive);
            jsonObject.put("useChid", useChid);
            jsonObject.put("ids", ids);
            return jsonObject.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @NonNull
    public String toString(int indentSpaces) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            jsonObject.put("command", command);
            jsonObject.put("keepAlive", keepAlive);
            jsonObject.put("useChid", useChid);
            jsonObject.put("ids", ids);
            return jsonObject.toString(indentSpaces);
        } catch (Exception e) {
            return "";
        }
    }

    public void insert(SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("name", name);
        values.put("command", command);
        values.put("keepAlive", keepAlive ? 1 : 0);
        values.put("useChid", useChid ? 1 : 0);
        values.put("ids", ids);
        database.insert(table_name, null, values);
    }

    public void update(SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("name", name);
        values.put("command", command);
        values.put("keepAlive", keepAlive ? 1 : 0);
        values.put("useChid", useChid ? 1 : 0);
        values.put("ids", ids);
        database.update(table_name, values, "id=?", new String[]{String.valueOf(id)});
    }

    public static CmdInfo query(SQLiteDatabase database, int id) {
        return query(database, String.valueOf(id));
    }

    public static CmdInfo[] query(SQLiteDatabase database, int[] ids) {
        String[] args = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            args[i] = String.valueOf(ids[i]);
        }
        return query(database, args);
    }

    public static CmdInfo query(SQLiteDatabase database, String id) {
        CmdInfo[] cmdInfos = query(database, new String[]{id});
        return cmdInfos.length == 0 ? null : cmdInfos[0];
    }

    @SuppressLint({"Range", "Recycle"})
    public static CmdInfo[] query(SQLiteDatabase database, String[] ids) {
        Cursor cursor = database.query(table_name, null, "id=?", ids, null, null, null);
        CmdInfo[] result = new CmdInfo[cursor.getCount()];
        if (cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.move(i);
                CmdInfo cmdInfo = new CmdInfo();
                cmdInfo.id = cursor.getInt(cursor.getColumnIndex("id"));
                cmdInfo.name = cursor.getString(cursor.getColumnIndex("name"));
                cmdInfo.command = cursor.getString(cursor.getColumnIndex("command"));
                cmdInfo.keepAlive = cursor.getInt(cursor.getColumnIndex("keepAlive")) == 1;
                cmdInfo.useChid = cursor.getInt(cursor.getColumnIndex("useChid")) == 1;
                cmdInfo.ids = cursor.getString(cursor.getColumnIndex("ids"));
                result[i] = cmdInfo;
            }
        }
        return result;
    }

    @SuppressLint({"Range", "Recycle"})
    public static CmdInfo[] getAll(SQLiteDatabase database) {
        Cursor cursor = database.query(table_name, null, null, null, null, null, null);
        CmdInfo[] result = new CmdInfo[cursor.getCount()];
        if (cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.move(i);
                CmdInfo cmdInfo = new CmdInfo();
                cmdInfo.id = cursor.getInt(cursor.getColumnIndex("id"));
                cmdInfo.name = cursor.getString(cursor.getColumnIndex("name"));
                cmdInfo.command = cursor.getString(cursor.getColumnIndex("command"));
                cmdInfo.keepAlive = cursor.getInt(cursor.getColumnIndex("keepAlive")) == 1;
                cmdInfo.useChid = cursor.getInt(cursor.getColumnIndex("useChid")) == 1;
                cmdInfo.ids = cursor.getString(cursor.getColumnIndex("ids"));
                result[i] = cmdInfo;
            }
        }
        return result;
    }

    public void delete(SQLiteDatabase database) {
        delete(database, id);
    }

    public static void delete(SQLiteDatabase database, int id) {
        delete(database, String.valueOf(id));
    }

    public static void delete(SQLiteDatabase database, int[] ids) {
        String[] args = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            args[i] = String.valueOf(ids[i]);
        }
        delete(database, args);
    }

    public static void delete(SQLiteDatabase database, String id) {
        delete(database, new String[]{id});
    }

    public static void delete(SQLiteDatabase database, String[] ids) {
        database.delete(table_name, "id=?", ids);
    }
}
