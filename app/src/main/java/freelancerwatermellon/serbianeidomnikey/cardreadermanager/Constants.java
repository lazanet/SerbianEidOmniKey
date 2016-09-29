package freelancerwatermellon.serbianeidomnikey.cardreadermanager;

import android.util.SparseArray;

public final class Constants {
    public static final String BackendIPCAction = "com.theobroma.cardreadermanager.backendipc";
    public static final String BackendServiceAction = "com.theobroma.cardreadermanager.backendservice";
    public static final int DEVICE_BT = 2;
    public static final int DEVICE_USB = 1;
    public static final int ERROR_INVALID_CONTEXT_ID = -3;
    public static final int ERROR_INVALID_READER_ID = -1;
    public static final int ERROR_NO_CARD_PRESENT = -5;
    public static final int ERROR_READER_EXCLUSIVE_IN_USE = -6;
    public static final int ERROR_READER_NAME_NOT_FOUND = -4;
    public static final int ERROR_UNABLE_TO_POWEROFF = -2;
    public static final int ERROR_UNSUPPORTED_PROTOCOL = -7;
    public static final String EXTRA_DEVICE = "cardreadermanager.device";
    public static final String KEY_ENABLE_DEBUG = "ENABLE_DEBUG";
    public static final String PREFERENCES_NAME = "CardReaderManager.prefs";
    public static final int PROTOCOL_T0 = 1;
    public static final int PROTOCOL_T1 = 2;
    public static final String READER_ATTACH_ACTION = "com.hidglobal.ia.omnikey.intent.READER_ATTACHED";
    public static final int READER_CARD_ACTIVE = 1;
    public static final int READER_CARD_NOT_PRESENT = 3;
    public static final int READER_CARD_PRESENT = 2;
    public static final String READER_DETACH_ACTION = "com.hidglobal.ia.omnikey.intent.READER_DETACHED";
    public static final int SHARE_MODE_EXCLUSIVE = 1;
    public static final int SHARE_MODE_SHARE = 2;
    public static final String SMARTCARDIO_PERMISSION = "com.hidglobal.ia.omnikey.service.permission.SMARTCARDIO";
    public static final SparseArray<String> USB_PRODUCTS;
    public static final SparseArray<String> USB_VENDORS;

    static {
        USB_VENDORS = new SparseArray();
        USB_VENDORS.put(1899, "OMNIKEY");
        USB_PRODUCTS = new SparseArray();
        USB_PRODUCTS.put(40993, "Smart@Link");
        USB_PRODUCTS.put(40994, "Smart@Link");
        USB_PRODUCTS.put(4129, "CardMan 1021");
        USB_PRODUCTS.put(12321, "CardMan 3x21");
        USB_PRODUCTS.put(13857, "CardMan 3621");
        USB_PRODUCTS.put(14369, "CardMan 3821");
        USB_PRODUCTS.put(26146, "CardMan 6121");
        USB_PRODUCTS.put(40995, "Smart@Link");
        USB_PRODUCTS.put(40996, "Smart@Link");
        USB_PRODUCTS.put(12322, "3x21 Smart Card Reader");
        USB_PRODUCTS.put(12337, "3121 Smart Card Reader");
        USB_PRODUCTS.put(13859, "3621 Smart Card Reader");
        USB_PRODUCTS.put(14371, "3821 Smart Card Reader");
        USB_PRODUCTS.put(20514, "5022 Smart Card Reader");
        USB_PRODUCTS.put(21543, "5427 CK Smart Card Reader");
        USB_PRODUCTS.put(20775, "5127 CK Smart Card Reader");
        USB_PRODUCTS.put(26147, "6121 Smart Card Reader");
    }
}
