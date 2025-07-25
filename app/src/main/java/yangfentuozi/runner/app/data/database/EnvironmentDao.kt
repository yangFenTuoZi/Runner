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
    fun update(key: String?, value: String?): Boolean {
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

    fun update(fromKey: String?, fromValue: String?, toKey: String?, toValue: String?): Boolean {
        val values = ContentValues()
        values.put(DataDbHelper.COLUMN_KEY, toKey)
        values.put(DataDbHelper.COLUMN_VALUE, toValue)

        val rowsAffected = db.update(
            DataDbHelper.TABLE_ENVIRONMENT,
            values,
            DataDbHelper.COLUMN_KEY + " = ? AND " + DataDbHelper.COLUMN_VALUE + " = ?",
            arrayOf(fromKey, fromValue)
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
                arrayOf(DataDbHelper.COLUMN_KEY, DataDbHelper.COLUMN_VALUE),
                null,
                null,
                null,
                null,
                null
            ).use { cursor ->
                val arrayList = ArrayList<EnvInfo>()
                while (cursor.moveToNext()) {
                    val key = cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_KEY))
                    val value =
                        cursor.getString(cursor.getColumnIndexOrThrow(DataDbHelper.COLUMN_VALUE))
                    val envInfo = EnvInfo()
                    envInfo.key = key
                    envInfo.value = value
                    arrayList.add(envInfo)
                }
                return arrayList
            }
        }
}