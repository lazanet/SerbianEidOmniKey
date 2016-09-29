package freelancerwatermellon.serbianeidomnikey.cardreadermanager;

import android.os.Parcel;
import android.os.Parcelable;

public final class Device implements Parcelable {
    public static final Creator<Device> CREATOR;
    private boolean mActivated;
    private String mBtMacAddress;
    private int mDeviceId;
    private boolean mDeviceInUse;
    private String mDeviceName;
    private int mDeviceType;
    private String mUsbCcidDescriptor;

    /* renamed from: com.theobroma.cardreadermanager.Device.1 */
    static class C01271 implements Creator<Device> {
        C01271() {
        }

        public Device[] newArray(int size) {
            return new Device[size];
        }

        public Device createFromParcel(Parcel source) {
            return new Device(source);
        }
    }

    static {
        CREATOR = new C01271();
    }

    public Device(boolean active, int deviceId, int deviceType) {
        this.mActivated = true;
        this.mDeviceId = 0;
        this.mDeviceType = 0;
        this.mDeviceName = null;
        this.mDeviceInUse = false;
        this.mUsbCcidDescriptor = null;
        this.mBtMacAddress = null;
        this.mActivated = active;
        this.mDeviceId = deviceId;
        this.mDeviceType = deviceType;
    }

    public Device(Parcel source) {
        boolean z = true;
        this.mActivated = true;
        this.mDeviceId = 0;
        this.mDeviceType = 0;
        this.mDeviceName = null;
        this.mDeviceInUse = false;
        this.mUsbCcidDescriptor = null;
        this.mBtMacAddress = null;
        this.mActivated = source.readInt() != 0;
        this.mDeviceId = source.readInt();
        this.mDeviceType = source.readInt();
        this.mDeviceName = source.readString();
        if (source.readInt() == 0) {
            z = false;
        }
        this.mDeviceInUse = z;
        this.mUsbCcidDescriptor = source.readString();
        if (this.mDeviceType == 2) {
            this.mBtMacAddress = source.readString();
        }
    }

    public void setActivated(boolean active) {
        this.mActivated = active;
    }

    public boolean getActivated() {
        return this.mActivated;
    }

    public int getDeviceId() {
        return this.mDeviceId;
    }

    public int getDeviceType() {
        return this.mDeviceType;
    }

    public void setDeviceName(String deviceName) {
        this.mDeviceName = deviceName;
    }

    public String getDeviceName() {
        return this.mDeviceName;
    }

    public void setDeviceInUse(boolean inUse) {
        this.mDeviceInUse = inUse;
    }

    public boolean getDeviceInUse() {
        return this.mDeviceInUse;
    }

    public void setUsbCcidDescriptor(String usbCcidDescriptor) {
        this.mUsbCcidDescriptor = usbCcidDescriptor;
    }

    public String getUsbCcidDescriptor() {
        return this.mUsbCcidDescriptor;
    }

    public void setBtMacAddress(String address) {
        this.mBtMacAddress = address;
    }

    public String getMacAddress() {
        return this.mBtMacAddress;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i = 1;
        dest.writeInt(this.mActivated ? 1 : 0);
        dest.writeInt(this.mDeviceId);
        dest.writeInt(this.mDeviceType);
        dest.writeString(this.mDeviceName);
        if (!this.mDeviceInUse) {
            i = 0;
        }
        dest.writeInt(i);
        dest.writeString(this.mUsbCcidDescriptor);
        if (this.mDeviceType == 2) {
            dest.writeString(this.mBtMacAddress);
        }
    }
}
