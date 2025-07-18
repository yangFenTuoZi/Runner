package yangfentuozi.runner.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.data.database.DataDbHelper
import yangfentuozi.runner.server.ServerMain
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors

object BackupManager {

    private const val TAG = "BackupManager"
    private const val SETTINGS_FILE_NAME = "settings.xml"
    private const val DATABASE_FILE_NAME = "data.db"
    private val executor = Executors.newSingleThreadExecutor()

    fun backup(
        context: Context,
        uri: Uri,
        backupAppSettings: Boolean,
        backupDataDb: Boolean,
        backupTermHome: Boolean,
        backupTermUsr: Boolean
    ) {
        executor.execute {
            val backupTmpDir = File(context.externalCacheDir, "backupData")
            try {
                backupTmpDir.deleteRecursively()
                backupTmpDir.mkdirs()

                val restricted = !Runner.pingServer()
                if (!restricted) {
                    Runner.service?.backupData(backupTmpDir.absolutePath, backupTermHome, backupTermUsr)
                }

                if (backupAppSettings) {
                    val prefsFile = File(context.applicationInfo.dataDir + "/shared_prefs/${context.packageName}_preferences.xml")
                    if (prefsFile.exists()) {
                        prefsFile.copyTo(File(backupTmpDir, SETTINGS_FILE_NAME), true)
                    }
                }

                if (backupDataDb) {
                    val dbFile = context.getDatabasePath(DataDbHelper.DATABASE_NAME)
                    if (dbFile.exists()) {
                        dbFile.copyTo(File(backupTmpDir, DATABASE_FILE_NAME), true)
                    }
                }

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    TarArchiveOutputStream(output).use { taos ->
                        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
                        ServerMain.tarFileRecursive(backupTmpDir, backupTmpDir, taos)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
            } finally {
                backupTmpDir.deleteRecursively()
            }
        }
    }

    fun restore(context: Context, uri: Uri, onFinished: () -> Unit) {
        executor.execute {
            val restoreTmpDir = File(context.externalCacheDir, "restoreData")
            try {
                restoreTmpDir.deleteRecursively()
                restoreTmpDir.mkdirs()

                context.contentResolver.openInputStream(uri)?.use { fis ->
                    TarArchiveInputStream(fis).use { tais ->
                        var entry: TarArchiveEntry?
                        while (tais.nextEntry.also { entry = it } != null) {
                            val outFile = File(restoreTmpDir, entry!!.name)
                            if (entry.isDirectory) {
                                if (!outFile.exists()) outFile.mkdirs()
                            } else if (entry.isSymbolicLink) {
                                continue
                            } else {
                                val parent = outFile.parentFile
                                if (parent != null && !parent.exists()) parent.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    val buffer = ByteArray(ServerMain.PAGE_SIZE)
                                    var len: Int
                                    while ((tais.read(buffer).also { len = it }) != -1) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                                outFile.setLastModified(entry.modTime.time)
                                outFile.setExecutable((entry.mode and 64) != 0)
                            }
                        }
                    }
                }

                val settingsFile = File(restoreTmpDir, SETTINGS_FILE_NAME)
                if (settingsFile.exists()) {
                    val prefsFile = File(context.applicationInfo.dataDir + "/shared_prefs/${context.packageName}_preferences.xml")
                    prefsFile.parentFile?.mkdirs()
                    settingsFile.copyTo(prefsFile, true)
                }

                val dbFile = File(restoreTmpDir, DATABASE_FILE_NAME)
                if (dbFile.exists()) {
                    val targetDbFile = context.getDatabasePath(DataDbHelper.DATABASE_NAME)
                    targetDbFile.parentFile?.mkdirs()
                    dbFile.copyTo(targetDbFile, true)
                }

                Runner.service?.restoreData(restoreTmpDir.absolutePath)
                (context.applicationContext as App).reinitializeDataRepository()

            } catch (e: IOException) {
                Log.e(TAG, "Restore failed", e)
            } finally {
                restoreTmpDir.deleteRecursively()
                onFinished()
            }
        }
    }
}