package yangfentuozi.runner.ui.activity

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Toast
import androidx.core.net.toUri
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseActivity
import yangfentuozi.runner.databinding.ActivityStreamActivityBinding
import yangfentuozi.runner.service.callback.IInstallTermExtCallback


class InstallTermExtActivity : BaseActivity() {
    private lateinit var binding: ActivityStreamActivityBinding
    private var callback: IInstallTermExtCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStreamActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.appBar.setLiftable(true)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        binding.text1.typeface = Typeface.MONOSPACE

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_VIEW == action && type != null) {
            val uri = intent.data
            if (uri != null)
                handleReceivedFile(uri)
            else {
                Toast.makeText(this, "Invalid file", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (Intent.ACTION_SEND == action && type != null) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null)
                handleReceivedFile(uri)
            else {
                Toast.makeText(this, "Invalid file", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Invalid intent", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleReceivedFile(uri: Uri) {
        val path = getPathFromUri(this, uri)
        if (path != null) {
            if (!Runner.pingServer()) {
                Toast.makeText(this, R.string.service_not_running, Toast.LENGTH_SHORT)
                    .show()
                finish()
            }

            callback = object : IInstallTermExtCallback.Stub() {
                override fun onMessage(message: String?) {
                    runOnUiThread {
                        binding.text1.append(message + "\n")
                    }
                }

                override fun onExit(isSuccessful: Boolean) {
                    onMessage(if (isSuccessful) " - Installation successful" else " ! Installation failed")
                    callback = null
                }
            }

            Runner.service?.installTermExt(path, callback)
        } else {
            Toast.makeText(this, "Failed to get file path", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
    }

    fun getPathFromUri(context: Context, uri: Uri): String? {
        var absolutePath: String? = null


        // 检查 Uri 的 scheme
        val uriScheme = uri.scheme

        if (ContentResolver.SCHEME_FILE == uriScheme) {
            // 如果是 file:// Uri，直接获取路径
            absolutePath = uri.path
        } else if (ContentResolver.SCHEME_CONTENT == uriScheme) {
            // 如果是 content:// Uri，使用 ContentResolver 查询
            absolutePath = if (DocumentsContract.isDocumentUri(context, uri)) {
                // 处理 DocumentProvider 提供的 Uri
                getPathFromDocumentUri(context, uri)
            } else {
                // 处理普通 ContentProvider 提供的 Uri
                getDataColumn(context, uri, null, null)
            }
        }

        return absolutePath
    }

    private fun getPathFromDocumentUri(context: Context, uri: Uri): String? {
        // 检查 Uri 的 authority
        if (isExternalStorageDocument(uri)) {
            // 处理外部存储文档
            val docId = DocumentsContract.getDocumentId(uri)
            val split: Array<String?> =
                docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
            // 处理其他存储设备
        } else if (isDownloadsDocument(uri)) {
            // 处理下载目录中的文档
            val id = DocumentsContract.getDocumentId(uri)
            val contentUri = ContentUris.withAppendedId(
                "content://downloads/public_downloads".toUri(), id.toLong()
            )

            return getDataColumn(context, contentUri, null, null)
        } else if (isMediaDocument(uri)) {
            // 处理媒体文档
            val docId = DocumentsContract.getDocumentId(uri)
            val split: Array<String?> =
                docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            var contentUri: Uri? = null
            if ("image" == type) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else if ("video" == type) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if ("audio" == type) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val selection = "_id=?"
            val selectionArgs = arrayOf<String?>(split[1])

            return contentUri?.let { getDataColumn(context, it, selection, selectionArgs) }
        }

        return null
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String?>?
    ): String? {
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf<String?>(column)

        try {
            context.contentResolver.query(
                uri, projection, selection, selectionArgs, null
            ).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}