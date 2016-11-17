package freelancerwatermellon.serbianeidomnikey.cardreadermanager.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;

import java.nio.ByteBuffer;

import freelancerwatermellon.serbianeidomnikey.cardreadermanager.BuildConfig;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.Constants;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.ICommunicator;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid.CcidDescriptor;

public class UsbCommunicator implements ICommunicator {
    private static final int BULK_TRANSFER_TIMEOUT = 100;
    private UsbEndpoint mBulkEndpointIn;
    private UsbEndpoint mBulkEndpointOut;
    private CcidDescriptor mCcidDescriptor;
    private UsbDeviceConnection mConnection;
    private UsbDevice mDevice;
    private int mDeviceId;
    private String mDeviceName;
    private UsbEndpoint mIntrEndpoint;
    private int mProductId;

    public UsbCommunicator(UsbManager usbManager, UsbDevice usbDevice) {
        this.mDevice = null;
        this.mConnection = null;
        this.mIntrEndpoint = null;
        this.mBulkEndpointIn = null;
        this.mBulkEndpointOut = null;
        this.mDeviceId = 0;
        this.mProductId = 0;
        this.mDeviceName = null;
        this.mCcidDescriptor = null;
        this.mDevice = usbDevice;
        this.mConnection = usbManager.openDevice(this.mDevice);
        this.mDeviceId = usbDevice.getDeviceId();
        this.mProductId = usbDevice.getProductId();
        this.mDeviceName = "USB [" + usbDevice.getDeviceId() + "] " + ((String) Constants.USB_VENDORS.get(usbDevice.getVendorId())) + " " + ((String) Constants.USB_PRODUCTS.get(usbDevice.getProductId()));
    }

    public boolean initialize() {
        if (this.mConnection != null && readCcidDescriptor() && findEndpoints()) {
            return true;
        }
        return false;
    }

    public void shutdown() {
        this.mConnection.close();
    }

    public int getType() {
        return 1;
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
        return BuildConfig.FLAVOR;
    }

    public boolean bulkIn(ByteBuffer buffer, int length) {
        if (this.mBulkEndpointIn == null) {
            return false;
        }
        boolean ret = false;
        UsbRequest request = new UsbRequest();
        request.initialize(this.mConnection, this.mBulkEndpointIn);
        request.queue(buffer, length);
        if (this.mConnection.requestWait() == request) {
            ret = true;
        } else {
            Util.logDebug("requestWait returned null");
        }
        request.close();
        return ret;
    }

    public boolean bulkOut(byte[] buffer) {
        if (this.mBulkEndpointOut == null) {
            return false;
        }
        int num = this.mConnection.bulkTransfer(this.mBulkEndpointOut, buffer, buffer.length, BULK_TRANSFER_TIMEOUT);
        if (num == buffer.length) {
            return true;
        }
        Util.logError("only transferred " + num + "/" + buffer.length + " bytes");
        return false;
    }

    private boolean readCcidDescriptor() {
        byte[] rawDescriptors = this.mConnection.getRawDescriptors();
        int ccidDescriptorStart = findCcidDescriptorStart(rawDescriptors);
        if (ccidDescriptorStart == -1) {
            Util.logError("unable to find descriptor start");
            return false;
        }
        this.mCcidDescriptor = new CcidDescriptor();
        if (this.mCcidDescriptor.parseRawDescriptor(rawDescriptors, ccidDescriptorStart)) {
            Util.logDebug(this.mCcidDescriptor.toString());
            return true;
        }
        Util.logError("unable to parse descriptor");
        return false;
    }

    private int findCcidDescriptorStart(byte[] rawDescriptors) {
        int start = 0;
        while (start < rawDescriptors.length) {
            int len = rawDescriptors[start];
            if (rawDescriptors[start + 1] == 33) {
                return start;
            }
            start += len;
        }
        return -1;
    }

    private boolean findEndpoints() {
        UsbInterface usbInterface = this.mDevice.getInterface(0);
        if (this.mConnection.claimInterface(usbInterface, true)) {
            for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(i);
                if (usbEndpoint.getType() == 3) {
                    this.mIntrEndpoint = usbEndpoint;
                } else if (usbEndpoint.getType() == 2) {
                    switch (usbEndpoint.getDirection()) {
                        case 0 /*0*/:
                            this.mBulkEndpointOut = usbEndpoint;
                            break;
                        case 128 /*128*/:
                            this.mBulkEndpointIn = usbEndpoint;
                            break;
                        default:
                            break;
                    }
                }
            }
            if (this.mIntrEndpoint != null && this.mBulkEndpointIn != null && this.mBulkEndpointOut != null) {
                return true;
            }
            Util.logError("cannot find all endpoints");
            return false;
        }
        Util.logError("cannot claim interface");
        return false;
    }
}
