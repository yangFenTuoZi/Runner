package yangfentuozi.runner.shared.data

import android.os.Parcel
import android.os.Parcelable
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Properties

data class TermModuleInfo(
    val moduleId: String?,
    val moduleName: String?,
    val versionName: String?,
    val versionCode: Long,
    val author: String?,
    val description: String?,
    val updateJson: String?,
) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(moduleId)
        dest.writeString(moduleName)
        dest.writeString(versionName)
        dest.writeLong(versionCode)
        dest.writeString(author)
        dest.writeString(description)
        dest.writeString(updateJson)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TermModuleInfo?> =
            object : Parcelable.Creator<TermModuleInfo?> {
                override fun createFromParcel(source: Parcel): TermModuleInfo {
                    return fromParcel(source)
                }

                override fun newArray(size: Int): Array<TermModuleInfo?> {
                    return arrayOfNulls(size)
                }
            }

        fun fromParcel(`in`: Parcel): TermModuleInfo {
            return TermModuleInfo(
                moduleId = `in`.readString(),
                moduleName = `in`.readString(),
                versionName = `in`.readString(),
                versionCode = `in`.readLong(),
                author = `in`.readString(),
                description = `in`.readString(),
                updateJson = `in`.readString(),
            )
        }

        fun fromInputStream(`in`: InputStream?): TermModuleInfo {
            val moduleProp = Properties().apply {
                load(InputStreamReader(`in`, "UTF-8"))
            }
            return TermModuleInfo(
                moduleId = moduleProp.getProperty("id"),
                moduleName = moduleProp.getProperty("name"),
                versionName = moduleProp.getProperty("version"),
                versionCode = (moduleProp.getProperty("versionCode") ?: "-1").toLongOrNull() ?: -1,
                author = moduleProp.getProperty("author"),
                description = moduleProp.getProperty("description"),
                updateJson = moduleProp.getProperty("updateJson")
            )
        }
    }
}