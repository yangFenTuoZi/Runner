package yangfentuozi.runner.app.data

import android.content.Context
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.data.database.CommandDao
import yangfentuozi.runner.app.data.database.DataDbHelper
import yangfentuozi.runner.app.data.database.EnvironmentDao
import yangfentuozi.runner.shared.data.CommandInfo
import yangfentuozi.runner.shared.data.EnvInfo
import java.util.concurrent.Executors

class DataRepository private constructor(context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val dbHelper = DataDbHelper(context.applicationContext)
    private val commandDao = CommandDao(dbHelper.writableDatabase)
    private val environmentDao = EnvironmentDao(dbHelper.writableDatabase)

    private fun close() {
        dbHelper.close()
    }

    // Command Operations
    fun getAllCommands(): List<CommandInfo> = commandDao.readAll()
    fun addCommand(commandInfo: CommandInfo) {
        commandDao.insert(commandInfo)
    }
    fun addCommand(commandInfo: CommandInfo, position: Int) {
        commandDao.insertInto(commandInfo, position)
    }

    fun updateCommand(commandInfo: CommandInfo, position: Int) {
        commandDao.edit(commandInfo, position)
    }
    fun deleteCommand(position: Int) {
        commandDao.delete(position)
    }
    fun moveCommand(fromPosition: Int, toPosition: Int) {
        commandDao.move(fromPosition, toPosition)
    }

    // Environment Operations
    fun getAllEnvs(): List<EnvInfo> = environmentDao.all
    fun addEnv(key: String, value: String) {
        environmentDao.insert(key, value)
        syncToService()
    }
    fun updateEnv(fromKey: String, fromValue: String, toKey: String, toValue: String) {
        environmentDao.update(fromKey, fromValue, toKey, toValue)
        syncToService()
    }
    fun deleteEnv(key: String) {
        environmentDao.delete(key)
        syncToService()
    }

    private fun syncToService() {
        executor.execute {
            Runner.service?.syncAllData(getAllEnvs())
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DataRepository? = null

        fun getInstance(context: Context): DataRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DataRepository(context)
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}