package freelancerwatermellon.serbianeidomnikey.cardreadermanager.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.freelancewatermelon.licnakarta.cardreadermanager.ICommunicator;
import com.freelancewatermelon.licnakarta.cardreadermanager.Util;
import com.freelancewatermelon.licnakarta.cardreadermanager.ccid.CcidDescriptor;
import com.freelancewatermelon.licnakarta.cardreadermanager.ccid.DeviceCommunicator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BtCommunicator implements ICommunicator {
    private static final int BLUETOOTH_BAD_RESPONSE_PREFIX = 163;
    private static final int BLUETOOTH_COMMAND_PREFIX = 165;
    private static final UUID BT_SERIALPORT_UUID;
    private static final int READ_TIMEOUT_ASYNC = 35000;
    private static final int READ_TIMEOUT_BULK = 150000;
    private static final int READ_TIMEOUT_DEFAULT = 5000;
    private static final int READ_TIMEOUT_MIN = 1000;

    static {
        BT_SERIALPORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    private CcidDescriptor mCcidDescriptor;
    private BluetoothDevice mDevice;
    private int mDeviceId;
    private String mDeviceName;
    private byte[] mLastBulkOut;
    private int mNextBulkInTimeout;
    private int mProductId;
    private BluetoothSocket mSocket;
    private boolean mSocketConnected;

    public BtCommunicator(BluetoothDevice btDevice) {
        this.mDevice = null;
        this.mSocket = null;
        this.mSocketConnected = false;
        this.mDeviceId = 0;
        this.mProductId = 0;
        this.mDeviceName = null;
        this.mCcidDescriptor = null;
        this.mLastBulkOut = null;
        this.mNextBulkInTimeout = READ_TIMEOUT_DEFAULT;
        this.mDevice = btDevice;
        this.mDeviceId = btDevice.getAddress().hashCode();
        this.mDeviceName = btDevice.getAddress() + " (";
        if (btDevice.getName() != null) {
            this.mDeviceName += btDevice.getName();
        }
        this.mDeviceName += ")";
    }

    public void finalize() throws Throwable {
        shutdown();
    }

    public boolean initialize() {
        if (prepareCcidDescriptor()) {
            return true;
        }
        return false;
    }

    public void shutdown() {
        if (this.mSocketConnected) {
            try {
                this.mSocket.close();
                this.mSocketConnected = false;
            } catch (IOException e) {
                Util.logError("socket close: " + e.toString());
            }
        }
    }

    public int getType() {
        return 2;
    }

    public int getId() {
        return this.mDeviceId;
    }

    public int getProductId() {
        return this.mProductId;
    }

    public String getName() {
        return this.mDeviceName;
    }

    public CcidDescriptor getCcidDescriptor() {
        return this.mCcidDescriptor;
    }

    public String getMacAddress() {
        return this.mDevice.getAddress();
    }

    public boolean bulkIn(ByteBuffer buffer, int length) {
        if (!setupSocketConnection()) {
            return false;
        }
        while (true) {
            byte[] prefix = doBulkIn(1);
            if (prefix != null) {
                if ((prefix[0] & 0xff) == BLUETOOTH_COMMAND_PREFIX) {
                    break;
                }
                Util.logDebug("unexpected prefix " + Util.formatByte(prefix[0]));
            } else {
                return false;
            }
        }
        byte[] ccidHeader = doBulkIn(10);
        if (ccidHeader == null) {
            return false;
        }
        int dwLength = (((ccidHeader[1] & 0xff) | ((ccidHeader[2] << 8) & 0xff00)) | ((ccidHeader[3] << 16) & 16711680)) | ((ccidHeader[4] << 24) & 0xff000000);
        if (dwLength + 10 > length) {
            Util.logError("dwLength indicates more data than expected");
            return false;
        }
        byte[] abData = doBulkIn(dwLength + 1);
        if (abData == null) {
            return false;
        }
        buffer.clear();
        buffer.put(ccidHeader, 0, ccidHeader.length);
        if (dwLength > 0) {
            buffer.put(abData, 0, abData.length - 1);
        }
        return true;
    }

    public boolean bulkOut(byte[] buffer) {
        if (!setupSocketConnection()) {
            return false;
        }
        this.mNextBulkInTimeout = READ_TIMEOUT_DEFAULT;
        switch (buffer[0]) {
            case DeviceCommunicator.MESSAGE_TYPE_PC_SET_PARAMETERS /*97*/:
            case DeviceCommunicator.MESSAGE_TYPE_PC_ICC_POWER_OFF /*99*/:
            case DeviceCommunicator.MESSAGE_TYPE_PC_GET_SLOT_STATUS /*101*/:
            case DeviceCommunicator.MESSAGE_TYPE_PC_ESCAPE /*107*/:
            case DeviceCommunicator.MESSAGE_TYPE_PC_GET_PARAMETERS /*108*/:
                this.mNextBulkInTimeout = READ_TIMEOUT_MIN;
                break;
            case DeviceCommunicator.MESSAGE_TYPE_PC_ICC_POWER_ON /*98*/:
                this.mNextBulkInTimeout = READ_TIMEOUT_ASYNC;
                break;
            case DeviceCommunicator.MESSAGE_TYPE_PC_XFR_BLOCK /*111*/:
                this.mNextBulkInTimeout = READ_TIMEOUT_BULK;
                break;
        }
        Util.logDebug("set timeout for next bulk in to " + this.mNextBulkInTimeout);
        return doBulkOut(buffer, false);
    }

    private byte[] doBulkIn(int length) {
        byte[] data = new byte[length];
        int dataLength = 0;
        try {
            long startTime = System.currentTimeMillis();
            while (dataLength < length) {
                long curTime = System.currentTimeMillis();
                if (curTime - startTime > ((long) this.mNextBulkInTimeout)) {
                    Util.logError("timeout while reading from socket, start at " + startTime + ", current time is " + curTime);
                    return null;
                } else if (this.mSocket.getInputStream().available() > 0) {
                    int num = this.mSocket.getInputStream().read(data, dataLength, data.length - dataLength);
                    if (num >= 0 || dataLength >= length) {
                        dataLength += num;
                    } else {
                        Util.logError("end of stream reached prematurely");
                        return null;
                    }
                }
            }
            return data;
        } catch (IOException e) {
            Util.logError("socket read: " + e.toString());
            return null;
        }
    }

    private boolean doBulkOut(byte[] buffer, boolean prepared) {
        if (!prepared) {
            this.mLastBulkOut = new byte[(buffer.length + 2)];
            this.mLastBulkOut[0] = (byte) -91;
            System.arraycopy(buffer, 0, this.mLastBulkOut, 1, buffer.length);
            this.mLastBulkOut[this.mLastBulkOut.length - 1] = Util.lrc(buffer);
            Util.logDebug("prepared bulk out buffer with prefix and LRC");
        }
        try {
            Util.logDebug(Util.byteArrayToString(this.mLastBulkOut));
            this.mSocket.getOutputStream().write(this.mLastBulkOut);
            return true;
        } catch (IOException e) {
            Util.logError("socket write: " + e.toString());
            return false;
        }
    }

//    /* JADX WARNING: inconsistent code. */
//    /* Code decompiled incorrectly, please refer to instructions dump. */
//    private boolean setupSocketConnection() {
//        /*
//        r6 = this;
//        r3 = 0;
//        r2 = 1;
//        r4 = r6.mSocketConnected;
//        if (r4 == 0) goto L_0x0007;
//    L_0x0006:
//        return r2;
//    L_0x0007:
//        r1 = 0;
//    L_0x0008:
//        r4 = r6.mDevice;	 Catch:{ IOException -> 0x0038 }
//        r5 = BT_SERIALPORT_UUID;	 Catch:{ IOException -> 0x0038 }
//        r4 = r4.createRfcommSocketToServiceRecord(r5);	 Catch:{ IOException -> 0x0038 }
//        r6.mSocket = r4;	 Catch:{ IOException -> 0x0038 }
//        r4 = r6.mSocket;	 Catch:{ IOException -> 0x001b }
//        r4.connect();	 Catch:{ IOException -> 0x001b }
//        r4 = 1;
//        r6.mSocketConnected = r4;	 Catch:{ IOException -> 0x001b }
//        goto L_0x0006;
//    L_0x001b:
//        r0 = move-exception;
//        r2 = new java.lang.StringBuilder;
//        r2.<init>();
//        r4 = "unable to connect to socket: ";
//        r2 = r2.append(r4);
//        r4 = r0.toString();
//        r2 = r2.append(r4);
//        r2 = r2.toString();
//        com.theobroma.cardreadermanager.Util.logError(r2);
//        r2 = r3;
//        goto L_0x0006;
//    L_0x0038:
//        r0 = move-exception;
//        r4 = 3;
//        if (r1 != r4) goto L_0x0068;
//    L_0x003c:
//        r2 = new java.lang.StringBuilder;
//        r2.<init>();
//        r4 = "unable to create to socket for UUID ";
//        r2 = r2.append(r4);
//        r4 = BT_SERIALPORT_UUID;
//        r4 = r4.toString();
//        r2 = r2.append(r4);
//        r4 = ": ";
//        r2 = r2.append(r4);
//        r4 = r0.toString();
//        r2 = r2.append(r4);
//        r2 = r2.toString();
//        com.theobroma.cardreadermanager.Util.logError(r2);
//        r2 = r3;
//        goto L_0x0006;
//    L_0x0068:
//        r4 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
//        java.lang.Thread.sleep(r4);	 Catch:{ InterruptedException -> 0x0070 }
//    L_0x006d:
//        r1 = r1 + 1;
//        goto L_0x0008;
//    L_0x0070:
//        r4 = move-exception;
//        goto L_0x006d;
//        */
//        throw new UnsupportedOperationException("Method not decompiled: com.theobroma.cardreadermanager.bt.BtCommunicator.setupSocketConnection():boolean");
//    }

    private boolean setupSocketConnection() {
        // rewrite code
        if (this.mSocketConnected) return true; // already connected
        int i = 0;
        while (i < 3) { // try connecting 3 times
            try {
                this.mSocket = this.mDevice.createRfcommSocketToServiceRecord(BT_SERIALPORT_UUID);
                this.mSocket.connect();
                this.mSocketConnected = true;
                return true;
            } catch (IOException e) {
                Util.logError("unable to connect to socket: " + e.toString());
                if (i == 3) {
                    Util.logError("unable to create to socket for UUID " + BT_SERIALPORT_UUID.toString() + ": " + e.toString());
                    return false;
                }
            }
            try {
                Thread.sleep(1000L);
                i++;
            } catch (InterruptedException e) {
                for (; ; ) {
                }
            }
        }
        return false;
    }


    private boolean prepareCcidDescriptor() {
        byte[] raw = new byte[]{(byte) 54, (byte) 33, (byte) 1, (byte) 16, (byte) 0, (byte) 7, (byte) 3, (byte) 0, (byte) 0, (byte) 0, (byte) -64, (byte) 18, (byte) 0, (byte) 0, (byte) 64, (byte) 31, (byte) 0, (byte) 0, (byte) 4, (byte) 0, (byte) 42, (byte) 0, (byte) 0, (byte) -25, (byte) 76, (byte) 6, (byte) 0, (byte) 106, (byte) -2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 2, (byte) 0, (byte) 15, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        this.mCcidDescriptor = new CcidDescriptor();
        if (this.mCcidDescriptor.parseRawDescriptor(raw, 0)) {
            Util.logDebug(this.mCcidDescriptor.toString());
            return true;
        }
        Util.logError("unable to parse descriptor");
        return false;
    }
}
