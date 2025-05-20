package yangfentuozi.runner.service.data

import android.os.Parcel
import android.os.Parcelable
import java.util.Objects

open class ProcessInfo : Parcelable {
    var pid: Int = 0
    var ppid: Int = 0
    var exe: String? = null
    var args: Array<String?>? = null

    constructor()

    constructor(source: Parcel) : super() {
        pid = source.readInt()
        ppid = source.readInt()
        exe = source.readString()
        val argsLength = source.readInt()
        args = arrayOfNulls<String>(argsLength)
        source.readStringArray(args!!)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(pid)
        dest.writeInt(ppid)
        dest.writeString(exe)
        dest.writeInt(args?.size ?: 0)
        dest.writeStringArray(args)
    }

    override fun toString(): String {
        return "ProcessInfo{" +
                "pid=" + pid +
                ", ppid=" + ppid +
                ", name='" + exe + '\'' +
                ", args=" + args.contentToString() +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (o !is ProcessInfo) return false
        return pid == o.pid
    }

    override fun hashCode(): Int {
        return Objects.hashCode(pid)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ProcessInfo?> = object : Parcelable.Creator<ProcessInfo?> {
            override fun createFromParcel(source: Parcel): ProcessInfo {
                return ProcessInfo(source)
            }

            override fun newArray(size: Int): Array<ProcessInfo?> {
                return arrayOfNulls<ProcessInfo>(size)
            }
        }
    }
}