package yangfentuozi.runner.service.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

public class ProcessInfo implements Parcelable {

    public int pid;
    public int ppid;
    public String exe;
    public String[] args;

    public ProcessInfo() {
    }

    public ProcessInfo(Parcel source) {
        super();
        pid = source.readInt();
        ppid = source.readInt();
        exe = source.readString();
        int argsLength = source.readInt();
        args = new String[argsLength];
        source.readStringArray(args);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(pid);
        dest.writeInt(ppid);
        dest.writeString(exe);
        dest.writeInt(args.length);
        dest.writeStringArray(args);
    }

    public static final Parcelable.Creator<ProcessInfo> CREATOR = new Parcelable.Creator<>() {
        public ProcessInfo createFromParcel(Parcel source) {
            return new ProcessInfo(source);
        }

        public ProcessInfo[] newArray(int size) {
            return new ProcessInfo[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "ProcessInfo{" +
                "pid=" + pid +
                ", ppid=" + ppid +
                ", name='" + exe + '\'' +
                ", args=" + Arrays.toString(args) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProcessInfo that)) return false;
        return pid == that.pid;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pid);
    }
}
