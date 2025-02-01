package yangFenTuoZi.runner.plus.server;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DB_VER = 1;
    public static final String DB_NAME = Server.DATA_PATH + "/database.db";

    public static final String TABLE_NAME = "cmds";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS cmds(
                name        nvarchar,
                command     nvarchar,
                keepAlive   int         not null,
                useChid     int         not null,
                ids         nvarchar
            );
            """);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion != DB_VER) return;
        switch (oldVersion) {
        }
    }
}
