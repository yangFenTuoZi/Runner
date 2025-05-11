package yangfentuozi.runner.service.data;

import android.os.Parcel;
import android.os.Parcelable;

public class EnvInfo implements Parcelable {

    public String key;
    public String value;

    public EnvInfo() {
    }

    public EnvInfo(Parcel source) {
        super();
        key = source.readString();
        value = source.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(value);
    }

    public static final Parcelable.Creator<EnvInfo> CREATOR = new Parcelable.Creator<>() {
        public EnvInfo createFromParcel(Parcel source) {
            return new EnvInfo(source);
        }

        public EnvInfo[] newArray(int size) {
            return new EnvInfo[size];
        }
    };
}
