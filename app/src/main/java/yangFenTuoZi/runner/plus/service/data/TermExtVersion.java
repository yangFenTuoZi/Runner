package yangFenTuoZi.runner.plus.service.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TermExtVersion implements Parcelable {

    public final String versionName;
    public final int versionCode;
    public final String abi;

    public TermExtVersion(String versionName, int versionCode, String abi) {
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.abi = abi;
    }

    public TermExtVersion(InputStream in) throws IOException {
        int versionCode1;
        Properties buildProp = new Properties();
        buildProp.load(in);
        versionName = buildProp.getProperty("version.name");
        try {
            versionCode1 = Integer.parseInt(buildProp.getProperty("version.code"));
        } catch (NumberFormatException e) {
            versionCode1 = -1;
        }
        versionCode = versionCode1;
        abi = buildProp.getProperty("build.abi");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.versionName);
        dest.writeInt(this.versionCode);
        dest.writeString(this.abi);
    }

    protected TermExtVersion(Parcel in) {
        this.versionName = in.readString();
        this.versionCode = in.readInt();
        this.abi = in.readString();
    }

    public static final Creator<TermExtVersion> CREATOR = new Creator<>() {
        @Override
        public TermExtVersion createFromParcel(Parcel source) {
            return new TermExtVersion(source);
        }

        @Override
        public TermExtVersion[] newArray(int size) {
            return new TermExtVersion[size];
        }
    };
}
