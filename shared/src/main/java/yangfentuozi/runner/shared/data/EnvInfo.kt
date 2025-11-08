package yangfentuozi.runner.shared.data

import android.os.Parcel
import android.os.Parcelable

open class EnvInfo : Parcelable {
    var key: String? = null
    var value: String? = null
    var enabled: Boolean = true

    constructor()

    constructor(source: Parcel) : super() {
        key = source.readString()
        value = source.readString()
        enabled = source.readBoolean()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(key)
        dest.writeString(value)
        dest.writeBoolean(enabled)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<EnvInfo?> = object : Parcelable.Creator<EnvInfo?> {
            override fun createFromParcel(source: Parcel): EnvInfo {
                return EnvInfo(source)
            }

            override fun newArray(size: Int): Array<EnvInfo?> {
                return arrayOfNulls<EnvInfo>(size)
            }
        }
    }
}