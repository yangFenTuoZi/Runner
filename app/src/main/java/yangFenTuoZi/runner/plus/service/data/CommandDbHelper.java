package yangFenTuoZi.runner.plus.service.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import yangFenTuoZi.runner.plus.service.ServiceImpl;

public class CommandDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = ServiceImpl.DATA_PATH + "/commands.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_COMMANDS = "commands";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_POSITION = "position";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_COMMAND = "command";
    public static final String COLUMN_KEEP_ALIVE = "keep_alive";
    public static final String COLUMN_USE_CHID = "use_chid";
    public static final String COLUMN_IDS = "ids";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_COMMANDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_POSITION + " INTEGER NOT NULL, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_COMMAND + " TEXT NOT NULL, " +
                    COLUMN_KEEP_ALIVE + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_USE_CHID + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_IDS + " TEXT);";

    public CommandDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMANDS);
        onCreate(db);
    }
}