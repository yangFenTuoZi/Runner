package yangfentuozi.runner.service.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.sqlite.transaction
import yangfentuozi.runner.service.data.CommandInfo

class CommandDao(private val db: SQLiteDatabase) {
    fun size(): Int {
        db.query(
            DataDbHelper.TABLE_COMMANDS,
            arrayOf<String>(DataDbHelper.COLUMN_ID),
            null, null, null, null, null
        ).use { cursor ->
            return cursor.count
        }
    }

    fun readAll(): ArrayList<CommandInfo>? {
        db.query(
            DataDbHelper.TABLE_COMMANDS,
            null,
            null, null, null, null,
            DataDbHelper.COLUMN_POSITION + " ASC"
        ).use { cursor ->
            val commands = ArrayList<CommandInfo>()

            while (cursor.moveToNext()) {
                val info = CommandInfo()
                info.name = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_NAME))
                info.command =
                    cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_COMMAND))
                info.keepAlive =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_KEEP_ALIVE)) == 1
                info.reducePerm =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_REDUCE_PERM)) == 1
                info.targetPerm =
                    cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_TARGET_PERM))
                commands.add(info)
            }
            return commands
        }
    }

    fun read(position: Int): CommandInfo? {
        db.query(
            DataDbHelper.TABLE_COMMANDS,
            null,
            DataDbHelper.COLUMN_POSITION + " = ?",
            arrayOf<String>(position.toString()),
            null, null, null, "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val info = CommandInfo()
                info.name = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_NAME))
                info.command =
                    cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_COMMAND))
                info.keepAlive =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_KEEP_ALIVE)) == 1
                info.reducePerm =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_REDUCE_PERM)) == 1
                info.targetPerm =
                    cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_TARGET_PERM))
                return info
            }
            return null
        }
    }

    fun insert(commandInfo: CommandInfo): Long {
        val values = ContentValues()
        values.put(DataDbHelper.COLUMN_POSITION, size()) // 新项目放在最后
        values.put(DataDbHelper.COLUMN_NAME, commandInfo.name)
        values.put(DataDbHelper.COLUMN_COMMAND, commandInfo.command)
        values.put(DataDbHelper.COLUMN_KEEP_ALIVE, if (commandInfo.keepAlive) 1 else 0)
        values.put(DataDbHelper.COLUMN_REDUCE_PERM, if (commandInfo.reducePerm) 1 else 0)
        values.put(DataDbHelper.COLUMN_TARGET_PERM, commandInfo.targetPerm)

        return db.insert(DataDbHelper.TABLE_COMMANDS, null, values)
    }

    fun insertInto(commandInfo: CommandInfo, position: Int) {
        db.transaction {
            try {
                execSQL(
                    "UPDATE " + DataDbHelper.TABLE_COMMANDS +
                            " SET " + DataDbHelper.COLUMN_POSITION + " = " + DataDbHelper.COLUMN_POSITION + " + 1" +
                            " WHERE " + DataDbHelper.COLUMN_POSITION + " >= ?",
                    arrayOf<Any>(position)
                )

                val values = ContentValues()
                values.put(DataDbHelper.COLUMN_POSITION, position)
                values.put(DataDbHelper.COLUMN_NAME, commandInfo.name)
                values.put(DataDbHelper.COLUMN_COMMAND, commandInfo.command)
                values.put(DataDbHelper.COLUMN_KEEP_ALIVE, if (commandInfo.keepAlive) 1 else 0)
                values.put(DataDbHelper.COLUMN_REDUCE_PERM, if (commandInfo.reducePerm) 1 else 0)
                values.put(DataDbHelper.COLUMN_TARGET_PERM, commandInfo.targetPerm)

                insert(DataDbHelper.TABLE_COMMANDS, null, values)

            } finally {
            }
        }
    }

    fun edit(commandInfo: CommandInfo, position: Int) {
        val values = ContentValues()
        values.put(DataDbHelper.COLUMN_NAME, commandInfo.name)
        values.put(DataDbHelper.COLUMN_COMMAND, commandInfo.command)
        values.put(DataDbHelper.COLUMN_KEEP_ALIVE, if (commandInfo.keepAlive) 1 else 0)
        values.put(DataDbHelper.COLUMN_REDUCE_PERM, if (commandInfo.reducePerm) 1 else 0)
        values.put(DataDbHelper.COLUMN_TARGET_PERM, commandInfo.targetPerm)

        db.update(
            DataDbHelper.TABLE_COMMANDS, values,
            DataDbHelper.COLUMN_POSITION + " = ?",
            arrayOf<String>(position.toString())
        )
    }

    fun move(fromPosition: Int, toPosition: Int) {
        db.transaction {
            try {
                // 获取要移动的项目的ID
                val idToMove = getIdAtPosition(fromPosition)

                if (fromPosition < toPosition) {
                    // 向下移动 - 将中间项目向上移动
                    execSQL(
                        "UPDATE " + DataDbHelper.TABLE_COMMANDS +
                                " SET " + DataDbHelper.COLUMN_POSITION + " = " + DataDbHelper.COLUMN_POSITION + " - 1" +
                                " WHERE " + DataDbHelper.COLUMN_POSITION + " > ? AND " + DataDbHelper.COLUMN_POSITION + " <= ?",
                        arrayOf<Any>(fromPosition, toPosition)
                    )
                } else {
                    // 向上移动 - 将中间项目向下移动
                    execSQL(
                        "UPDATE " + DataDbHelper.TABLE_COMMANDS +
                                " SET " + DataDbHelper.COLUMN_POSITION + " = " + DataDbHelper.COLUMN_POSITION + " + 1" +
                                " WHERE " + DataDbHelper.COLUMN_POSITION + " >= ? AND " + DataDbHelper.COLUMN_POSITION + " < ?",
                        arrayOf<Any>(toPosition, fromPosition)
                    )
                }

                // 更新移动项目的position
                val values = ContentValues()
                values.put(DataDbHelper.COLUMN_POSITION, toPosition)
                update(
                    DataDbHelper.TABLE_COMMANDS, values,
                    DataDbHelper.COLUMN_ID + " = ?",
                    arrayOf<String>(idToMove.toString())
                )

            } finally {
            }
        }
    }

    fun delete(position: Int) {
        db.transaction {
            try {
                // 先删除项目
                delete(
                    DataDbHelper.TABLE_COMMANDS,
                    DataDbHelper.COLUMN_POSITION + " = ?",
                    arrayOf<String>(position.toString())
                )

                // 将后面的项目position减1
                execSQL(
                    "UPDATE " + DataDbHelper.TABLE_COMMANDS +
                            " SET " + DataDbHelper.COLUMN_POSITION + " = " + DataDbHelper.COLUMN_POSITION + " - 1" +
                            " WHERE " + DataDbHelper.COLUMN_POSITION + " > ?",
                    arrayOf<Any>(position)
                )

            } finally {
            }
        }
    }

    private fun getIdAtPosition(position: Int): Long {
        db.query(
            DataDbHelper.TABLE_COMMANDS,
            arrayOf<String>(DataDbHelper.COLUMN_ID),
            DataDbHelper.COLUMN_POSITION + " = ?",
            arrayOf<String>(position.toString()),
            null, null, null, "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_ID))
            }
            return -1
        }
    }
}