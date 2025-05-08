package yangfentuozi.runner.service.data;

import android.os.Parcel;
import android.os.Parcelable;

public class CommandInfo implements Parcelable {

//    public int id;
    public String name;
    public String command;
    public boolean keepAlive;
    public boolean useChid;
    public String ids;

    public CommandInfo() {
    }

    public CommandInfo(Parcel source) {
        super();
//        id = source.readInt();
        name = source.readString();
        command = source.readString();
        keepAlive = source.readInt() == 1;
        useChid = source.readInt() == 1;
        ids = source.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(command);
        dest.writeInt(keepAlive ? 1 : 0);
        dest.writeInt(useChid ? 1 : 0);
        dest.writeString(ids);
    }

    public static final Parcelable.Creator<CommandInfo> CREATOR = new Parcelable.Creator<>() {
        public CommandInfo createFromParcel(Parcel source) {
            return new CommandInfo(source);
        }

        public CommandInfo[] newArray(int size) {
            return new CommandInfo[size];
        }
    };

//    private String encBase64(String text) {
//        return new String(Base64.getEncoder().encode(text.getBytes()));
////        return text;
//    }
//
//    private String decBase64(String text) {
//        return new String(Base64.getDecoder().decode(text.getBytes()));
////        return text;
//    }
}
