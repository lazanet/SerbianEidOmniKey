package freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid;

import com.freelancewatermelon.licnakarta.cardreadermanager.BuildConfig;

public class CcidDescriptor {
    public static final int CCID_DESCRIPTOR_LENGTH = 54;
    public static final int CCID_DESCRIPTOR_TYPE = 33;
    public static final int FEAT_AUTOMATIC_BAUD_RATE_CHANGE = 32;
    public static final int FEAT_AUTOMATIC_ICC_ACTIVATION_AFTER_INSERT = 4;
    public static final int FEAT_AUTOMATIC_ICC_CLOCK_FREQ_CHANGE = 16;
    public static final int FEAT_AUTOMATIC_ICC_VOLTAGE_SELECTION = 8;
    public static final int FEAT_AUTOMATIC_IFSD_EXCHANGE = 1024;
    public static final int FEAT_AUTOMATIC_PARAMETER_CONFIGURATION = 2;
    public static final int FEAT_AUTOMATIC_PARAMETER_NEGOTIATION = 64;
    public static final int FEAT_AUTOMATIC_PPS = 128;
    public static final int FEAT_EXCHANGE_CHAR = 0;
    public static final int FEAT_EXCHANGE_EXTENDED = 262144;
    public static final int FEAT_EXCHANGE_MASK = 458752;
    public static final int FEAT_EXCHANGE_SHORT = 131072;
    public static final int FEAT_EXCHANGE_TPDU = 65536;
    public static final int FEAT_NAD_VALUE_OTHER_THAN_0 = 512;
    public static final int FEAT_SET_ICC = 256;
    public static final int FEAT_USB_WAKE_UP_SIGNALLING = 1048576;
    public static final int MECHANICAL_CARD_ACCEPT = 1;
    public static final int MECHANICAL_CARD_CAPTURE = 3;
    public static final int MECHANICAL_CARD_EJECTION = 2;
    public static final int MECHANICAL_CARD_LOCK_UNLOCK = 4;
    public static final int PIN_SUPPORT_MODIFICATION = 2;
    public static final int PIN_SUPPORT_VERIFICATION = 1;
    public static final int PROTOCOL_T0 = 1;
    public static final int PROTOCOL_T1 = 2;
    public static final int PROTOCOL_TYPE_2WIRE = 1;
    public static final int PROTOCOL_TYPE_3WIRE = 2;
    public static final int PROTOCOL_TYPE_I2C = 3;
    public static final int VOLTAGE_1_8 = 4;
    public static final int VOLTAGE_3_0 = 2;
    public static final int VOLTAGE_5_0 = 1;
    private int bClassEnvelope;
    private int bClassGetResponse;
    private int bMaxCCIDBusySlots;
    private int bMaxSlotIndex;
    private int bNumClockSupported;
    private int bNumDataRatesSupported;
    private int bPINSupport;
    private int bVoltageSupport;
    private int bcdCCID;
    private int dwDataRate;
    private int dwDefaultClock;
    private int dwFeatures;
    private int dwMaxCCIDMessageLength;
    private int dwMaxDataRate;
    private int dwMaxIFSD;
    private int dwMaximumClock;
    private int dwMechanical;
    private int dwProtocols;
    private int dwSynchProtocols;
    private boolean mInitialized;
    private int wLcdLayout;

    public CcidDescriptor() {
        this.mInitialized = false;
    }

    public boolean parseRawDescriptor(byte[] rawDescriptor, int start) {
        this.mInitialized = false;
        if (rawDescriptor[start] != CCID_DESCRIPTOR_LENGTH || rawDescriptor[start + VOLTAGE_5_0] != CCID_DESCRIPTOR_TYPE) {
            return false;
        }
        this.bcdCCID = shortFromByteArray(rawDescriptor, start + VOLTAGE_3_0);
        this.bMaxSlotIndex = byteFromByteArray(rawDescriptor, start + VOLTAGE_1_8);
        this.bVoltageSupport = byteFromByteArray(rawDescriptor, start + 5);
        this.dwProtocols = intFromByteArray(rawDescriptor, start + 6);
        this.dwDefaultClock = intFromByteArray(rawDescriptor, start + 10);
        this.dwMaximumClock = intFromByteArray(rawDescriptor, start + 14);
        this.bNumClockSupported = byteFromByteArray(rawDescriptor, start + 18);
        this.dwDataRate = intFromByteArray(rawDescriptor, start + 19);
        this.dwMaxDataRate = intFromByteArray(rawDescriptor, start + 23);
        this.bNumDataRatesSupported = byteFromByteArray(rawDescriptor, start + 27);
        this.dwMaxIFSD = intFromByteArray(rawDescriptor, start + 28);
        this.dwSynchProtocols = intFromByteArray(rawDescriptor, start + FEAT_AUTOMATIC_BAUD_RATE_CHANGE);
        this.dwMechanical = intFromByteArray(rawDescriptor, start + 36);
        this.dwFeatures = intFromByteArray(rawDescriptor, start + 40);
        this.dwMaxCCIDMessageLength = intFromByteArray(rawDescriptor, start + 44);
        this.bClassGetResponse = byteFromByteArray(rawDescriptor, start + 48);
        this.bClassEnvelope = byteFromByteArray(rawDescriptor, start + 49);
        this.wLcdLayout = shortFromByteArray(rawDescriptor, start + 50);
        this.bPINSupport = byteFromByteArray(rawDescriptor, start + 52);
        this.bMaxCCIDBusySlots = byteFromByteArray(rawDescriptor, start + 53);
        this.mInitialized = true;
        return true;
    }

    public int slotCount() {
        if (this.mInitialized) {
            return this.bMaxSlotIndex + VOLTAGE_5_0;
        }
        return -1;
    }

    public byte voltageSupport() {
        if ((this.bVoltageSupport & VOLTAGE_5_0) == VOLTAGE_5_0) {
            return (byte) 1;
        }
        if ((this.bVoltageSupport & VOLTAGE_3_0) == VOLTAGE_5_0) {
            return (byte) 2;
        }
        if ((this.bVoltageSupport & VOLTAGE_1_8) == VOLTAGE_5_0) {
            return (byte) 3;
        }
        return (byte) 0;
    }

    public boolean supportsProtocol(int protocol) {
        return (this.dwProtocols & protocol) != 0;
    }

    public int defaultClock() {
        return this.dwDefaultClock;
    }

    public int maxIFSD() {
        return this.dwMaxIFSD;
    }

    public int featExchange() {
        return this.dwFeatures & FEAT_EXCHANGE_MASK;
    }

    public int maxCCIDMessageLength() {
        return this.dwMaxCCIDMessageLength;
    }

    public String toString() {
        if (!this.mInitialized) {
            return "Not initialized";
        }
        return ((((((((((((((((((((((BuildConfig.FLAVOR + formatInt(CCID_DESCRIPTOR_LENGTH, "bLength: ", "\n")) + formatInt(CCID_DESCRIPTOR_TYPE, "bDescriptorType: ", "\n")) + formatInt(this.bcdCCID, "bcdCCID: ", "\n")) + formatInt(this.bMaxSlotIndex, "bMaxSlotIndex: ", "\n")) + formatInt(this.bVoltageSupport, "bVoltageSupport: ", "\n")) + formatInt(this.dwProtocols, "dwProtocols: ", "\n")) + formatInt(this.dwDefaultClock, "dwDefaultClock: ", "\n")) + formatInt(this.dwMaximumClock, "dwMaximumClock: ", "\n")) + formatInt(this.bNumClockSupported, "bNumClockSupported: ", "\n")) + formatInt(this.dwDataRate, "dwDataRate: ", "\n")) + formatInt(this.dwMaxDataRate, "dwMaxDataRate: ", "\n")) + formatInt(this.bNumDataRatesSupported, "bNumDataRatesSupported: ", "\n")) + formatInt(this.dwMaxIFSD, "dwMaxIFSD: ", "\n")) + formatInt(this.dwSynchProtocols, "dwSynchProtocols: ", "\n")) + formatInt(this.dwMechanical, "dwMachanical: ", "\n")) + formatInt(this.dwFeatures, "dwFeatures: ", "\n")) + formatInt(featExchange(), "ExchangeLevel: ", "\n")) + formatInt(this.dwMaxCCIDMessageLength, "dwMaxCCIDMessageLength: ", "\n")) + formatInt(this.bClassGetResponse, "bClassGetResponse: ", "\n")) + formatInt(this.bClassEnvelope, "bClassEnvelope: ", "\n")) + formatInt(this.wLcdLayout, "wLcdLayout: ", "\n")) + formatInt(this.bPINSupport, "bPINSupport: ", "\n")) + formatInt(this.bMaxCCIDBusySlots, "bMaxCCIDBusySlots: ", "\n");
    }

    private int byteFromByteArray(byte[] array, int position) {
        return array[position] & 0xff;
    }

    private int shortFromByteArray(byte[] array, int start) {
        return (array[start] & 0xff) | ((array[start + VOLTAGE_5_0] << FEAT_AUTOMATIC_ICC_VOLTAGE_SELECTION) & 0xff00);
    }

    private int intFromByteArray(byte[] array, int start) {
        return (((array[start] & 0xff) | ((array[start + VOLTAGE_5_0] << FEAT_AUTOMATIC_ICC_VOLTAGE_SELECTION) & 0xff00)) | ((array[start + VOLTAGE_3_0] << FEAT_AUTOMATIC_ICC_CLOCK_FREQ_CHANGE) & 16711680)) | ((array[start + PROTOCOL_TYPE_I2C] << 24) & 0xff000000);
    }

    private String formatInt(int toFormat, String prefix, String postfix) {
        return prefix + "0x" + Integer.toHexString(toFormat) + postfix;
    }
}
