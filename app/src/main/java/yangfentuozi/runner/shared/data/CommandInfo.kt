package yangfentuozi.runner.shared.data

import android.os.BaseBundle
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

open class CommandInfo : Parcelable {
    var name: String? = null
    var command: String? = null
    var keepAlive: Boolean = false
    var reducePerm: Boolean = false
    var targetPerm: String? = null

    constructor()

    constructor(source: Parcel) : super() {
        name = source.readString()
        command = source.readString()
        keepAlive = source.readInt() == 1
        reducePerm = source.readInt() == 1
        targetPerm = source.readString()
    }

    constructor(bundle: BaseBundle) : super() {
        name = bundle.getString("name")
        command = bundle.getString("command")
        keepAlive = bundle.getBoolean("keepAlive", false)
        reducePerm = bundle.getBoolean("reducePerm", false)
        targetPerm = bundle.getString("targetPerm")
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeString(command)
        dest.writeInt(if (keepAlive) 1 else 0)
        dest.writeInt(if (reducePerm) 1 else 0)
        dest.writeString(targetPerm)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CommandInfo?> = object : Parcelable.Creator<CommandInfo?> {
            override fun createFromParcel(source: Parcel): CommandInfo {
                return CommandInfo(source)
            }

            override fun newArray(size: Int): Array<CommandInfo?> {
                return arrayOfNulls<CommandInfo>(size)
            }
        }
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("name", name)
            putString("command", command)
            putBoolean("keepAlive", keepAlive)
            putBoolean("reducePerm", reducePerm)
            putString("targetPerm", targetPerm)
        }
    }
}