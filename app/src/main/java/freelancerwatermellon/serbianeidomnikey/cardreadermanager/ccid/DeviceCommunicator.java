package freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import freelancerwatermellon.serbianeidomnikey.cardreadermanager.BuildConfig;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.ICommunicator;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util;

public class DeviceCommunicator {
    public static final byte BLOCK_WAITING_TIME_EXTENDER = (byte) 1;
    public static final int CCID_BULK_MESSAGE_HDR_LEN = 10;
    public static final int DEF_CLOCK_STOP = 0;
    public static final int DEF_FINDEX_DINDEX = 0;
    public static final int EXT_APDU_CHAIN_BEGIN = 1;
    public static final int EXT_APDU_CHAIN_BEGIN_AND_END = 0;
    public static final int EXT_APDU_CHAIN_EMPTY = 16;
    public static final int EXT_APDU_CHAIN_END = 2;
    public static final int EXT_APDU_CHAIN_MIDDLE = 3;
    public static final int EXT_APDU_LEVEL_BEGIN = 1;
    public static final int EXT_APDU_LEVEL_BEGIN_AND_END = 0;
    public static final int EXT_APDU_LEVEL_EMPTY = 16;
    public static final int EXT_APDU_LEVEL_END = 2;
    public static final int EXT_APDU_LEVEL_MIDDLE = 3;
    public static final int MESSAGE_TYPE_ESCAPE_T0_TPDU_TRANSFER = 27;
    public static final int MESSAGE_TYPE_ESCAPE_T1_TPDU_TRANSFER = 26;
    public static final int MESSAGE_TYPE_PC_ABORT = 114;
    public static final int MESSAGE_TYPE_PC_DATA_RATE_AND_CLOCK_FREQ = 115;
    public static final int MESSAGE_TYPE_PC_ESCAPE = 107;
    public static final int MESSAGE_TYPE_PC_GET_PARAMETERS = 108;
    public static final int MESSAGE_TYPE_PC_GET_SLOT_STATUS = 101;
    public static final int MESSAGE_TYPE_PC_ICC_CLOCK = 110;
    public static final int MESSAGE_TYPE_PC_ICC_POWER_OFF = 99;
    public static final int MESSAGE_TYPE_PC_ICC_POWER_ON = 98;
    public static final int MESSAGE_TYPE_PC_MECHANICAL = 113;
    public static final int MESSAGE_TYPE_PC_RESET_PARAMETERS = 109;
    public static final int MESSAGE_TYPE_PC_SECURE = 105;
    public static final int MESSAGE_TYPE_PC_SET_PARAMETERS = 97;
    public static final int MESSAGE_TYPE_PC_T0_APDU = 106;
    public static final int MESSAGE_TYPE_PC_XFR_BLOCK = 111;
    public static final int MESSAGE_TYPE_RDR_DATA_BLOCK = 128;
    public static final int MESSAGE_TYPE_RDR_DATA_RATE_AND_CLOCK_FREQ = 132;
    public static final int MESSAGE_TYPE_RDR_ESCAPE = 131;
    public static final int MESSAGE_TYPE_RDR_HARDWARE_ERROR = 81;
    public static final int MESSAGE_TYPE_RDR_NOTIFY_SLOT_CHANGE = 80;
    public static final int MESSAGE_TYPE_RDR_PARAMETERS = 130;
    public static final int MESSAGE_TYPE_RDR_SLOT_STATUS = 129;
    public static final int PROTOCOL_DATA_LEN_T0 = 5;
    public static final int PROTOCOL_DATA_LEN_T1 = 7;
    public static final boolean PROTOCOL_T0 = true;
    public static final boolean PROTOCOL_T1 = false;
    public static final int RDR_TO_PC_MAXLEN = 65548;
    public static final int RDR_TO_PC_SLOT_STATUS_LEN = 10;
    public static final int SLOT_ERROR_FAILED = -1;
    public static final int SLOT_ERROR_TIME_EXT = -2;
    public static final int SLOT_ERROR_UNKNOWN = -3;
    public static final int SLOT_OK = 0;
    private final ReentrantLock mXferLock;
    private AtrReader mATR;
    private boolean mActivated;
    private ArrayList<Integer> mCardHandles;
    private CcidDescriptor mCcidDescriptor;
    private ICommunicator mCommunicator;
    private int mCurrentProtocol;
    private int mExclusiveUser;
    private boolean mIFSSet;
    private int mLastBulkOut;
    private byte[] mRdrToPcIn;
    private boolean mResyncPerformed;
    private byte mSequenceNumber;
    private int mShareMode;
    private int mSlotStatus;
    private T1Handler mT1Recv;
    private T1Handler mT1Send;
    private boolean mTpduTransmission;

    public DeviceCommunicator(ICommunicator communicator) {
        this.mXferLock = new ReentrantLock(PROTOCOL_T0);
        this.mCommunicator = null;
        this.mCcidDescriptor = null;
        this.mActivated = PROTOCOL_T0;
        this.mShareMode = EXT_APDU_LEVEL_BEGIN_AND_END;
        this.mExclusiveUser = EXT_APDU_LEVEL_BEGIN_AND_END;
        this.mCardHandles = null;
        this.mCurrentProtocol = EXT_APDU_LEVEL_BEGIN;
        this.mTpduTransmission = PROTOCOL_T1;
        this.mLastBulkOut = EXT_APDU_LEVEL_BEGIN_AND_END;
        this.mSlotStatus = EXT_APDU_LEVEL_MIDDLE;
        this.mSequenceNumber = (byte) 0;
        this.mResyncPerformed = PROTOCOL_T1;
        this.mIFSSet = PROTOCOL_T1;
        this.mATR = null;
        this.mT1Send = null;
        this.mT1Recv = null;
        this.mRdrToPcIn = null;
        this.mCommunicator = communicator;
        this.mShareMode = EXT_APDU_LEVEL_END;
        this.mCardHandles = new ArrayList(EXT_APDU_LEVEL_BEGIN);
        this.mRdrToPcIn = new byte[RDR_TO_PC_MAXLEN];
    }

    public boolean initialize() {
        if (!this.mCommunicator.initialize()) {
            return PROTOCOL_T1;
        }
        this.mCcidDescriptor = this.mCommunicator.getCcidDescriptor();
        this.mSlotStatus = EXT_APDU_LEVEL_MIDDLE;
        this.mCardHandles.clear();
        this.mResyncPerformed = PROTOCOL_T1;
        this.mIFSSet = PROTOCOL_T1;
        if (this.mCommunicator.getProductId() == 12337) {
        }
        return PROTOCOL_T0;
    }

    public void shutdown() {
        this.mCommunicator.shutdown();
    }

    public int getDeviceId() {
        return this.mCommunicator.getId();
    }

    public int getProductId() {
        return this.mCommunicator.getProductId();
    }

    public String getDeviceName() {
        return this.mCommunicator.getName();
    }

    public int getDeviceType() {
        return this.mCommunicator.getType();
    }

    public String getCcidDescriptorString() {
        return this.mCcidDescriptor.toString();
    }

    public String getMacAddressString() {
        return this.mCommunicator.getMacAddress();
    }

    public boolean setActive(boolean active) {
        if (this.mActivated && this.mCardHandles.size() > 0 && !active) {
            return PROTOCOL_T1;
        }
        this.mActivated = active;
        return PROTOCOL_T0;
    }

    public boolean isActive() {
        return this.mActivated;
    }

    public boolean addCardHandle(int cardHandle) {
        if (this.mShareMode == EXT_APDU_LEVEL_BEGIN) {
            return PROTOCOL_T1;
        }
        this.mCardHandles.add(Integer.valueOf(cardHandle));
        return PROTOCOL_T0;
    }

    public boolean removeCardHandle(int cardHandle) {
        if (!this.mCardHandles.contains(Integer.valueOf(cardHandle))) {
            return PROTOCOL_T1;
        }
        this.mCardHandles.remove(this.mCardHandles.indexOf(Integer.valueOf(cardHandle)));
        return PROTOCOL_T0;
    }

    public int cardHandles() {
        return this.mCardHandles.size();
    }

    public boolean containsCardHandle(int cardHandle) {
        return this.mCardHandles.contains(Integer.valueOf(cardHandle));
    }

    public boolean startExclusiveAccess(int cardHandle) {
        if (this.mShareMode == EXT_APDU_LEVEL_BEGIN || this.mExclusiveUser != 0 || !this.mCardHandles.contains(Integer.valueOf(cardHandle))) {
            return PROTOCOL_T1;
        }
        this.mShareMode = EXT_APDU_LEVEL_BEGIN;
        this.mExclusiveUser = cardHandle;
        return PROTOCOL_T0;
    }

    public boolean stopExclusiveAccess(int cardHandle) {
        if (this.mShareMode != EXT_APDU_LEVEL_BEGIN || this.mExclusiveUser != cardHandle || !this.mCardHandles.contains(Integer.valueOf(cardHandle))) {
            return PROTOCOL_T1;
        }
        this.mShareMode = EXT_APDU_LEVEL_END;
        this.mExclusiveUser = EXT_APDU_LEVEL_BEGIN_AND_END;
        return PROTOCOL_T0;
    }

    public boolean isCardPresent() {
        getStatus(this.mSlotStatus, SLOT_ERROR_FAILED);
        return getStatus(this.mSlotStatus, SLOT_ERROR_FAILED) != EXT_APDU_LEVEL_MIDDLE ? PROTOCOL_T0 : PROTOCOL_T1;
    }

    public int getStatus(int currentStatus, int expectedStatus) {
        this.mXferLock.lock();
        try {
            PC_to_RDR_GetSlotStatus();
            RDR_to_PC_SlotStatus();
            if (expectedStatus == SLOT_ERROR_FAILED) {
                if (this.mSlotStatus != currentStatus) {
                    currentStatus = this.mSlotStatus;
                    return currentStatus;
                }
            } else if (this.mSlotStatus == expectedStatus) {
                currentStatus = this.mSlotStatus;
                //this.mXferLock.unlock();
                return currentStatus;
            }
            //this.mXferLock.unlock();
            return currentStatus;
        } finally {
            //if (this.mXferLock.isLocked())
            this.mXferLock.unlock();
        }
    }

    public byte[] slotIccOn() {
        this.mXferLock.lock();
        try {
            if (!PC_to_RDR_IccPowerOn()) {
                return null;
            }
            byte[] atr = RDR_to_PC_DataBlock();
            if (atr == null) {
                //this.mXferLock.unlock();
                return null;
            }
            Util.logInfo("Creating atr reader...");
            this.mATR = new AtrReader(atr);
            if (this.mATR.parse()) {
                Util.logInfo("Parsing ATR success...");
                this.mResyncPerformed = PROTOCOL_T1;
                this.mIFSSet = PROTOCOL_T1;
                this.mTpduTransmission = PROTOCOL_T1;
                this.mCurrentProtocol = EXT_APDU_LEVEL_BEGIN;
                //this.mXferLock.unlock();
                Util.logInfo("Returning ATR...");
                return atr;
            }
            //this.mXferLock.unlock();
            return null;
        } finally {
            this.mXferLock.unlock();
        }
    }

    public int setProtocol(int preferredProtocols) {
        int protocol = setReaderParameters(preferredProtocols);
        if (protocol == SLOT_ERROR_FAILED) {
            return -7;
        }
        this.mCurrentProtocol = protocol;
        return this.mCurrentProtocol;
    }

    public boolean slotIccOff() {
        if (!PC_to_RDR_IccPowerOff()) {
            return PROTOCOL_T1;
        }
        this.mResyncPerformed = PROTOCOL_T1;
        this.mIFSSet = PROTOCOL_T1;
        this.mTpduTransmission = PROTOCOL_T1;
        return RDR_to_PC_SlotStatus();
    }

    public byte[] transmitControl(byte[] control) {
        this.mXferLock.lock();
        try {
            if (!bulkOut(control, control[EXT_APDU_LEVEL_BEGIN_AND_END])) {
                return null;
            }
            if (bulkIn(this.mRdrToPcIn)) {
                //this.mXferLock.unlock();
                return this.mRdrToPcIn;
            }
            //this.mXferLock.unlock();
            return null;
        } finally {
            //if (this.mXferLock.isLocked())
            this.mXferLock.unlock();
        }
    }

    public byte[] transmitData(byte[] command) {
        byte[] bArr = null;
        PduAnalyzer pduAnalyzer = new PduAnalyzer();
        if (!pduAnalyzer.putCommand(command)) {
            Util.logError("PDU Analyzer returned false");
        } else if (this.mCcidDescriptor.featExchange() == CcidDescriptor.FEAT_EXCHANGE_EXTENDED) {
            Util.logDebug("Extended APDU transmission");
            this.mXferLock.lock();
            try {
                if (PC_to_RDR_XfrBlockExtendedLength(command)) {
                    bArr = RDR_to_PC_DataBlock();
                    //this.mXferLock.unlock();
                }
            } finally {
                //if (this.mXferLock.isLocked())
                this.mXferLock.unlock();
            }
        } else if (this.mCcidDescriptor.featExchange() == CcidDescriptor.FEAT_EXCHANGE_TPDU || pduAnalyzer.extendedModeNecessary(this.mCcidDescriptor.maxCCIDMessageLength()) || this.mTpduTransmission) {
            Util.logDebug("TPDU transmission");
            this.mTpduTransmission = PROTOCOL_T0;
            switch (this.mCurrentProtocol) {
                case EXT_APDU_LEVEL_BEGIN /*1*/:
                    this.mXferLock.lock();
                    try {
                        bArr = transmitTpduT0(pduAnalyzer, PROTOCOL_T1);
                        break;
                    } finally {
                        //if (this.mXferLock.isLocked())
                        this.mXferLock.unlock();
                    }
                case EXT_APDU_LEVEL_END /*2*/:
                    this.mXferLock.lock();
                    try {
                        bArr = transmitTpduT1(pduAnalyzer, PROTOCOL_T0);
                        break;
                    } finally {
                        //if (this.mXferLock.isLocked())
                        this.mXferLock.unlock();
                    }
                default:
                    Util.logError("unexpected value for mCurrentProtocol: " + this.mCurrentProtocol);
                    this.mTpduTransmission = PROTOCOL_T1;
                    break;
            }
        } else if (this.mCcidDescriptor.featExchange() == CcidDescriptor.FEAT_EXCHANGE_SHORT) {
            Util.logDebug("Short APDU transmission");
            this.mXferLock.lock();
            try {
                if (PC_to_RDR_XfrBlock(command, EXT_APDU_LEVEL_BEGIN_AND_END)) {
                    bArr = RDR_to_PC_DataBlock();
                    //this.mXferLock.unlock();
                }
            } finally {
                //if (this.mXferLock.isLocked())
                this.mXferLock.unlock();
            }
        } else if (this.mCcidDescriptor.featExchange() == 0) {
            Util.logError("CCID character level currently not supported");
        } else {
            Util.logError("Unknown exchange level: " + this.mCcidDescriptor.featExchange());
        }
        return bArr;
    }

    private byte getNextSequenceNumber() {
        byte retSequenceNumber = this.mSequenceNumber;
        int newSequenceNumber = this.mSequenceNumber + EXT_APDU_LEVEL_BEGIN;
        if (newSequenceNumber > 0xff) {
            this.mSequenceNumber = (byte) 0;
        } else {
            this.mSequenceNumber = (byte) newSequenceNumber;
        }
        return retSequenceNumber;
    }

    private boolean bulkOut(byte[] buffer, int messageType) {
        if (this.mCommunicator.bulkOut(buffer)) {
            return PROTOCOL_T0;
        }
        return PROTOCOL_T1;
    }

    private boolean bulkIn(byte[] buffer) {
        boolean timeExtensionRequested = PROTOCOL_T1;
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        int i = EXT_APDU_LEVEL_BEGIN_AND_END;
        while (true) {
            if (i >= EXT_APDU_LEVEL_MIDDLE && !timeExtensionRequested) {
                return PROTOCOL_T1;
            }
            timeExtensionRequested = PROTOCOL_T1;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            if (this.mCommunicator.bulkIn(byteBuffer, buffer.length)) {
                buffer = byteBuffer.array();
                int bStatus = buffer[PROTOCOL_DATA_LEN_T1] & 0xff;
                int bError = buffer[8] & 0xff;
                slotStatus(bStatus);
                switch (slotStatus(bStatus, bError)) {
                    case SLOT_ERROR_UNKNOWN /*-3*/:
                    case SLOT_ERROR_FAILED /*-1*/:
                        return PROTOCOL_T1;
                    case SLOT_ERROR_TIME_EXT /*-2*/:
                        timeExtensionRequested = PROTOCOL_T0;
                        break;
                    case EXT_APDU_LEVEL_BEGIN_AND_END /*0*/:
                        return PROTOCOL_T0;
                    default:
                        break;
                }
            } else if (this.mCommunicator.getType() != EXT_APDU_LEVEL_BEGIN) {
                return PROTOCOL_T1;
            }
            i += EXT_APDU_LEVEL_BEGIN;
        }
    }

    private boolean slotStatus(int bStatus) {
        switch (bStatus & EXT_APDU_LEVEL_END) {
            case EXT_APDU_LEVEL_BEGIN_AND_END /*0*/:
            case EXT_APDU_LEVEL_BEGIN /*1*/:
                this.mSlotStatus = EXT_APDU_LEVEL_END;
                Util.logDebug("slot status changed to ICC present (" + (bStatus & EXT_APDU_LEVEL_END) + ")");
                break;
            case EXT_APDU_LEVEL_END /*2*/:
                this.mSlotStatus = EXT_APDU_LEVEL_MIDDLE;
                Util.logDebug("slot status changed to ICC absent");
                this.mResyncPerformed = PROTOCOL_T1;
                this.mIFSSet = PROTOCOL_T1;
                break;
            default:
                Util.logError("unexpected value for bmICCStatus: " + (bStatus & EXT_APDU_LEVEL_END));
                break;
        }
        return PROTOCOL_T0;
    }

    private int slotStatus(int bStatus, int bError) {
        switch ((bStatus & T1Handler.S_BLOCK_RESYNC_REQUEST) >> 6) {
            case EXT_APDU_LEVEL_BEGIN_AND_END /*0*/:
                return EXT_APDU_LEVEL_BEGIN_AND_END;
            case EXT_APDU_LEVEL_BEGIN /*1*/:
                Util.logError("message returned status code " + Util.formatByte(bStatus) + ", error code " + Util.formatByte(bError) + " (slot status " + this.mSlotStatus + ")");
                return SLOT_ERROR_FAILED;
            case EXT_APDU_LEVEL_END /*2*/:
                Util.logDebug("time extension required, multiplier " + bError);
                return SLOT_ERROR_TIME_EXT;
            default:
                Util.logError("unexpected value for bmCommandStatus: " + ((bStatus & T1Handler.S_BLOCK_RESYNC_REQUEST) >> 6));
                return SLOT_ERROR_UNKNOWN;
        }
    }

    private int setReaderParameters(int preferredProtocols) {
        byte b = (byte) 0;
        this.mXferLock.lock();
        try {
            if (!PC_to_RDR_GetParameters()) {
                return SLOT_ERROR_FAILED;
            }
            byte[] currentParameters = RDR_to_PC_Parameters();
            if (currentParameters == null) {
                Util.logError("no parameters from reader!");
            } else {
                Util.logDebug("parameters from reader: " + Util.byteArrayToString(currentParameters));
            }
            if (preferredProtocols != EXT_APDU_LEVEL_BEGIN) {
                if ((preferredProtocols & EXT_APDU_LEVEL_END) != 0) {
                    if (this.mATR.supportsProtocol(EXT_APDU_LEVEL_END) && this.mCcidDescriptor.supportsProtocol(EXT_APDU_LEVEL_END)) {
                        int i;
                        Util.logDebug("Activating protocol T=1");
                        byte[] t1Parameters = new byte[PROTOCOL_DATA_LEN_T1];
                        t1Parameters[EXT_APDU_LEVEL_BEGIN_AND_END] = currentParameters != null ? currentParameters[EXT_APDU_LEVEL_BEGIN] : (byte) 0;
                        if (this.mATR.mConvention == EXT_APDU_LEVEL_BEGIN) {
                            i = EXT_APDU_LEVEL_BEGIN_AND_END;
                        } else {
                            i = EXT_APDU_LEVEL_BEGIN;
                        }
                        int i2 = (i << EXT_APDU_LEVEL_BEGIN) | EXT_APDU_LEVEL_EMPTY;
                        if (this.mATR.mT1_CRC) {
                            i = EXT_APDU_LEVEL_BEGIN;
                        } else {
                            i = EXT_APDU_LEVEL_BEGIN_AND_END;
                        }
                        t1Parameters[EXT_APDU_LEVEL_BEGIN] = (byte) (i | i2);
                        t1Parameters[EXT_APDU_LEVEL_END] = (byte) 0;
                        t1Parameters[EXT_APDU_LEVEL_MIDDLE] = (byte) ((this.mATR.mT1_BWI << 4) | this.mATR.mT1_CWI);
                        if (currentParameters != null) {
                            b = currentParameters[PROTOCOL_DATA_LEN_T0];
                        }
                        t1Parameters[4] = b;
                        t1Parameters[PROTOCOL_DATA_LEN_T0] = (byte) this.mATR.mT1_IFSC;
                        t1Parameters[6] = (byte) 0;
                        Util.logDebug("T=1 parameters to set: " + Util.byteArrayToString(t1Parameters));
                        if (PC_to_RDR_SetParameters(BLOCK_WAITING_TIME_EXTENDER, t1Parameters)) {
                            byte[] newParameters = RDR_to_PC_Parameters();
                            if (newParameters == null) {
                                Util.logError("no parameters from reader!");
                                //this.mXferLock.unlock();
                                return SLOT_ERROR_FAILED;
                            }
                            Util.logError("new parameters from reader: " + Util.byteArrayToString(newParameters));
                            if (newParameters[EXT_APDU_LEVEL_BEGIN_AND_END] == BLOCK_WAITING_TIME_EXTENDER) {
                                Util.logDebug("switching to protocol T=1");
                                //this.mXferLock.unlock();
                                return EXT_APDU_LEVEL_END;
                            } else if (newParameters.length == 8) {
                                Util.logWarn("Protocol tagged as T=0 but data structure length specific to T=1; attempting T=1");
                                //this.mXferLock.unlock();
                                return EXT_APDU_LEVEL_END;
                            }
                        }
                        //this.mXferLock.unlock();
                        return SLOT_ERROR_FAILED;
                    } else if ((preferredProtocols & EXT_APDU_LEVEL_BEGIN) == 0) {
                        Util.logError("No matching protocol found");
                        //this.mXferLock.unlock();
                        return SLOT_ERROR_FAILED;
                    } else {
                        Util.logDebug("Keeping protocol T=0");
                        //this.mXferLock.unlock();
                        return EXT_APDU_LEVEL_BEGIN;
                    }
                }
                //this.mXferLock.unlock();
                return SLOT_ERROR_FAILED;
            } else if (this.mATR.supportsProtocol(EXT_APDU_LEVEL_BEGIN) && this.mCcidDescriptor.supportsProtocol(EXT_APDU_LEVEL_BEGIN)) {
                Util.logDebug("Keeping protocol T=0");
                //this.mXferLock.unlock();
                return EXT_APDU_LEVEL_BEGIN;
            } else {
                Util.logError("No matching protocol found");
                //this.mXferLock.unlock();
                return SLOT_ERROR_FAILED;
            }
        } finally {
            //if (this.mXferLock.isLocked())
            this.mXferLock.unlock();
        }
    }

    private byte[] transmitTpduT0(PduAnalyzer pduAnalyzer, boolean escapeTransmit) {
        T0Handler t0Handler = new T0Handler(pduAnalyzer);
        byte[] commandTPDU = t0Handler.getCommand(escapeTransmit);
        while (commandTPDU != null && PC_to_RDR(commandTPDU, escapeTransmit, PROTOCOL_T0) && t0Handler.putResponse(RDR_to_PC(escapeTransmit, PROTOCOL_T0))) {
            byte[] response = t0Handler.getResponse();
            if (response != null) {
                return response;
            }
            commandTPDU = t0Handler.getNextCommand();
        }
        Util.logError("reached end without returning response");
        return null;
    }

    private byte[] transmitTpduT1(PduAnalyzer pduAnalyzer, boolean escapeTransmit) {
        byte[] block;
        if (this.mT1Send == null) {
            this.mT1Send = new T1Handler(this.mATR.mT1_IFSC, this.mATR.mT1_CRC);
        }
        if (this.mT1Recv == null) {
            this.mT1Recv = new T1Handler(this.mATR.mT1_CRC);
        }
        if (!this.mResyncPerformed) {
            Util.logDebug("performing resync");
            if (resyncTpduT1()) {
                this.mResyncPerformed = PROTOCOL_T0;
            }
        }
        this.mT1Send.putRaw(pduAnalyzer);
        if (this.mIFSSet) {
            block = this.mT1Send.getNextIBlock(PROTOCOL_T1);
        } else {
            block = this.mT1Send.getSBlock(T1Handler.S_BLOCK_IFSC_REQUEST, this.mCcidDescriptor.maxIFSD());
            Util.logDebug("request IFS to " + this.mCcidDescriptor.maxIFSD());
        }
        int retransmitIBlock = EXT_APDU_LEVEL_BEGIN_AND_END;
        boolean resyncRetransmit = PROTOCOL_T1;
        boolean abort = PROTOCOL_T1;
        while (block != null) {
            Util.logDebug("sending " + block.length + " bytes");
            if (!PC_to_RDR(block, escapeTransmit, PROTOCOL_T1)) {
                return null;
            }
            if (abort) {
                return this.mT1Recv.getPDU();
            }
            block = RDR_to_PC(escapeTransmit, PROTOCOL_T1);
            if (block == null) {
                return null;
            }
            switch (this.mT1Recv.putBlock(block)) {
                case EXT_APDU_LEVEL_BEGIN /*1*/:
                    if (!this.mT1Recv.blocksDone()) {
                        block = this.mT1Recv.getRBlock();
                        break;
                    }
                    Util.logDebug("all blocks received from card");
                    return this.mT1Recv.getPDU();
                case EXT_APDU_LEVEL_END /*2*/:
                    boolean z;
                    if (this.mT1Recv.rBlockErrors()) {
                        Util.logDebug("card signals transmission error");
                        retransmitIBlock += EXT_APDU_LEVEL_BEGIN;
                        if (retransmitIBlock > EXT_APDU_LEVEL_END) {
                            if (resyncRetransmit) {
                                Util.logError("resync stage 2, no change, completely giving up");
                                return null;
                            }
                            Util.logError("trying resync and i-block retransmit");
                            resyncTpduT1();
                            resyncRetransmit = PROTOCOL_T0;
                        }
                    } else {
                        retransmitIBlock = EXT_APDU_LEVEL_BEGIN_AND_END;
                        resyncRetransmit = PROTOCOL_T1;
                    }
                    T1Handler t1Handler = this.mT1Send;
                    if (retransmitIBlock > 0) {
                        z = PROTOCOL_T0;
                    } else {
                        z = PROTOCOL_T1;
                    }
                    block = t1Handler.getNextIBlock(z);
                    break;
                case EXT_APDU_LEVEL_MIDDLE /*3*/:
                    switch (this.mT1Recv.getSBlockType()) {
                        case T1Handler.S_BLOCK_ABORT_REQUEST /*194*/:
                            block = this.mT1Recv.getSBlock(T1Handler.S_BLOCK_ABORT_RESPONSE, EXT_APDU_LEVEL_BEGIN_AND_END);
                            abort = PROTOCOL_T0;
                            break;
                        case T1Handler.S_BLOCK_WTX_REQUEST /*195*/:
                            block = this.mT1Recv.getSBlock(T1Handler.S_BLOCK_WTX_RESPONSE, block[EXT_APDU_LEVEL_MIDDLE]);
                            break;
                        case T1Handler.S_BLOCK_IFSC_RESPONSE /*225*/:
                            int ifs = block[EXT_APDU_LEVEL_MIDDLE] & 0xff;
                            if (ifs != this.mCcidDescriptor.maxIFSD()) {
                                Util.logDebug("card does not accept IFS " + this.mCcidDescriptor.maxIFSD() + ", reports " + ifs);
                            } else {
                                Util.logDebug("card accepted IFS " + ifs);
                            }
                            if (ifs <= this.mATR.mT1_IFSC) {
                                this.mT1Send.setBlockSize(ifs);
                            }
                            this.mIFSSet = PROTOCOL_T0;
                            block = this.mT1Send.getNextIBlock(PROTOCOL_T1);
                            break;
                        default:
                            Util.logError("unexpected s-block type " + Util.formatByte(this.mT1Recv.getSBlockType()));
                            return null;
                    }
                default:
                    return null;
            }
        }
        return this.mT1Recv.getPDU();
    }

    private boolean resyncTpduT1() {
        Util.logInfo(BuildConfig.FLAVOR);
        byte[] command = new byte[PROTOCOL_DATA_LEN_T0];
        command[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 26;
        command[EXT_APDU_LEVEL_BEGIN] = (byte) 0;
        command[EXT_APDU_LEVEL_END] = (byte) -64;
        command[EXT_APDU_LEVEL_MIDDLE] = (byte) 0;
        command[4] = (byte) -64;
        if (!PC_to_RDR_Escape(command) || RDR_to_PC_Escape() == null) {
            return PROTOCOL_T1;
        }
        this.mT1Send.resetCounters();
        this.mT1Recv.resetCounters();
        return PROTOCOL_T0;
    }

    private boolean PC_to_RDR(byte[] command, boolean viaEscape, boolean t0) {
        Util.logDebug("PC_to_RDR: viaEscape = " + viaEscape);
        if (!viaEscape) {
            return PC_to_RDR_XfrBlock(command, EXT_APDU_LEVEL_BEGIN_AND_END);
        }
        if (t0) {
            return PC_to_RDR_Escape(command);
        }
        byte[] commandWithPrefix = new byte[(command.length + EXT_APDU_LEVEL_BEGIN)];
        commandWithPrefix[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 26;
        System.arraycopy(command, EXT_APDU_LEVEL_BEGIN_AND_END, commandWithPrefix, EXT_APDU_LEVEL_BEGIN, command.length);
        return PC_to_RDR_Escape(commandWithPrefix);
    }

    private byte[] RDR_to_PC(boolean viaEscape, boolean t0) {
        Util.logDebug("RDR_to_PC: viaEscape = " + viaEscape);
        if (!viaEscape) {
            return RDR_to_PC_DataBlock();
        }
        byte[] response = RDR_to_PC_Escape();
        if (t0) {
            byte[] responseWithoutPrefix = new byte[(response.length - 5)];
            System.arraycopy(response, PROTOCOL_DATA_LEN_T0, responseWithoutPrefix, EXT_APDU_LEVEL_BEGIN_AND_END, response.length - 5);
            return responseWithoutPrefix;
        }
        byte[] responseWithoutPrefix = new byte[(response.length + SLOT_ERROR_FAILED)];
        System.arraycopy(response, EXT_APDU_LEVEL_BEGIN, responseWithoutPrefix, EXT_APDU_LEVEL_BEGIN_AND_END, response.length + SLOT_ERROR_FAILED);
        return responseWithoutPrefix;
    }

    private boolean PC_to_RDR_IccPowerOn() {
        Util.logInfo(BuildConfig.FLAVOR);
        byte[] out = new byte[RDR_TO_PC_SLOT_STATUS_LEN];
        out[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 98;
        out[EXT_APDU_LEVEL_BEGIN] = (byte) 0;
        out[EXT_APDU_LEVEL_END] = (byte) 0;
        out[EXT_APDU_LEVEL_MIDDLE] = (byte) 0;
        out[4] = (byte) 0;
        out[PROTOCOL_DATA_LEN_T0] = (byte) 0;
        out[6] = getNextSequenceNumber();
        out[PROTOCOL_DATA_LEN_T1] = this.mCcidDescriptor.voltageSupport();
        out[8] = (byte) 0;
        out[9] = (byte) 0;
        return bulkOut(out, MESSAGE_TYPE_PC_ICC_POWER_ON);
    }

    private boolean PC_to_RDR_IccPowerOff() {
        Util.logInfo(BuildConfig.FLAVOR);
        byte[] out = new byte[RDR_TO_PC_SLOT_STATUS_LEN];
        out[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 99;
        out[EXT_APDU_LEVEL_BEGIN] = (byte) 0;
        out[EXT_APDU_LEVEL_END] = (byte) 0;
        out[EXT_APDU_LEVEL_MIDDLE] = (byte) 0;
        out[4] = (byte) 0;
        out[PROTOCOL_DATA_LEN_T0] = (byte) 0;
        out[6] = getNextSequenceNumber();
        out[PROTOCOL_DATA_LEN_T1] = (byte) 0;
        out[8] = (byte) 0;
        out[9] = (byte) 0;
        return bulkOut(out, MESSAGE_TYPE_PC_ICC_POWER_OFF);
    }

    private boolean PC_to_RDR_GetSlotStatus() {
        Util.logInfo(BuildConfig.FLAVOR);
        byte[] out = new byte[RDR_TO_PC_SLOT_STATUS_LEN];
        out[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 101;
        out[EXT_APDU_LEVEL_BEGIN] = (byte) 0;
        out[EXT_APDU_LEVEL_END] = (byte) 0;
        out[EXT_APDU_LEVEL_MIDDLE] = (byte) 0;
        out[4] = (byte) 0;
        out[PROTOCOL_DATA_LEN_T0] = (byte) 0;
        out[6] = getNextSequenceNumber();
        out[PROTOCOL_DATA_LEN_T1] = (byte) 0;
        out[8] = (byte) 0;
        out[9] = (byte) 0;
        return bulkOut(out, MESSAGE_TYPE_PC_GET_SLOT_STATUS);
    }

    private boolean PC_to_RDR_XfrBlockExtendedLength(byte[] command) {
        boolean success;
        if (command.length + RDR_TO_PC_SLOT_STATUS_LEN <= this.mCcidDescriptor.maxCCIDMessageLength()) {
            success = PC_to_RDR_XfrBlock(command, EXT_APDU_LEVEL_BEGIN_AND_END);
        } else {
            int bytesLeft = command.length;
            byte[] commandFragment = new byte[(this.mCcidDescriptor.maxCCIDMessageLength() - 10)];
            System.arraycopy(command, EXT_APDU_LEVEL_BEGIN_AND_END, commandFragment, EXT_APDU_LEVEL_BEGIN_AND_END, commandFragment.length);
            if (!PC_to_RDR_XfrBlock(commandFragment, EXT_APDU_LEVEL_BEGIN)) {
                return PROTOCOL_T1;
            }
            bytesLeft -= commandFragment.length;
            byte[] dummyRes = RDR_to_PC_DataBlock();
            while (bytesLeft > this.mCcidDescriptor.maxCCIDMessageLength() - 10) {
                commandFragment = new byte[(this.mCcidDescriptor.maxCCIDMessageLength() - 10)];
                System.arraycopy(command, command.length - bytesLeft, commandFragment, EXT_APDU_LEVEL_BEGIN_AND_END, commandFragment.length);
                if (!PC_to_RDR_XfrBlock(commandFragment, EXT_APDU_LEVEL_MIDDLE)) {
                    return PROTOCOL_T1;
                }
                bytesLeft -= commandFragment.length;
                dummyRes = RDR_to_PC_DataBlock();
            }
            commandFragment = new byte[bytesLeft];
            System.arraycopy(command, command.length - bytesLeft, commandFragment, EXT_APDU_LEVEL_BEGIN_AND_END, commandFragment.length);
            success = PC_to_RDR_XfrBlock(commandFragment, EXT_APDU_LEVEL_END);
            int length = bytesLeft - commandFragment.length;
        }
        return success;
    }

    private boolean PC_to_RDR_XfrBlock(byte[] abData, int wLevelParameter) {
        Util.logInfo(BuildConfig.FLAVOR);
        int length = abData.length;
        byte[] out = new byte[(abData.length + RDR_TO_PC_SLOT_STATUS_LEN)];
        out[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 111;
        out[EXT_APDU_LEVEL_BEGIN] = (byte) (length & 0xff);
        out[EXT_APDU_LEVEL_END] = (byte) ((length >> 8) & 0xff);
        out[EXT_APDU_LEVEL_MIDDLE] = (byte) ((length >> EXT_APDU_LEVEL_EMPTY) & 0xff);
        out[4] = (byte) ((length >> 24) & 0xff);
        out[PROTOCOL_DATA_LEN_T0] = (byte) 0;
        out[6] = getNextSequenceNumber();
        out[PROTOCOL_DATA_LEN_T1] = BLOCK_WAITING_TIME_EXTENDER;
        out[8] = (byte) (wLevelParameter & 0xff);
        out[9] = (byte) ((wLevelParameter >> 8) & 0xff);
        System.arraycopy(abData, EXT_APDU_LEVEL_BEGIN_AND_END, out, RDR_TO_PC_SLOT_STATUS_LEN, abData.length);
        Util.logDebug("Sending out " + out.length + " bytes of data to the reader.");
        Util.logDebug("mCcidDescriptor.maxCCIDMessageLength() = " + this.mCcidDescriptor.maxCCIDMessageLength());
        return bulkOut(out, MESSAGE_TYPE_PC_XFR_BLOCK);
    }

    private boolean PC_to_RDR_GetParameters() {
        Util.logInfo(BuildConfig.FLAVOR);
        byte[] out = new byte[RDR_TO_PC_SLOT_STATUS_LEN];
        out[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 108;
        out[EXT_APDU_LEVEL_BEGIN] = (byte) 0;
        out[EXT_APDU_LEVEL_END] = (byte) 0;
        out[EXT_APDU_LEVEL_MIDDLE] = (byte) 0;
        out[4] = (byte) 0;
        out[PROTOCOL_DATA_LEN_T0] = (byte) 0;
        out[6] = getNextSequenceNumber();
        out[PROTOCOL_DATA_LEN_T1] = (byte) 0;
        out[8] = (byte) 0;
        out[9] = (byte) 0;
        return bulkOut(out, MESSAGE_TYPE_PC_GET_PARAMETERS);
    }

    private boolean PC_to_RDR_SetParameters(byte bProtocolNum, byte[] abProtocolData) {
        Util.logInfo(BuildConfig.FLAVOR);
        byte[] out = new byte[(abProtocolData.length + RDR_TO_PC_SLOT_STATUS_LEN)];
        out[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 97;
        out[EXT_APDU_LEVEL_BEGIN] = (byte) (abProtocolData.length & 0xff);
        out[EXT_APDU_LEVEL_END] = (byte) ((abProtocolData.length >> 8) & 0xff);
        out[EXT_APDU_LEVEL_MIDDLE] = (byte) ((abProtocolData.length >> EXT_APDU_LEVEL_EMPTY) & 0xff);
        out[4] = (byte) ((abProtocolData.length >> 24) & 0xff);
        out[PROTOCOL_DATA_LEN_T0] = (byte) 0;
        out[6] = getNextSequenceNumber();
        out[PROTOCOL_DATA_LEN_T1] = bProtocolNum;
        out[8] = (byte) 0;
        out[9] = (byte) 0;
        System.arraycopy(abProtocolData, EXT_APDU_LEVEL_BEGIN_AND_END, out, RDR_TO_PC_SLOT_STATUS_LEN, abProtocolData.length);
        return bulkOut(out, MESSAGE_TYPE_PC_SET_PARAMETERS);
    }

    private boolean PC_to_RDR_Escape(byte[] abData) {
        Util.logInfo(BuildConfig.FLAVOR);
        byte[] out = new byte[(abData.length + RDR_TO_PC_SLOT_STATUS_LEN)];
        out[EXT_APDU_LEVEL_BEGIN_AND_END] = (byte) 107;
        out[EXT_APDU_LEVEL_BEGIN] = (byte) (abData.length & 0xff);
        out[EXT_APDU_LEVEL_END] = (byte) ((abData.length >> 8) & 0xff);
        out[EXT_APDU_LEVEL_MIDDLE] = (byte) ((abData.length >> EXT_APDU_LEVEL_EMPTY) & 0xff);
        out[4] = (byte) ((abData.length >> 24) & 0xff);
        out[PROTOCOL_DATA_LEN_T0] = (byte) 0;
        out[6] = getNextSequenceNumber();
        out[PROTOCOL_DATA_LEN_T1] = (byte) 0;
        out[8] = (byte) 0;
        out[9] = (byte) 0;
        System.arraycopy(abData, EXT_APDU_LEVEL_BEGIN_AND_END, out, RDR_TO_PC_SLOT_STATUS_LEN, abData.length);
        return bulkOut(out, MESSAGE_TYPE_PC_ESCAPE);
    }

    private byte[] RDR_to_PC_DataBlock() {
        byte[] result = new byte[EXT_APDU_LEVEL_BEGIN_AND_END];
        do {
            Util.logDebug("result.length: " + result.length);
            Util.logDebug("calling bulkIn...");
            boolean success = bulkIn(this.mRdrToPcIn);
            Util.logDebug("calling bulkIn...done");
            if (success) {
                int dwLength = (((this.mRdrToPcIn[EXT_APDU_LEVEL_BEGIN] & 0xff) | ((this.mRdrToPcIn[EXT_APDU_LEVEL_END] << 8) & 0xff00)) | ((this.mRdrToPcIn[EXT_APDU_LEVEL_MIDDLE] << EXT_APDU_LEVEL_EMPTY) & 16711680)) | ((this.mRdrToPcIn[4] << 24) & 0xff000000);
                if (dwLength > RDR_TO_PC_MAXLEN) {
                    Util.logWarn("dwLength exceeds 65548");
                }
                byte[] newResult = new byte[(result.length + dwLength)];
                System.arraycopy(result, EXT_APDU_LEVEL_BEGIN_AND_END, newResult, EXT_APDU_LEVEL_BEGIN_AND_END, result.length);
                System.arraycopy(this.mRdrToPcIn, RDR_TO_PC_SLOT_STATUS_LEN, newResult, result.length, dwLength);
                result = newResult;
                if (this.mCommunicator.getCcidDescriptor().featExchange() == CcidDescriptor.FEAT_EXCHANGE_EXTENDED) {
                    int bChainParameter = this.mRdrToPcIn[9] & 0xff;
                    if (!(bChainParameter == 0 || bChainParameter == EXT_APDU_LEVEL_END || bChainParameter == EXT_APDU_LEVEL_EMPTY)) {
                    }
                }
                return result;
            }
            Util.logError("bulkIn failed!");
            return null;
        } while (PC_to_RDR_XfrBlock(new byte[EXT_APDU_LEVEL_BEGIN_AND_END], EXT_APDU_LEVEL_EMPTY));
        //Util.logError("PC_to_RDR_XfrBlock failed!");
        //return null;
    }

    private boolean RDR_to_PC_SlotStatus() {
        if (bulkIn(new byte[RDR_TO_PC_SLOT_STATUS_LEN])) {
            return PROTOCOL_T0;
        }
        return PROTOCOL_T1;
    }

    private byte[] RDR_to_PC_Parameters() {
        if (!bulkIn(this.mRdrToPcIn)) {
            return null;
        }
        int dwLength = (((this.mRdrToPcIn[EXT_APDU_LEVEL_BEGIN] & 0xff) | ((this.mRdrToPcIn[EXT_APDU_LEVEL_END] << 8) & 0xff00)) | ((this.mRdrToPcIn[EXT_APDU_LEVEL_MIDDLE] << EXT_APDU_LEVEL_EMPTY) & 16711680)) | ((this.mRdrToPcIn[4] << 24) & 0xff000000);
        Util.logDebug("dwLength = 0x" + Integer.toHexString(dwLength));
        if (dwLength > RDR_TO_PC_MAXLEN) {
            Util.logError("dwLength exceeds 65548");
            return null;
        } else if (dwLength == PROTOCOL_DATA_LEN_T0 || dwLength == PROTOCOL_DATA_LEN_T1) {
            byte[] abProtocolData = new byte[(dwLength + EXT_APDU_LEVEL_BEGIN)];
            System.arraycopy(this.mRdrToPcIn, 9, abProtocolData, EXT_APDU_LEVEL_BEGIN_AND_END, dwLength);
            return abProtocolData;
        } else {
            Util.logError("dwLength does not match abProtocolDataStructureLength");
            return null;
        }
    }

    private byte[] RDR_to_PC_Escape() {
        Util.logInfo(BuildConfig.FLAVOR);
        if (!bulkIn(this.mRdrToPcIn)) {
            return null;
        }
        int dwLength = (((this.mRdrToPcIn[EXT_APDU_LEVEL_BEGIN] & 0xff) | ((this.mRdrToPcIn[EXT_APDU_LEVEL_END] << 8) & 0xff00)) | ((this.mRdrToPcIn[EXT_APDU_LEVEL_MIDDLE] << EXT_APDU_LEVEL_EMPTY) & 16711680)) | ((this.mRdrToPcIn[4] << 24) & 0xff000000);
        Util.logDebug("dwLength = 0x" + Integer.toHexString(dwLength));
        if (dwLength > RDR_TO_PC_MAXLEN) {
            Util.logError("dwLength exceeds 65548");
            return null;
        }
        byte[] result = new byte[dwLength];
        System.arraycopy(this.mRdrToPcIn, RDR_TO_PC_SLOT_STATUS_LEN, result, EXT_APDU_LEVEL_BEGIN_AND_END, dwLength);
        return result;
    }
}
