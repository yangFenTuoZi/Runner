package yangFenTuoZi.runner.plus.service.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class CommandDao {
    private final SQLiteDatabase db;

    public CommandDao(SQLiteDatabase db) {
        this.db = db;
    }

    public int size() {
        try (Cursor cursor = db.query(
                DataDbHelper.TABLE_COMMANDS,
                new String[]{DataDbHelper.COLUMN_ID},
                null, null, null, null, null)) {
            return cursor.getCount();
        }
    }

    public CommandInfo[] readAll() {
        try (Cursor cursor = db.query(
                DataDbHelper.TABLE_COMMANDS,
                null,
                null, null, null, null,
                DataDbHelper.COLUMN_POSITION + " ASC")) {

            CommandInfo[] commands = new CommandInfo[cursor.getCount()];
            int index = 0;

            while (cursor.moveToNext()) {
                CommandInfo info = new CommandInfo();
                info.name = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_NAME));
                info.command = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_COMMAND));
                info.keepAlive = cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_KEEP_ALIVE)) == 1;
                info.useChid = cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_USE_CHID)) == 1;
                info.ids = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_IDS));
                commands[index++] = info;
            }
            return commands;
        }
    }

    public CommandInfo read(int position) {
        try (Cursor cursor = db.query(
                DataDbHelper.TABLE_COMMANDS,
                null,
                DataDbHelper.COLUMN_POSITION + " = ?",
                new String[]{String.valueOf(position)},
                null, null, null, "1")) {

            if (cursor.moveToFirst()) {
                CommandInfo info = new CommandInfo();
                info.name = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_NAME));
                info.command = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_COMMAND));
                info.keepAlive = cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_KEEP_ALIVE)) == 1;
                info.useChid = cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_USE_CHID)) == 1;
                info.ids = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_IDS));
                return info;
            }
            return null;
        }
    }

    public long insert(CommandInfo commandInfo) {
        ContentValues values = new ContentValues();
        values.put(DataDbHelper.COLUMN_POSITION, size()); // 新项目放在最后
        values.put(DataDbHelper.COLUMN_NAME, commandInfo.name);
        values.put(DataDbHelper.COLUMN_COMMAND, commandInfo.command);
        values.put(DataDbHelper.COLUMN_KEEP_ALIVE, commandInfo.keepAlive ? 1 : 0);
        values.put(DataDbHelper.COLUMN_USE_CHID, commandInfo.useChid ? 1 : 0);
        values.put(DataDbHelper.COLUMN_IDS, commandInfo.ids);

        return db.insert(DataDbHelper.TABLE_COMMANDS, null, values);
    }

    public void insertInto(CommandInfo commandInfo, int position) {
        db.beginTransaction();
        try {
            db.execSQL("UPDATE " + DataDbHelper.TABLE_COMMANDS +
                            " SET " + DataDbHelper.COLUMN_POSITION + " = " + DataDbHelper.COLUMN_POSITION + " + 1" +
                            " WHERE " + DataDbHelper.COLUMN_POSITION + " >= ?",
                    new Object[]{position});

            ContentValues values = new ContentValues();
            values.put(DataDbHelper.COLUMN_POSITION, position);
            values.put(DataDbHelper.COLUMN_NAME, commandInfo.name);
            values.put(DataDbHelper.COLUMN_COMMAND, commandInfo.command);
            values.put(DataDbHelper.COLUMN_KEEP_ALIVE, commandInfo.keepAlive ? 1 : 0);
            values.put(DataDbHelper.COLUMN_USE_CHID, commandInfo.useChid ? 1 : 0);
            values.put(DataDbHelper.COLUMN_IDS, commandInfo.ids);

            db.insert(DataDbHelper.TABLE_COMMANDS, null, values);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void edit(CommandInfo commandInfo, int position) {
        ContentValues values = new ContentValues();
        values.put(DataDbHelper.COLUMN_NAME, commandInfo.name);
        values.put(DataDbHelper.COLUMN_COMMAND, commandInfo.command);
        values.put(DataDbHelper.COLUMN_KEEP_ALIVE, commandInfo.keepAlive ? 1 : 0);
        values.put(DataDbHelper.COLUMN_USE_CHID, commandInfo.useChid ? 1 : 0);
        values.put(DataDbHelper.COLUMN_IDS, commandInfo.ids);

        db.update(DataDbHelper.TABLE_COMMANDS, values,
                DataDbHelper.COLUMN_POSITION + " = ?",
                new String[]{String.valueOf(position)});
    }

    public void move(int fromPosition, int toPosition) {
        db.beginTransaction();
        try {
            // 获取要移动的项目的ID
            long idToMove = getIdAtPosition(fromPosition);

            if (fromPosition < toPosition) {
                // 向下移动 - 将中间项目向上移动
                db.execSQL("UPDATE " + DataDbHelper.TABLE_COMMANDS +
                                " SET " + DataDbHelper.COLUMN_POSITION + " = " + DataDbHelper.COLUMN_POSITION + " - 1" +
                                " WHERE " + DataDbHelper.COLUMN_POSITION + " > ? AND " + DataDbHelper.COLUMN_POSITION + " <= ?",
                        new Object[]{fromPosition, toPosition});
            } else {
                // 向上移动 - 将中间项目向下移动
                db.execSQL("UPDATE " + DataDbHelper.TABLE_COMMANDS +
                                " SET " + DataDbHelper.COLUMN_POSITION + " = " + DataDbHelper.COLUMN_POSITION + " + 1" +
                                " WHERE " + DataDbHelper.COLUMN_POSITION + " >= ? AND " + DataDbHelper.COLUMN_POSITION + " < ?",
                        new Object[]{toPosition, fromPosition});
            }

            // 更新移动项目的position
            ContentValues values = new ContentValues();
            values.put(DataDbHelper.COLUMN_POSITION, toPosition);
            db.update(DataDbHelper.TABLE_COMMANDS, values,
                    DataDbHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(idToMove)});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void delete(int position) {
        db.beginTransaction();
        try {
            // 先删除项目
            db.delete(DataDbHelper.TABLE_COMMANDS,
                    DataDbHelper.COLUMN_POSITION + " = ?",
                    new String[]{String.valueOf(position)});

            // 将后面的项目position减1
            db.execSQL("UPDATE " + DataDbHelper.TABLE_COMMANDS +
                            " SET " + DataDbHelper.COLUMN_POSITION + " = " + DataDbHelper.COLUMN_POSITION + " - 1" +
                            " WHERE " + DataDbHelper.COLUMN_POSITION + " > ?",
                    new Object[]{position});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private long getIdAtPosition(int position) {
        try (Cursor cursor = db.query(
                DataDbHelper.TABLE_COMMANDS,
                new String[]{DataDbHelper.COLUMN_ID},
                DataDbHelper.COLUMN_POSITION + " = ?",
                new String[]{String.valueOf(position)},
                null, null, null, "1")) {

            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_ID));
            }
            return -1;
        }
    }
}