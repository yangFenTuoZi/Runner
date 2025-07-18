package yangfentuozi.runner.shared.data

import android.os.Parcel
import android.os.Parcelable
import java.io.InputStream
import java.util.Properties

open class TermExtVersion : Parcelable {
    val versionName: String?
    val versionCode: Int
    val abi: String?

    constructor(versionName: String?, versionCode: Int, abi: String?) {
        this.versionName = versionName
        this.versionCode = versionCode
        this.abi = abi
    }

    constructor(`in`: InputStream?) {
        var versionCode1: Int
        val buildProp = Properties()
        buildProp.load(`in`)
        versionName = buildProp.getProperty("version.name")
        try {
            versionCode1 = buildProp.getProperty("version.code").toInt()
        } catch (e: NumberFormatException) {
            versionCode1 = -1
        }
        versionCode = versionCode1
        abi = buildProp.getProperty("build.abi")
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.versionName)
        dest.writeInt(this.versionCode)
        dest.writeString(this.abi)
    }

    protected constructor(`in`: Parcel) {
        this.versionName = `in`.readString()
        this.versionCode = `in`.readInt()
        this.abi = `in`.readString()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TermExtVersion?> =
            object : Parcelable.Creator<TermExtVersion?> {
                override fun createFromParcel(source: Parcel): TermExtVersion {
                    return TermExtVersion(source)
                }

                override fun newArray(size: Int): Array<TermExtVersion?> {
                    return arrayOfNulls<TermExtVersion>(size)
                }
            }
    }
}