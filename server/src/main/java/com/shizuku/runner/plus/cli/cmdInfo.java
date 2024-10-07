package com.shizuku.runner.plus.cli;

import android.os.Parcel;
import android.os.Parcelable;

public class cmdInfo implements Parcelable {

    public int id;
    public String name;
    public String command;
    public boolean keepAlive;
    public boolean useChid;
    public String ids;

    public cmdInfo() {
    }

    public cmdInfo(Parcel source) {
        super();
        id = source.readInt();
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
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(command);
        dest.writeInt(keepAlive ? 1 : 0);
        dest.writeInt(useChid ? 1 : 0);
        dest.writeString(ids);
    }

    public static final Parcelable.Creator<cmdInfo> CREATOR = new Parcelable.Creator<cmdInfo>() {
        public cmdInfo createFromParcel(Parcel source) {
            return new cmdInfo(source);
        }

        public cmdInfo[] newArray(int size) {
            return new cmdInfo[size];
        }
    };
}
