package yangfentuozi.runner.service.data;

import android.os.Parcel;
import android.os.Parcelable;

public class CommandInfo implements Parcelable {

    public String name;
    public String command;
    public boolean keepAlive;
    public boolean reducePerm;
    public String targetPerm;

    public CommandInfo() {
    }

    public CommandInfo(Parcel source) {
        super();
        name = source.readString();
        command = source.readString();
        keepAlive = source.readInt() == 1;
        reducePerm = source.readInt() == 1;
        targetPerm = source.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(command);
        dest.writeInt(keepAlive ? 1 : 0);
        dest.writeInt(reducePerm ? 1 : 0);
        dest.writeString(targetPerm);
    }

    public static final Parcelable.Creator<CommandInfo> CREATOR = new Parcelable.Creator<>() {
        public CommandInfo createFromParcel(Parcel source) {
            return new CommandInfo(source);
        }

        public CommandInfo[] newArray(int size) {
            return new CommandInfo[size];
        }
    };
}
