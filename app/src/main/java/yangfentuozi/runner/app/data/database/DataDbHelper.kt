package yangfentuozi.runner.app.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DataDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TABLE_COMMANDS_CREATE)
        db.execSQL(TABLE_ENVIRONMENT_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMMANDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ENVIRONMENT")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME: String = "data.db"
        const val DATABASE_VERSION: Int = 1

        // commands 表
        const val TABLE_COMMANDS: String = "commands"
        const val COLUMN_ID: String = "_id"
        const val COLUMN_POSITION: String = "position"
        const val COLUMN_NAME: String = "name"
        const val COLUMN_COMMAND: String = "command"
        const val COLUMN_KEEP_ALIVE: String = "keep_alive"

        private const val TABLE_COMMANDS_CREATE = "CREATE TABLE " + TABLE_COMMANDS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_POSITION + " INTEGER NOT NULL, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_COMMAND + " TEXT NOT NULL, " +
                COLUMN_KEEP_ALIVE + " INTEGER NOT NULL DEFAULT 0);"

        // environment 表
        const val TABLE_ENVIRONMENT: String = "environment"
        const val COLUMN_KEY: String = "env_key"
        const val COLUMN_VALUE: String = "env_value"

        private const val TABLE_ENVIRONMENT_CREATE = "CREATE TABLE " + TABLE_ENVIRONMENT + " (" +
                COLUMN_KEY + " TEXT NOT NULL, " +
                COLUMN_VALUE + " TEXT NOT NULL, " +
                "PRIMARY KEY (" + COLUMN_KEY + "));"
    }
}