package freelancerwatermellon.serbianeidomnikey.cardreadermanager;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.smartcardio.ipc.IBackendIPC.Stub;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import freelancerwatermellon.serbianeidomnikey.cardreadermanager.bt.BtCommunicator;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid.DeviceCommunicator;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.usb.UsbCommunicator;

public class BackendService extends Service {
    private static final String ACTION_USB_PERMISSION = "com.theobroma.cardreadermanager.USB_PERMISSION";
    private static final String KEY_BLUETOOTH_MACS = "BLUETOOTH_MACS";
    private final Stub mBackendIPC;
    private final IBackendService.Stub mBackendService;
    private final Object mPermissionLock;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<Integer> mContextIds;
    private BroadcastReceiver mDetachReceiver;
    private ArrayList<DeviceCommunicator> mDevices;
    private PendingIntent mPermissionIntent;
    private BroadcastReceiver mPermissionReceiver;
    private UsbManager mUsbManager;

    public BackendService() {
        this.mUsbManager = null;
        this.mBluetoothAdapter = null;
        this.mContextIds = null;
        this.mPermissionIntent = null;
        this.mDevices = null;

        this.mBackendService = new C01551();
        this.mBackendIPC = new C01562();
        this.mDetachReceiver = new C01193();
        this.mPermissionLock = new Object();
        this.mPermissionReceiver = new C01204();
    }

    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(Constants.BackendServiceAction)) {
            return this.mBackendService;
        }
        if (intent.getAction().equals(Constants.BackendIPCAction)) {
            return this.mBackendIPC;
        }
        return null;
    }

    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        registerReceiver(this.mDetachReceiver, intentFilter);
        this.mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        registerReceiver(this.mPermissionReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        this.mUsbManager = (UsbManager) getSystemService(USB_SERVICE);

        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mDevices = new ArrayList(1);
        try {
            new PollUsbReadersTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            this.mBackendService.setBtDevicesFromPreferences();
        } catch (RemoteException e) {
        }
        Log.e("BackendService", "STARTED");
        Util.logDebug("backend service started up");
    }

    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mDetachReceiver);
        unregisterReceiver(this.mPermissionReceiver);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return 1;
    }

    private boolean addUsbDevice(UsbDevice device) {
        if (findDeviceByDeviceId(device.getDeviceId()) != null) {
            return false;
        }
        DeviceCommunicator communicator = new DeviceCommunicator(new UsbCommunicator(this.mUsbManager, device));
        if (communicator.initialize()) {
            return this.mDevices.add(communicator);
        }
        Util.logError("USB device " + device.getVendorId() + "/" + device.getProductId() + " failed to initialize");
        return false;
    }

    private boolean validContextId(long contextId) {
        if (this.mContextIds == null) {
            return false;
        }
        return this.mContextIds.contains(Integer.valueOf((int) contextId));
    }

    private DeviceCommunicator findDeviceByCardId(long cardId) {
        Iterator it = this.mDevices.iterator();
        while (it.hasNext()) {
            DeviceCommunicator c = (DeviceCommunicator) it.next();
            if (c.containsCardHandle((int) cardId)) {
                return c;
            }
        }
        return null;
    }

    private DeviceCommunicator findDeviceByDeviceId(int deviceId) {
        Iterator it = this.mDevices.iterator();
        while (it.hasNext()) {
            DeviceCommunicator c = (DeviceCommunicator) it.next();
            if (c.getDeviceId() == deviceId) {
                return c;
            }
        }
        return null;
    }

    private DeviceCommunicator findDeviceByName(String deviceName) {
        Iterator it = this.mDevices.iterator();
        while (it.hasNext()) {
            DeviceCommunicator c = (DeviceCommunicator) it.next();
            if (c.getDeviceName().equals(deviceName)) {
                return c;
            }
        }
        return null;
    }

    private Set<String> getBtMacsFromPreferences() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFERENCES_NAME, 4);
        String macString = BuildConfig.FLAVOR;
        try {
            macString = prefs.getString(KEY_BLUETOOTH_MACS, BuildConfig.FLAVOR);
        } catch (ClassCastException e) {
        }
        return Util.stringToSet(macString, ",");
    }

    private Device getDeviceFromCommunicator(DeviceCommunicator communicator) {
        boolean inUse = communicator.cardHandles() > 0;
        Device device = new Device(communicator.isActive(), communicator.getDeviceId(), communicator.getDeviceType());
        device.setDeviceName(communicator.getDeviceName());
        device.setDeviceInUse(inUse);
        device.setUsbCcidDescriptor(communicator.getCcidDescriptorString());
        if (communicator.getDeviceType() == 2) {
            device.setBtMacAddress(communicator.getMacAddressString());
        }
        return device;
    }

    private void showToast(String text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(17, 0, 0);
        toast.show();
    }

    /* renamed from: com.theobroma.cardreadermanager.BackendService.3 */
    class C01193 extends BroadcastReceiver {
        C01193() {
        }

        public void onReceive(Context context, Intent intent) {
            int deviceId = -1;
            if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(intent.getAction())) {
                Util.logDebug("ACTION_USB_DEVICE_DETACHED");
                deviceId = ((UsbDevice) intent.getParcelableExtra("device")).getDeviceId();
            }
            if ("android.bluetooth.device.action.ACL_DISCONNECTED".equals(intent.getAction())) {
                Util.logDebug("ACTION_ACL_DISCONNECTED");
                deviceId = ((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE")).getAddress().hashCode();
            }
            if (deviceId != -1) {
                DeviceCommunicator communicator = null;
                Iterator it = BackendService.this.mDevices.iterator();
                while (it.hasNext()) {
                    DeviceCommunicator c = (DeviceCommunicator) it.next();
                    if (c.getDeviceId() == deviceId) {
                        communicator = c;
                    }
                }
                if (communicator == null) {
                    Util.logDebug("received detach intent but cannot find device " + deviceId);
                    return;
                }
                communicator.shutdown();
                if (communicator.getDeviceType() == 1) {
                    BackendService.this.mDevices.remove(communicator);
                    Util.logInfo("device " + communicator.getDeviceName() + " with id " + deviceId + " detached");
                } else {
                    Util.logInfo("device " + deviceId + " out of reach, but " + "keeping in list as it's Bluetooth");
                }
                Intent bc = new Intent();
                bc.setFlags(32);
                bc.setAction(Constants.READER_DETACH_ACTION);
                BackendService.this.sendBroadcast(bc, Manifest.permission.SMARTCARDIO);
            }
        }
    }

    /* renamed from: com.theobroma.cardreadermanager.BackendService.4 */
    class C01204 extends BroadcastReceiver {
        C01204() {
        }

        public void onReceive(Context context, Intent intent) {
            if (BackendService.ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (BackendService.this.mPermissionLock) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra("device");
                    int deviceId = device.getDeviceId();
                    int vendorId = device.getVendorId();
                    int productId = device.getProductId();
                    if (!intent.getBooleanExtra("permission", false)) {
                        Util.logWarn("permission for USB device " + deviceId + ", " + vendorId + "/" + productId + " NOT granted");
                    } else if (device != null) {
                        if (BackendService.this.addUsbDevice(device)) {
                            Intent bc = new Intent();
                            bc.setFlags(32);
                            bc.setAction(Constants.READER_ATTACH_ACTION);
                            BackendService.this.sendBroadcast(bc, Manifest.permission.SMARTCARDIO);
                            Util.logInfo("device " + deviceId + ", " + vendorId + "/" + productId + " attached");
                        } else {
                            Util.logError("failed to add device " + deviceId + ", " + vendorId + "/" + productId);
                            BackendService.this.showToast("Failed to attach new USB device");
                        }
                    }
                    BackendService.this.mPermissionLock.notify();
                }
            }
        }
    }

//    private class PollUsbReadersTask extends AsyncTask<Void, Void, Boolean> {
//        private PollUsbReadersTask() {
//        }
//
//        /* JADX WARNING: inconsistent code. */
//        /* Code decompiled incorrectly, please refer to instructions dump. */
//        protected Boolean doInBackground(Void... r8) {
//            /*
//            r7 = this;
//        L_0x0000:
//            r3 = com.theobroma.cardreadermanager.BackendService.this;
//            r3 = r3.mUsbManager;
//            r2 = r3.getDeviceList();
//            r3 = r2.keySet();
//            r3 = r3.iterator();
//        L_0x0012:
//            r4 = r3.hasNext();
//            if (r4 == 0) goto L_0x006c;
//        L_0x0018:
//            r1 = r3.next();
//            r1 = (java.lang.String) r1;
//            r0 = r2.get(r1);
//            r0 = (android.hardware.usb.UsbDevice) r0;
//            r4 = com.theobroma.cardreadermanager.Constants.USB_VENDORS;
//            r5 = r0.getVendorId();
//            r4 = r4.get(r5);
//            if (r4 == 0) goto L_0x0012;
//        L_0x0030:
//            r4 = com.theobroma.cardreadermanager.Constants.USB_PRODUCTS;
//            r5 = r0.getProductId();
//            r4 = r4.get(r5);
//            if (r4 == 0) goto L_0x0012;
//        L_0x003c:
//            r4 = com.theobroma.cardreadermanager.BackendService.this;
//            r5 = r0.getDeviceId();
//            r4 = r4.findDeviceByDeviceId(r5);
//            if (r4 != 0) goto L_0x0012;
//        L_0x0048:
//            r4 = com.theobroma.cardreadermanager.BackendService.this;
//            r4 = r4.mPermissionLock;
//            monitor-enter(r4);
//            r5 = com.theobroma.cardreadermanager.BackendService.this;	 Catch:{ all -> 0x0069 }
//            r5 = r5.mUsbManager;	 Catch:{ all -> 0x0069 }
//            r6 = com.theobroma.cardreadermanager.BackendService.this;	 Catch:{ all -> 0x0069 }
//            r6 = r6.mPermissionIntent;	 Catch:{ all -> 0x0069 }
//            r5.requestPermission(r0, r6);	 Catch:{ all -> 0x0069 }
//        L_0x005e:
//            r5 = com.theobroma.cardreadermanager.BackendService.this;	 Catch:{ InterruptedException -> 0x0074 }
//            r5 = r5.mPermissionLock;	 Catch:{ InterruptedException -> 0x0074 }
//            r5.wait();	 Catch:{ InterruptedException -> 0x0074 }
//            monitor-exit(r4);	 Catch:{ all -> 0x0069 }
//            goto L_0x0012;
//        L_0x0069:
//            r3 = move-exception;
//            monitor-exit(r4);	 Catch:{ all -> 0x0069 }
//            throw r3;
//        L_0x006c:
//            r4 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
//            java.lang.Thread.sleep(r4);	 Catch:{ InterruptedException -> 0x0072 }
//            goto L_0x0000;
//        L_0x0072:
//            r3 = move-exception;
//            goto L_0x0000;
//        L_0x0074:
//            r5 = move-exception;
//            goto L_0x005e;
//            */
//            throw new UnsupportedOperationException("Method not decompiled: com.theobroma.cardreadermanager.BackendService.PollUsbReadersTask.doInBackground(java.lang.Void[]):java.lang.Boolean");
//        }
//
//        protected void onPostExecute(Boolean result) {
//        }
//    }

    private class PollUsbReadersTask
            extends AsyncTask<Void, Void, Boolean> {
        private PollUsbReadersTask(BackendService paramBackendService) {
        }

        protected Boolean doInBackground(Void... paramVarArgs) {

            while (true) { // Loop forever
                HashMap localHashMap = mUsbManager.getDeviceList(); // get all usb devices

                Iterator localIterator = localHashMap.keySet().iterator();
                UsbDevice localUsbDevice = null;

                // loop through usb devices
                while (localIterator.hasNext()) {
                    localUsbDevice = (UsbDevice) localHashMap.get((String) localIterator.next());
                    if ((Constants.USB_VENDORS.get(localUsbDevice.getVendorId()) != null) &&
                            (Constants.USB_PRODUCTS.get(localUsbDevice.getProductId()) != null) &&
                            (findDeviceByDeviceId(localUsbDevice.getDeviceId()) == null)) {
                        // local Usb device is recognised by vendor and product id, and is not in the list
                        synchronized (BackendService.this.mPermissionLock) {
                            // Ask for permission
                            Log.e("Backend Service", "Asking for permission");
                            mUsbManager.requestPermission(localUsbDevice, mPermissionIntent);
                            BackendService.this.mPermissionLock.notify();
                        }
                    }
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException localInterruptedException1) {
                }
            }
        }

        protected void onPostExecute(Boolean paramBoolean) {
        }
    }


    /* renamed from: com.theobroma.cardreadermanager.BackendService.1 */
    class C01551 extends IBackendService.Stub {
        C01551() {
        }

        public List<Device> getDevices() throws RemoteException {
            ArrayList<Device> devices = new ArrayList(BackendService.this.mDevices.size());
            Iterator it = BackendService.this.mDevices.iterator();
            while (it.hasNext()) {
                devices.add(BackendService.this.getDeviceFromCommunicator((DeviceCommunicator) it.next()));
            }
            return devices;
        }

        public Device getDevice(int deviceId) throws RemoteException {
            DeviceCommunicator communicator = BackendService.this.findDeviceByDeviceId(deviceId);
            if (communicator == null) {
                return null;
            }
            return BackendService.this.getDeviceFromCommunicator(communicator);
        }

        public boolean addBtDevice(String deviceAddress) throws RemoteException {
            deviceAddress = deviceAddress.toUpperCase();
            if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                if (BackendService.this.findDeviceByDeviceId(deviceAddress.hashCode()) != null) {
                    return false;
                }
                BluetoothDevice device = BackendService.this.mBluetoothAdapter.getRemoteDevice(deviceAddress);
                if (device == null) {
                    Util.logError("Bluetooth device " + deviceAddress + " not found");
                    return false;
                }
                DeviceCommunicator communicator = new DeviceCommunicator(new BtCommunicator(device));
                if (communicator.initialize()) {
                    BackendService.this.mDevices.add(communicator);
                    Editor editor = BackendService.this.getSharedPreferences(Constants.PREFERENCES_NAME, 4).edit();
                    Set<String> macSet = BackendService.this.getBtMacsFromPreferences();
                    if (!macSet.contains(deviceAddress)) {
                        macSet.add(deviceAddress);
                        Util.logDebug("add Bluetooth MAC " + deviceAddress + " to preferences storage");
                    }
                    return editor.putString(BackendService.KEY_BLUETOOTH_MACS, Util.setToString(macSet, ",")).commit();
                }
                Util.logError("Bluetooth device " + deviceAddress + " failed to initialize");
                return false;
            }
            Util.logError("invalid Bluetooth address " + deviceAddress);
            return false;
        }

        public boolean setBtDevicesFromPreferences() throws RemoteException {
            if (BackendService.this.mBluetoothAdapter.isEnabled()) {
                Set<String> macSet = BackendService.this.getBtMacsFromPreferences();
                if (macSet == null) {
                    return true;
                }
                Set<BluetoothDevice> pairedDevices = BackendService.this.mBluetoothAdapter.getBondedDevices();
                if (pairedDevices == null) {
                    return false;
                }
                for (BluetoothDevice device : pairedDevices) {
                    for (String mac : macSet) {
                        if (device.getAddress().equals(mac)) {
                            addBtDevice(mac);
                            macSet.remove(mac);
                            Util.logDebug("retrieve Bluetooth MAC " + mac + " from preferences storage");
                            break;
                        }
                    }
                }
                return true;
            }
            Util.logDebug("Bluetooth not enabled");
            return true;
        }

        public boolean removeBtDevice(int deviceId, String deviceAddress) throws RemoteException {
            DeviceCommunicator communicator = BackendService.this.findDeviceByDeviceId(deviceId);
            if (communicator == null) {
                return false;
            }
            Editor editor = BackendService.this.getSharedPreferences(Constants.PREFERENCES_NAME, 4).edit();
            Set<String> macSet = BackendService.this.getBtMacsFromPreferences();
            if (macSet == null) {
                return false;
            }
            if (macSet.contains(deviceAddress)) {
                macSet.remove(deviceAddress);
                editor.putString(BackendService.KEY_BLUETOOTH_MACS, Util.setToString(macSet, ","));
                if (!editor.commit()) {
                    return false;
                }
                communicator.shutdown();
                BackendService.this.mDevices.remove(communicator);
                return true;
            }
            Util.logError("trying to remove non-existing device, address " + deviceAddress);
            return false;
        }

        public boolean activateDevice(int deviceId, boolean active) throws RemoteException {
            DeviceCommunicator communicator = BackendService.this.findDeviceByDeviceId(deviceId);
            if (communicator == null) {
                return false;
            }
            return communicator.setActive(active);
        }
    }

    /* renamed from: com.theobroma.cardreadermanager.BackendService.2 */
    class C01562 extends Stub {
        C01562() {
        }

        public long SCardEstablishContext() throws RemoteException {
            int contextId = Integer.valueOf(UUID.randomUUID().hashCode()).intValue();
            if (BackendService.this.mContextIds == null) {
                BackendService.this.mContextIds = new ArrayList(1);
            }
            BackendService.this.mContextIds.add(Integer.valueOf(contextId));
            Util.logDebug("new context " + contextId);
            return (long) contextId;
        }

        public String[] SCardListReaders(long contextId) throws RemoteException {
            if (BackendService.this.validContextId(contextId)) {
                int activeReaders = 0;
                Iterator it = BackendService.this.mDevices.iterator();
                while (it.hasNext()) {
                    if (((DeviceCommunicator) it.next()).isActive()) {
                        activeReaders++;
                    }
                }
                String[] readerNames = new String[activeReaders];
                int i = 0;
                it = BackendService.this.mDevices.iterator();
                while (it.hasNext()) {
                    DeviceCommunicator c = (DeviceCommunicator) it.next();
                    if (c.isActive()) {
                        int i2 = i + 1;
                        readerNames[i] = c.getDeviceName();
                        i = i2;
                    }
                }
                return readerNames;
            }
            Util.logError("no valid context id " + contextId);
            return null;
        }

        public long SCardConnect(long contextId, String readerName, int shareMode) throws RemoteException {
            Log.e("Bakend Service", "SCardConnect...");
            if (BackendService.this.validContextId(contextId)) {
                DeviceCommunicator communicator = BackendService.this.findDeviceByName(readerName);
                if (communicator == null) {
                    Util.logError("reader " + readerName + " not found");
                    return -4;
                } else if (!communicator.isActive()) {
                    Util.logError("reader " + readerName + " deactivated");
                    return -4;
                } else if (communicator.isCardPresent()) {
                    int cardId = Integer.valueOf(UUID.randomUUID().hashCode()).intValue();
                    Log.e("Bakend Service", "SCardConnect...Card present");
                    if (communicator.addCardHandle(cardId)) {
                        Log.e("Bakend Service", "SCardConnect added: " + String.valueOf(cardId));
                        Util.logDebug("returning " + cardId);
                        return (long) cardId;
                    }
                    Util.logError("cannot add card handle to communicator");
                    return -6;
                } else {
                    Log.e("Bakend Service", "SCardConnect...Card Not present");
                    Util.logError("no card present");
                    return -5;
                }
            }
            Util.logError("no valid context id " + contextId);
            return -3;
        }

        public byte[] SCardPowerOn(long cardId) throws RemoteException {
            Log.e("Bakend Service", "SCardPowerOn...");
            DeviceCommunicator communicator = BackendService.this.findDeviceByCardId(cardId);
            if (communicator != null) {
                Log.e("Bakend Service", "SCardPowerOn...calling slotIccOn()");
                return communicator.slotIccOn();
            }
            Log.e("Bakend Service", "SCardPowerOn...reader not found");
            Util.logError("reader id " + cardId + " not found");
            return null;
        }

        public int SCardSetProtocol(long cardId, int preferredProtocols) throws RemoteException {
            Log.e("Bakend Service", "SCardSetProtocol...");
            DeviceCommunicator communicator = BackendService.this.findDeviceByCardId(cardId);
            if (communicator == null) {
                Util.logError("reader id " + cardId + " not found");
                return -1;
            }
            Util.logError("Preferred protocols " + preferredProtocols);
            Log.e("Bakend Service", "SCardSetProtocol...calling communicator.setProtocols");
            return communicator.setProtocol(preferredProtocols);
        }

        public int SCardDisconnect(long cardId) throws RemoteException {
            DeviceCommunicator communicator = BackendService.this.findDeviceByCardId(cardId);
            if (communicator == null) {
                Util.logError("reader id " + cardId + " not found");
                return -1;
            }
            communicator.removeCardHandle((int) cardId);
            Util.logDebug("removing card handle " + cardId);
            return communicator.slotIccOff() ? 0 : -2;
        }

        public byte[] SCardControl(long cardId, int controlCode, byte[] command) throws RemoteException {
            DeviceCommunicator communicator = BackendService.this.findDeviceByCardId(cardId);
            if (communicator == null) {
                Util.logError("reader id " + cardId + " not found");
                return null;
            }
            byte[] c = new byte[(command.length + 1)];
            c[0] = (byte) controlCode;
            System.arraycopy(command, 0, c, 1, command.length);
            Util.logDebug("transmitting control message " + controlCode + " to card " + cardId);
            return communicator.transmitControl(c);
        }

        public byte[] SCardTransmit(long cardId, int protocol, byte[] command, int offset, int len) throws RemoteException {
            DeviceCommunicator communicator = BackendService.this.findDeviceByCardId(cardId);
            if (communicator == null) {
                Util.logError("reader id " + cardId + " not found");
                return null;
            }
            byte[] c = new byte[len];
            System.arraycopy(command, offset, c, 0, len);
            Util.logDebug("transmitting command to card " + cardId);
            return communicator.transmitData(c);
        }

        public int[] SCardWaitForChange(long contextId, int timeout, String[] readerNames, int[] currentStates, int[] expectedStates) throws RemoteException {
            if (BackendService.this.validContextId(contextId)) {
                int[] newStates = new int[currentStates.length];
                System.arraycopy(currentStates, 0, newStates, 0, currentStates.length);
                long end = 0;
                while (true) {
                    long current = System.currentTimeMillis();
                    if (end == 0) {
                        end = current + ((long) timeout);
                    }
                    int i = 0;
                    while (i < readerNames.length) {
                        DeviceCommunicator communicator = BackendService.this.findDeviceByName(readerNames[i]);
                        if (communicator == null) {
                            Util.logDebug("reader " + readerNames[i] + " not found");
                            return null;
                        }
                        int currentState = currentStates[i];
                        int expectedState = expectedStates == null ? -1 : expectedStates[i];
                        newStates[i] = communicator.getStatus(currentState, expectedState);
                        if (expectedState == -1 && newStates[i] != currentState) {
                            return newStates;
                        }
                        if (currentState == -1 && newStates[i] == expectedState) {
                            return newStates;
                        }
                        i++;
                    }
                    if (timeout != 0 && current >= end) {
                        return currentStates;
                    }
                }
            } else {
                Util.logError("no valid context id " + contextId);
                return null;
            }
        }

        public boolean SCardBeginTransaction(long cardId) throws RemoteException {
            DeviceCommunicator communicator = BackendService.this.findDeviceByCardId(cardId);
            if (communicator == null) {
                Util.logError("reader id " + cardId + " not found");
                return false;
            }
            Util.logDebug("starting exclusive access for " + cardId);
            return communicator.startExclusiveAccess((int) cardId);
        }

        public boolean SCardEndTransaction(long cardId) throws RemoteException {
            DeviceCommunicator communicator = BackendService.this.findDeviceByCardId(cardId);
            if (communicator == null) {
                Util.logError("reader id " + cardId + " not found");
                return false;
            }
            Util.logDebug("ending exclusive access for " + cardId);
            return communicator.stopExclusiveAccess((int) cardId);
        }
    }
}
