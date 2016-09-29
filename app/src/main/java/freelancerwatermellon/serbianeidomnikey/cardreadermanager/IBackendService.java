package freelancerwatermellon.serbianeidomnikey.cardreadermanager;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.List;

public interface IBackendService extends IInterface {

    boolean activateDevice(int i, boolean z) throws RemoteException;

    boolean addBtDevice(String str) throws RemoteException;

    Device getDevice(int i) throws RemoteException;

    List<Device> getDevices() throws RemoteException;

    boolean removeBtDevice(int i, String str) throws RemoteException;

    boolean setBtDevicesFromPreferences() throws RemoteException;

    public static abstract class Stub extends Binder implements IBackendService {
        static final int TRANSACTION_activateDevice = 6;
        static final int TRANSACTION_addBtDevice = 1;
        static final int TRANSACTION_getDevice = 5;
        static final int TRANSACTION_getDevices = 4;
        static final int TRANSACTION_removeBtDevice = 3;
        static final int TRANSACTION_setBtDevicesFromPreferences = 2;
        private static final String DESCRIPTOR = "com.theobroma.cardreadermanager.IBackendService";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBackendService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IBackendService)) {
                return new Proxy(obj);
            }
            return (IBackendService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = 0;
            boolean _result;
            switch (code) {
                case TRANSACTION_addBtDevice /*1*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = addBtDevice(data.readString());
                    reply.writeNoException();
                    if (_result) {
                        i = TRANSACTION_addBtDevice;
                    }
                    reply.writeInt(i);
                    return true;
                case TRANSACTION_setBtDevicesFromPreferences /*2*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = setBtDevicesFromPreferences();
                    reply.writeNoException();
                    if (_result) {
                        i = TRANSACTION_addBtDevice;
                    }
                    reply.writeInt(i);
                    return true;
                case TRANSACTION_removeBtDevice /*3*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = removeBtDevice(data.readInt(), data.readString());
                    reply.writeNoException();
                    if (_result) {
                        i = TRANSACTION_addBtDevice;
                    }
                    reply.writeInt(i);
                    return true;
                case TRANSACTION_getDevices /*4*/:
                    data.enforceInterface(DESCRIPTOR);
                    List<Device> _result2 = getDevices();
                    reply.writeNoException();
                    reply.writeTypedList(_result2);
                    return true;
                case TRANSACTION_getDevice /*5*/:
                    data.enforceInterface(DESCRIPTOR);
                    Device _result3 = getDevice(data.readInt());
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(TRANSACTION_addBtDevice);
                        _result3.writeToParcel(reply, TRANSACTION_addBtDevice);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case TRANSACTION_activateDevice /*6*/:
                    boolean _arg1;
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = true;
                    } else {
                        _arg1 = false;
                    }
                    _result = activateDevice(_arg0, _arg1);
                    reply.writeNoException();
                    if (_result) {
                        i = TRANSACTION_addBtDevice;
                    }
                    reply.writeInt(i);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IBackendService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public boolean addBtDevice(String deviceAddress) throws RemoteException {
                boolean _result = true;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceAddress);
                    this.mRemote.transact(Stub.TRANSACTION_addBtDevice, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            public boolean setBtDevicesFromPreferences() throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_setBtDevicesFromPreferences, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            public boolean removeBtDevice(int deviceId, String deviceAddress) throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeString(deviceAddress);
                    this.mRemote.transact(Stub.TRANSACTION_removeBtDevice, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            public List<Device> getDevices() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getDevices, _data, _reply, 0);
                    _reply.readException();
                    List<Device> _result = _reply.createTypedArrayList(Device.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Device getDevice(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Device _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(Stub.TRANSACTION_getDevice, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Device) Device.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }

                return null;
            }

            public boolean activateDevice(int deviceId, boolean active) throws RemoteException {
                boolean _result = true;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    int i;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    if (active) {
                        i = Stub.TRANSACTION_addBtDevice;
                    } else {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_activateDevice, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
    }
}
