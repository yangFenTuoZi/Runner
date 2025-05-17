package yangfentuozi.runner.service.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import yangfentuozi.runner.service.ServiceImpl;

public class DataDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = ServiceImpl.DATA_PATH + "/data.db";
    private static final int DATABASE_VERSION = 1;

    // commands 表
    public static final String TABLE_COMMANDS = "commands";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_POSITION = "position";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_COMMAND = "command";
    public static final String COLUMN_KEEP_ALIVE = "keep_alive";
    public static final String COLUMN_REDUCE_PERM = "reduce_perm";
    public static final String COLUMN_TARGET_PERM = "target_perm";

    private static final String TABLE_COMMANDS_CREATE =
            "CREATE TABLE " + TABLE_COMMANDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_POSITION + " INTEGER NOT NULL, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_COMMAND + " TEXT NOT NULL, " +
                    COLUMN_KEEP_ALIVE + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_REDUCE_PERM + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_TARGET_PERM + " TEXT);";

    // environment 表
    public static final String TABLE_ENVIRONMENT = "environment";
    public static final String COLUMN_KEY = "env_key";
    public static final String COLUMN_VALUE = "env_value";

    private static final String TABLE_ENVIRONMENT_CREATE =
            "CREATE TABLE " + TABLE_ENVIRONMENT + " (" +
                    COLUMN_KEY + " TEXT NOT NULL, " +
                    COLUMN_VALUE + " TEXT NOT NULL, " +
                    "PRIMARY KEY (" + COLUMN_KEY + "));";

    public DataDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_COMMANDS_CREATE);
        db.execSQL(TABLE_ENVIRONMENT_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMANDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENVIRONMENT);
        onCreate(db);
    }

    private final SQLiteDatabase database = getWritableDatabase();

    public SQLiteDatabase getDatabase() {
        return database;
    }
}