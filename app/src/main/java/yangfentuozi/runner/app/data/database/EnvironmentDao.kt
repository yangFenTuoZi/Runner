package yangfentuozi.runner.app.data.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import yangfentuozi.runner.shared.data.EnvInfo

class EnvironmentDao(private val db: SQLiteDatabase) {
    // 插入键值对
    fun insert(key: String?, value: String?): Boolean {
        val values = ContentValues()
        values.put(DataDbHelper.COLUMN_KEY, key)
        values.put(DataDbHelper.COLUMN_VALUE, value)
        val result = db.insert(DataDbHelper.TABLE_ENVIRONMENT, null, values)
        return result != -1L // 返回是否插入成功
    }

    // 更新键值对
    fun update(
        key: String?,
        value: String?
    ): Boolean {
        val values = ContentValues()
        values.put(DataDbHelper.COLUMN_VALUE, value)
        val rowsAffected = db.update(
            DataDbHelper.TABLE_ENVIRONMENT,
            values,
            DataDbHelper.COLUMN_KEY + " = ?",
            arrayOf(key)
        )
        return rowsAffected > 0 // 返回是否更新成功
    }

    // 更新启用状态
    fun update(
        key: String?,
        enabled: Boolean
    ): Boolean {
        val values = ContentValues()
        values.put(DataDbHelper.COLUMN_ENABLED, if (enabled) 1 else 0)
        val rowsAffected = db.update(
            DataDbHelper.TABLE_ENVIRONMENT,
            values,
            DataDbHelper.COLUMN_KEY + " = ?",
            arrayOf(key)
        )
        return rowsAffected > 0 // 返回是否更新成功
    }

    // 根据键获取值
    fun getValue(key: String?): String? {
        db.query(
            DataDbHelper.TABLE_ENVIRONMENT,
            arrayOf(DataDbHelper.COLUMN_VALUE),
            DataDbHelper.COLUMN_KEY + " = ?",
            arrayOf(key),
            null,
            null,
            null
        ).use { cursor ->
            var value: String? = null
            if (cursor.moveToFirst()) {
                value = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_VALUE))
            }
            return value
        }
    }

    // 删除键值对
    fun delete(key: String?) {
        db.delete(
            DataDbHelper.TABLE_ENVIRONMENT,
            DataDbHelper.COLUMN_KEY + " = ?",
            arrayOf(key)
        )
    }

    val all: ArrayList<EnvInfo>
        // 获取所有键值对
        get() {
            db.query(
                DataDbHelper.TABLE_ENVIRONMENT,
                arrayOf(DataDbHelper.COLUMN_KEY, DataDbHelper.COLUMN_VALUE, DataDbHelper.COLUMN_ENABLED),
                null,
                null,
                null,
                null,
                null
            ).use { cursor ->
                val arrayList = ArrayList<EnvInfo>()
                while (cursor.moveToNext()) {

                    arrayList.add(EnvInfo().apply {
                        key = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_KEY))
                        value = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_VALUE))
                        enabled = cursor.getInt(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_ENABLED)) == 1
                    })
                }
                return arrayList
            }
        }
}