package yangFenTuoZi.runner.plus.util

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import yangFenTuoZi.runner.plus.util.ThrowableUtil.getStackTraceString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateUtil {

    fun wget(url: String): String? {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setConnectTimeout(30000)
            connection.setRequestMethod("GET")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            if (connection.responseCode == 200) {
                val result = StringBuilder()
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        result.append(line).append("\n")
                    }
                }
                return result.toString()
            }
            return null
        } catch (e: Exception) {
            Log.e("UpdateUtils", e.getStackTraceString())
            return null
        }
    }

    @Throws(UpdateException::class)
    private fun update(url: String): UpdateInfo {
        val jsonInfo = wget(url)
        if (jsonInfo == null) {
            val e = UpdateException()
            e.what = UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER
            throw e
        }
        return parseReleaseInfo(jsonInfo)
    }

    @Throws(UpdateException::class)
    fun update(isBeta: Boolean): UpdateInfo {
        val endpoint = if (isBeta) "releases?per_page=1" else "releases/latest"
        return update("https://api.github.com/repos/yangFenTuoZi/Runner/$endpoint")
    }

    @Throws(UpdateException::class)
    private fun parseReleaseInfo(jsonInfo: String): UpdateInfo {
        try {
            val jsonObject = if (jsonInfo.startsWith("[")) {
                // Handle array response (for prereleases)
                JSONObject(jsonInfo.substring(1, jsonInfo.length - 1))
            } else {
                // Handle object response (for latest release)
                JSONObject(jsonInfo)
            }

//            val tagName = jsonObject.getString("tag_name")
            val releaseName = jsonObject.getString("name")
            val body = jsonObject.getString("body")

            // Find app-release.apk in assets
            val assetsArray = jsonObject.getJSONArray("assets")
            var apkDownloadUrl: String? = null
            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                if (asset.getString("name") == "app-release.apk") {
                    apkDownloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (apkDownloadUrl == null) {
                val e = UpdateException("No app-release.apk found in assets")
                e.what = UpdateException.WHAT_JSON_FORMAT_ERROR
                throw e
            }

            // Parse version name and code from release name (format: "verName (verCode)")
            val versionRegex = """(.+)\s+\((\d+)\)""".toRegex()
            val matchResult = versionRegex.find(releaseName)

            if (matchResult == null) {
                val e = UpdateException("Invalid release name format")
                e.what = UpdateException.WHAT_JSON_FORMAT_ERROR
                throw e
            }

            val (versionName, versionCodeStr) = matchResult.destructured
            val versionCode = versionCodeStr.toInt()

            return UpdateInfo(
                versionName = versionName,
                versionCode = versionCode,
                updateUrl = apkDownloadUrl,
                updateMsg = body
            )
        } catch (e: JSONException) {
            val e1 = UpdateException(e)
            e1.what = UpdateException.WHAT_JSON_FORMAT_ERROR
            throw e1
        } catch (e: NumberFormatException) {
            val e1 = UpdateException(e)
            e1.what = UpdateException.WHAT_JSON_FORMAT_ERROR
            throw e1
        } catch (e: IllegalStateException) {
            val e1 = UpdateException(e)
            e1.what = UpdateException.WHAT_JSON_FORMAT_ERROR
            throw e1
        }
    }

    class UpdateException : RuntimeException {
        var what: Int = 0

        constructor() : super()
        constructor(message: String?) : super(message)
        constructor(message: String?, cause: Throwable?) : super(message, cause)
        constructor(cause: Throwable?) : super(cause)

        companion object {
            const val WHAT_CAN_NOT_CONNECT_UPDATE_SERVER: Int = 0
            const val WHAT_CAN_NOT_PARSE_JSON: Int = 1
            const val WHAT_JSON_FORMAT_ERROR: Int = 2
        }
    }

    class UpdateInfo(
        var versionName: String?,
        var versionCode: Int,
        var updateUrl: String?,
        var updateMsg: String?
    )
}