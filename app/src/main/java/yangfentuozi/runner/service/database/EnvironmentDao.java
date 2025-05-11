package yangfentuozi.runner.service.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import yangfentuozi.runner.service.data.EnvInfo;

public class EnvironmentDao {
    private final SQLiteDatabase db;

    public EnvironmentDao(SQLiteDatabase db) {
        this.db = db;
    }

    // 插入键值对
    public boolean insert(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(DataDbHelper.COLUMN_KEY, key);
        values.put(DataDbHelper.COLUMN_VALUE, value);
        long result = db.insert(DataDbHelper.TABLE_ENVIRONMENT, null, values);
        return result != -1; // 返回是否插入成功
    }

    // 更新键值对
    public boolean update(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(DataDbHelper.COLUMN_VALUE, value);
        int rowsAffected = db.update(
                DataDbHelper.TABLE_ENVIRONMENT,
                values,
                DataDbHelper.COLUMN_KEY + " = ?",
                new String[]{key}
        );
        return rowsAffected > 0; // 返回是否更新成功
    }

    public boolean update(String fromKey, String fromValue, String toKey, String toValue) {
        ContentValues values = new ContentValues();
        values.put(DataDbHelper.COLUMN_KEY, toKey);
        values.put(DataDbHelper.COLUMN_VALUE, toValue);

        int rowsAffected = db.update(
                DataDbHelper.TABLE_ENVIRONMENT,
                values,
                DataDbHelper.COLUMN_KEY + " = ? AND " + DataDbHelper.COLUMN_VALUE + " = ?",
                new String[]{fromKey, fromValue}
        );
        return rowsAffected > 0; // 返回是否更新成功
    }

    // 根据键获取值
    public String getValue(String key) {
        Cursor cursor = db.query(
                DataDbHelper.TABLE_ENVIRONMENT,
                new String[]{DataDbHelper.COLUMN_VALUE},
                DataDbHelper.COLUMN_KEY + " = ?",
                new String[]{key},
                null,
                null,
                null
        );

        String value = null;
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_VALUE));
        }
        cursor.close();
        return value;
    }

    // 删除键值对
    public void delete(String key) {
        db.delete(DataDbHelper.TABLE_ENVIRONMENT, DataDbHelper.COLUMN_KEY + " = ?", new String[]{key});
    }

    // 获取所有键值对
    public ArrayList<EnvInfo> getAll() {
        Cursor cursor = db.query(
                DataDbHelper.TABLE_ENVIRONMENT,
                new String[]{DataDbHelper.COLUMN_KEY, DataDbHelper.COLUMN_VALUE},
                null,
                null,
                null,
                null,
                null
        );

        ArrayList<EnvInfo> arrayList = new ArrayList<>();
        while (cursor.moveToNext()) {
            String key = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_KEY));
            String value = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_VALUE));
            var envInfo = new EnvInfo();
            envInfo.key = key;
            envInfo.value = value;
            arrayList.add(envInfo);
        }
        cursor.close();
        return arrayList;
    }
}