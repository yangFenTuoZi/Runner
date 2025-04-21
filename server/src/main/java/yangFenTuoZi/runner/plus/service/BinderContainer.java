package yangFenTuoZi.runner.plus.service;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class BinderContainer implements Parcelable {
    private final IBinder binder;

    public BinderContainer(IBinder binder) {
        this.binder = binder;
    }


    public IBinder getBinder() {
        return binder;
    }

    protected BinderContainer(Parcel in) {
        binder = in.readStrongBinder();
    }

    public static final Creator<BinderContainer> CREATOR = new Creator<BinderContainer>() {
        @Override
        public BinderContainer createFromParcel(Parcel in) {
            return new BinderContainer(in);
        }

        @Override
        public BinderContainer[] newArray(int size) {
            return new BinderContainer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(binder);
    }
}