package freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid;

import java.util.ArrayList;

import freelancerwatermellon.serbianeidomnikey.cardreadermanager.BuildConfig;

import static freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util.byteArrayToString;
import static freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util.formatByte;
import static freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util.logDebug;
import static freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util.logError;
import static freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util.lrc;

public class T1Handler {
    public static final int BLOCK_TYPE_I = 1;
    public static final int BLOCK_TYPE_R = 2;
    public static final int BLOCK_TYPE_S = 3;
    public static final int S_BLOCK_ABORT_REQUEST = 194;
    public static final int S_BLOCK_ABORT_RESPONSE = 226;
    public static final int S_BLOCK_IFSC_REQUEST = 193;
    public static final int S_BLOCK_IFSC_RESPONSE = 225;
    public static final int S_BLOCK_RESYNC_REQUEST = 192;
    public static final int S_BLOCK_RESYNC_RESPONSE = 224;
    public static final int S_BLOCK_WTX_REQUEST = 195;
    public static final int S_BLOCK_WTX_RESPONSE = 227;
    private static final int PURPOSE_PARSE = 2;
    private static final int PURPOSE_PREPARE = 1;
    private byte[] mBlock;
    private int mBlockSize;
    private int mBlockType;
    private boolean mCRC;
    private int[] mCRCTable;
    private byte[] mCurrentBlock;
    private boolean mLastBlock;
    private int mNextSendSeq;
    private ArrayList<Byte> mPDU;
    private int mPos;
    private int mPurpose;
    private byte[] mRaw;
    private int mRecvSeq;
    private int mSBlockType;
    private int mSendSeq;

    public T1Handler(boolean crc) {
        this.mPurpose = 0;
        this.mBlockType = 0;
        this.mSBlockType = 0;
        this.mRaw = null;
        this.mCurrentBlock = null;
        this.mBlock = null;
        this.mPDU = null;
        this.mBlockSize = 0;
        this.mCRC = false;
        this.mSendSeq = 0;
        this.mRecvSeq = 0;
        this.mNextSendSeq = 0;
        this.mPos = 0;
        this.mLastBlock = false;
        this.mCRCTable = null;
        this.mCRC = crc;
        if (this.mCRC) {
            crcInit(4129);
        }
        this.mPurpose = PURPOSE_PARSE;
    }

    public T1Handler(int blockSize, boolean crc) {
        this.mPurpose = 0;
        this.mBlockType = 0;
        this.mSBlockType = 0;
        this.mRaw = null;
        this.mCurrentBlock = null;
        this.mBlock = null;
        this.mPDU = null;
        this.mBlockSize = 0;
        this.mCRC = false;
        this.mSendSeq = 0;
        this.mRecvSeq = 0;
        this.mNextSendSeq = 0;
        this.mPos = 0;
        this.mLastBlock = false;
        this.mCRCTable = null;
        this.mBlockSize = blockSize;
        this.mCRC = crc;
        if (this.mCRC) {
            crcInit(4129);
        }
        this.mPurpose = PURPOSE_PREPARE;
    }

    public void setBlockSize(int blockSize) {
        this.mBlockSize = blockSize;
        logDebug("reset T=1 block size to " + blockSize);
    }

    public void putRaw(PduAnalyzer pduAnalyzer) {
        byte[] command = pduAnalyzer.getCommand();
        this.mRaw = new byte[command.length];
        System.arraycopy(command, 0, this.mRaw, 0, command.length);
        this.mLastBlock = false;
        this.mCurrentBlock = null;
        this.mPos = 0;
    }

    public int putBlock(byte[] block) {
        if (this.mPurpose != PURPOSE_PARSE) {
            logError("purpose of this object is not parse");
            return 0;
        }
        int eccLen;
        logDebug(byteArrayToString(block));
        if (this.mCRC) {
            eccLen = PURPOSE_PARSE;
        } else {
            eccLen = PURPOSE_PREPARE;
        }
        if (block.length < eccLen + BLOCK_TYPE_S) {
            logError("block too short to be a T=1 block");
            logError("eccLen = " + eccLen);
            logError("mCRC = " + this.mCRC);
            logError(BuildConfig.FLAVOR);
            return 0;
        }
        if ((this.mCRC ? crc(block, 0) : lrc(block)) != 0) {
            logError("checksum verification failed");
            return 0;
        } else if (block[0] != 0) {
            logError("NAD non-zero, node addressing not supported");
            return 0;
        } else {
            int len = block[PURPOSE_PARSE] & 255;
            if (len == 255) {
                logError("LEN field 0xff is RFU");
                return 0;
            }
            int expectedLen = (len + BLOCK_TYPE_S) + eccLen;
            if (block.length != expectedLen) {
                logError("block has invalid length " + formatByte(block.length) + ", expected " + formatByte(expectedLen));
                return 0;
            }
            int type = (block[PURPOSE_PREPARE] & 255) >> 6;
            int i;
            if (type == 0 || type == PURPOSE_PREPARE) {
                int expectedSeq;
                this.mBlockType = PURPOSE_PREPARE;
                if ((block[PURPOSE_PREPARE] & 64) == 0) {
                    expectedSeq = 0;
                } else {
                    expectedSeq = PURPOSE_PREPARE;
                }
                if (expectedSeq != this.mRecvSeq) {
                    logError("i-block unexpected sequence number");
                    return 0;
                }
                boolean z;
                if (this.mRecvSeq > 0) {
                    i = 0;
                } else {
                    i = PURPOSE_PREPARE;
                }
                this.mRecvSeq = i;
                if ((block[PURPOSE_PREPARE] & 32) == 0) {
                    z = true;
                } else {
                    z = false;
                }
                this.mLastBlock = z;
                savePDU(block, len);
                logDebug("i-block, pcb = " + formatByte(block[PURPOSE_PREPARE]));
            } else if (type == PURPOSE_PARSE) {
                this.mBlockType = PURPOSE_PARSE;
                if ((block[PURPOSE_PREPARE] & 16) == 0) {
                    i = 0;
                } else {
                    i = PURPOSE_PREPARE;
                }
                this.mNextSendSeq = i;
                logDebug("r-block, pcb = " + formatByte(block[PURPOSE_PREPARE]));
            } else if (type == BLOCK_TYPE_S) {
                this.mBlockType = BLOCK_TYPE_S;
                this.mSBlockType = block[PURPOSE_PREPARE] & 255;
                logDebug("s-block, pcb = " + formatByte(block[PURPOSE_PREPARE]));
            } else {
                logError("unexpected block type " + formatByte(block[PURPOSE_PREPARE]));
                return 0;
            }
            this.mBlock = new byte[block.length];
            System.arraycopy(block, 0, this.mBlock, 0, block.length);
            return this.mBlockType;
        }
    }

    public byte[] getNextIBlock(boolean retransmit) {
        int i = PURPOSE_PREPARE;
        if (this.mPurpose != PURPOSE_PREPARE) {
            logError("purpose of this object is not prepare");
            return null;
        } else if (this.mLastBlock) {
            return null;
        } else {
            if (retransmit) {
                return this.mCurrentBlock;
            }
            int eccLen;
            boolean z;
            int len = this.mRaw.length - this.mPos;
            if (this.mCRC) {
                eccLen = PURPOSE_PARSE;
            } else {
                eccLen = PURPOSE_PREPARE;
            }
            if (len > this.mBlockSize) {
                len = this.mBlockSize;
            }
            if (this.mPos + len >= this.mRaw.length) {
                z = true;
            } else {
                z = false;
            }
            this.mLastBlock = z;
            byte[] iblock = new byte[((len + BLOCK_TYPE_S) + eccLen)];
            iblock[0] = (byte) 0;
            if (this.mSendSeq > 0) {
                iblock[PURPOSE_PREPARE] = (byte) (iblock[PURPOSE_PREPARE] | 64);
            }
            if (!this.mLastBlock) {
                iblock[PURPOSE_PREPARE] = (byte) (iblock[PURPOSE_PREPARE] | 32);
            }
            iblock[PURPOSE_PARSE] = (byte) len;
            System.arraycopy(this.mRaw, this.mPos, iblock, BLOCK_TYPE_S, len);
            if (this.mCRC) {
                int checksum = crc(iblock, 0);
                iblock[iblock.length - 2] = (byte) ((checksum >> 8) & 255);
                iblock[iblock.length - 1] = (byte) (checksum & 255);
            } else {
                iblock[iblock.length - 1] = lrc(iblock);
            }
            if (this.mSendSeq > 0) {
                i = 0;
            }
            this.mSendSeq = i;
            this.mPos += len;
            this.mCurrentBlock = new byte[iblock.length];
            System.arraycopy(iblock, 0, this.mCurrentBlock, 0, iblock.length);
            return iblock;
        }
    }

    public boolean blocksDone() {
        return this.mLastBlock;
    }

    public boolean rBlockErrors() {
        return (this.mBlock[PURPOSE_PREPARE] & BLOCK_TYPE_S) != 0;
    }

    public byte[] getRBlock() {
        int eccLen;
        if (this.mCRC) {
            eccLen = PURPOSE_PARSE;
        } else {
            eccLen = PURPOSE_PREPARE;
        }
        byte[] rblock = new byte[(eccLen + BLOCK_TYPE_S)];
        rblock[0] = (byte) 0;
        rblock[PURPOSE_PREPARE] = Byte.MIN_VALUE;
        if (this.mRecvSeq > 0) {
            rblock[PURPOSE_PREPARE] = (byte) (rblock[PURPOSE_PREPARE] | 16);
        }
        rblock[PURPOSE_PARSE] = (byte) 0;
        if (this.mCRC) {
            int checksum = crc(rblock, 0);
            rblock[rblock.length - 2] = (byte) ((checksum >> 8) & 255);
            rblock[rblock.length - 1] = (byte) (checksum & 255);
        } else {
            rblock[rblock.length - 1] = lrc(rblock);
        }
        return rblock;
    }

    public int getSBlockType() {
        return this.mSBlockType;
    }

    public byte[] getSBlock(int type, int data) {
        int eccLen;
        byte[] sblock;
        if (this.mCRC) {
            eccLen = PURPOSE_PARSE;
        } else {
            eccLen = PURPOSE_PREPARE;
        }
        switch (type) {
            case S_BLOCK_IFSC_REQUEST /*193*/:
                sblock = new byte[(eccLen + 4)];
                sblock[0] = (byte) 0;
                sblock[PURPOSE_PREPARE] = (byte) -63;
                sblock[PURPOSE_PARSE] = (byte) 1;
                sblock[BLOCK_TYPE_S] = (byte) data;
                break;
            case S_BLOCK_ABORT_RESPONSE /*226*/:
                this.mLastBlock = true;
                sblock = new byte[(eccLen + BLOCK_TYPE_S)];
                sblock[0] = (byte) 0;
                sblock[PURPOSE_PREPARE] = (byte) -30;
                sblock[PURPOSE_PARSE] = (byte) 0;
                break;
            case S_BLOCK_WTX_RESPONSE /*227*/:
                sblock = new byte[(eccLen + 4)];
                sblock[0] = (byte) 0;
                sblock[PURPOSE_PREPARE] = (byte) -29;
                sblock[PURPOSE_PARSE] = (byte) 1;
                sblock[BLOCK_TYPE_S] = (byte) data;
                break;
            default:
                return null;
        }
        if (this.mCRC) {
            int checksum = crc(sblock, 0);
            sblock[sblock.length - 2] = (byte) ((checksum >> 8) & 255);
            sblock[sblock.length - 1] = (byte) (checksum & 255);
        } else {
            sblock[sblock.length - 1] = lrc(sblock);
        }
        return sblock;
    }

    public void resetCounters() {
        this.mSendSeq = 0;
        this.mRecvSeq = 0;
        this.mNextSendSeq = 0;
    }

    public byte[] getPDU() {
        if (!blocksDone()) {
            return null;
        }
        byte[] pdu = new byte[this.mPDU.size()];
        for (int i = 0; i < this.mPDU.size(); i += PURPOSE_PREPARE) {
            pdu[i] = ((Byte) this.mPDU.get(i)).byteValue();
        }
        this.mPDU = null;
        return pdu;
    }

    private void savePDU(byte[] block, int len) {
        if (this.mPDU == null) {
            this.mPDU = new ArrayList(len);
        }
        for (int i = BLOCK_TYPE_S; i < len + BLOCK_TYPE_S; i += PURPOSE_PREPARE) {
            this.mPDU.add(new Byte(block[i]));
        }
    }

    private void crcInit(int polynom) {
        this.mCRCTable = new int[CcidDescriptor.FEAT_SET_ICC];
        for (int i = 0; i < CcidDescriptor.FEAT_SET_ICC; i += PURPOSE_PREPARE) {
            int reminder = i << 8;
            for (int j = 0; j < 8; j += PURPOSE_PREPARE) {
                if ((0x00008000 & reminder) == PURPOSE_PREPARE) {
                    reminder = (reminder << PURPOSE_PREPARE) ^ polynom;
                } else {
                    reminder <<= PURPOSE_PREPARE;
                }
            }
            this.mCRCTable[i] = reminder;
        }
    }

    private int crc(byte[] data, int reminder) {
        for (int i = 0; i < data.length; i += PURPOSE_PREPARE) {
            reminder = this.mCRCTable[(byte) (data[i] ^ (reminder >> 8))] ^ (reminder << 8);
        }
        return reminder;
    }
}
