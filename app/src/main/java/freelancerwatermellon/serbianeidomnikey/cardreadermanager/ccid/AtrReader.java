package freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid;

import android.support.v4.media.TransportMediator;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;

import com.freelancewatermelon.licnakarta.cardreadermanager.Util;

public class AtrReader {
    public static final int CONVENTION_DIRECT = 1;
    public static final int CONVENTION_INVERSE = 2;
    public static final int[] Di;
    public static final int[] Fi;

    static {
        Fi = new int[]{372, 372, 558, 744, 1116, 1488, 1860, 0, 0, CcidDescriptor.FEAT_NAD_VALUE_OTHER_THAN_0, 768, CcidDescriptor.FEAT_AUTOMATIC_IFSD_EXCHANGE, 1536, AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT, 0, 0};
        Di = new int[]{CONVENTION_DIRECT, CONVENTION_INVERSE, 4, 8, 16, 32, 0, 12, 20, 0, 0, 0, 0, 0, 0};
    }

    public int mConvention;
    public int mD;
    public byte mDI;
    public int mF;
    public byte mFI;
    public byte mGuard;
    public int mHistoricalChars;
    public byte mProtocol;
    public int mProtocolCnt;
    public int mProtocols;
    public byte mT0_WI;
    public byte mT1_BWI;
    public boolean mT1_CRC;
    public byte mT1_CWI;
    public int mT1_IFSC;
    private byte[] mRaw;

    public AtrReader(byte[] rawATR) {
        this.mConvention = 0;
        this.mHistoricalChars = 0;
        this.mProtocols = 0;
        this.mProtocolCnt = 0;
        this.mProtocol = (byte) 0;
        this.mFI = (byte) 0;
        this.mF = 0;
        this.mDI = (byte) 0;
        this.mD = 0;
        this.mGuard = (byte) 0;
        this.mT0_WI = (byte) 0;
        this.mT1_IFSC = 0;
        this.mT1_CWI = (byte) 0;
        this.mT1_BWI = (byte) 0;
        this.mT1_CRC = false;
        this.mRaw = null;
        this.mRaw = new byte[rawATR.length];
        System.arraycopy(rawATR, 0, this.mRaw, 0, rawATR.length);
    }

    public boolean parse() {
        int length = this.mRaw.length;
        if (length >= CONVENTION_INVERSE) {
            length = this.mRaw.length;
            if (length <= 33) {
                this.mFI = (byte) 1;
                this.mDI = (byte) 1;
                this.mT0_WI = (byte) 10;
                this.mT1_IFSC = 32;
                this.mT1_CWI = (byte) 13;
                this.mT1_BWI = (byte) 4;
                Util.logDebug("parsing ATR " + Util.byteArrayToString(this.mRaw));
                int pos = 0 + CONVENTION_DIRECT;
                int ts = this.mRaw[0] & MotionEventCompat.ACTION_MASK;
                switch (ts) {
                    case 59:
                        Util.logDebug("TS: direct convention");
                        this.mConvention = CONVENTION_DIRECT;
                        break;
                    case 63:
                        Util.logDebug("TS: inverse convention");
                        this.mConvention = CONVENTION_INVERSE;
                        break;
                    default:
                        Util.logError("wrong TS: 0x" + Util.formatByte(ts));
                        return false;
                }
                boolean td = true;
                boolean tck = false;
                boolean t1Block = false;
                boolean t15Block = false;
                int firstOffer = 0;
                int ta2 = -1;
                int i = CONVENTION_DIRECT;
                while (td) {
                    int tbi;
                    int tci;
                    int pos2 = pos + CONVENTION_DIRECT;
                    int tdi = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                    int T = tdi & 15;
                    boolean ta = (tdi & 16) != 0;
                    boolean tb = (tdi & 32) != 0;
                    boolean tc = (tdi & 64) != 0;
                    td = (tdi & TransportMediator.FLAG_KEY_MEDIA_NEXT) != 0;
                    if (i == CONVENTION_DIRECT) {
                        if (ta) {
                            pos = pos2 + CONVENTION_DIRECT;
                            int ta1 = this.mRaw[pos2] & MotionEventCompat.ACTION_MASK;
                            this.mFI = (byte) (ta1 >> 4);
                            this.mDI = (byte) (ta1 & 15);
                            Util.logDebug("TA1: fi = " + Util.formatByte(this.mFI) + ", di = " + Util.formatByte(this.mDI));
                        } else {
                            pos = pos2;
                        }
                        if (tb) {
                            pos2 = pos + CONVENTION_DIRECT;
                            Util.logDebug("TB1: " + Util.formatByte(this.mRaw[pos] & MotionEventCompat.ACTION_MASK));
                            pos = pos2;
                        }
                        if (tc) {
                            pos2 = pos + CONVENTION_DIRECT;
                            this.mGuard = (byte) (this.mRaw[pos] & MotionEventCompat.ACTION_MASK);
                            Util.logDebug("TC1: guard = " + Util.formatByte(this.mGuard));
                        }
                        if (i > CONVENTION_DIRECT && T != 0) {
                            tck = true;
                        }
                        if (i == CONVENTION_INVERSE) {
                            firstOffer = T;
                            if (ta) {
                                pos2 = pos + CONVENTION_DIRECT;
                                ta2 = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                                Util.logDebug("TA2: " + Util.formatByte(ta2));
                                pos = pos2;
                            }
                            if (tb) {
                                pos2 = pos + CONVENTION_DIRECT;
                                Util.logDebug("TB2: " + Util.formatByte(this.mRaw[pos] & MotionEventCompat.ACTION_MASK));
                                pos = pos2;
                            }
                            if (tc) {
                                pos2 = pos + CONVENTION_DIRECT;
                                this.mT0_WI = (byte) (this.mRaw[pos] & MotionEventCompat.ACTION_MASK);
                                Util.logDebug("TC2: T0 WI = " + Util.formatByte(this.mT0_WI));
                                pos = pos2;
                            }
                        }
                        if (i > CONVENTION_INVERSE && T == CONVENTION_DIRECT) {
                            if (!t1Block) {
                                if (ta) {
                                    pos2 = pos + CONVENTION_DIRECT;
                                    this.mT1_IFSC = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                                    Util.logDebug("TA" + i + ": T1 IFSC = " + Util.formatByte(this.mT1_IFSC));
                                    pos = pos2;
                                }
                                if (tb) {
                                    pos2 = pos + CONVENTION_DIRECT;
                                    tbi = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                                    this.mT1_CWI = (byte) (tbi & 15);
                                    this.mT1_BWI = (byte) (tbi >> 4);
                                    Util.logDebug("TB" + i + ": T1 CWI = " + Util.formatByte(this.mT1_CWI) + ", T1 BWI = " + Util.formatByte(this.mT1_BWI));
                                    pos = pos2;
                                }
                                if (tc) {
                                    pos2 = pos + CONVENTION_DIRECT;
                                    tci = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                                    this.mT1_CRC = (tci & CONVENTION_DIRECT) != CONVENTION_DIRECT;
                                    Util.logDebug("TC" + i + ": T1 CRC = " + Util.formatByte(tci & CONVENTION_DIRECT));
                                    t1Block = true;
                                    pos = pos2;
                                }
                            }
                            pos2 = pos;
                            t1Block = true;
                            pos = pos2;
                        }
                        if (i > CONVENTION_INVERSE || T != 15) {
                            pos2 = pos;
                        } else {
                            if (t15Block || !ta) {
                                pos2 = pos;
                            } else {
                                pos2 = pos + CONVENTION_DIRECT;
                                Util.logDebug("TA" + i + ": " + Util.formatByte(this.mRaw[pos] & MotionEventCompat.ACTION_MASK));
                            }
                            t15Block = true;
                        }
                        if (i >= CONVENTION_INVERSE && T <= 14) {
                            if ((this.mProtocols & (CONVENTION_DIRECT << T)) != 0) {
                                this.mProtocols |= CONVENTION_DIRECT << T;
                                this.mProtocolCnt += CONVENTION_DIRECT;
                            }
                        }
                        i += CONVENTION_DIRECT;
                        pos = pos2;
                    }
                    pos = pos2;
                    tck = true;
                    if (i == CONVENTION_INVERSE) {
                        firstOffer = T;
                        if (ta) {
                            pos2 = pos + CONVENTION_DIRECT;
                            ta2 = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                            Util.logDebug("TA2: " + Util.formatByte(ta2));
                            pos = pos2;
                        }
                        if (tb) {
                            pos2 = pos + CONVENTION_DIRECT;
                            Util.logDebug("TB2: " + Util.formatByte(this.mRaw[pos] & MotionEventCompat.ACTION_MASK));
                            pos = pos2;
                        }
                        if (tc) {
                            pos2 = pos + CONVENTION_DIRECT;
                            this.mT0_WI = (byte) (this.mRaw[pos] & MotionEventCompat.ACTION_MASK);
                            Util.logDebug("TC2: T0 WI = " + Util.formatByte(this.mT0_WI));
                            pos = pos2;
                        }
                    }
                    if (t1Block) {
                        if (ta) {
                            pos2 = pos + CONVENTION_DIRECT;
                            this.mT1_IFSC = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                            Util.logDebug("TA" + i + ": T1 IFSC = " + Util.formatByte(this.mT1_IFSC));
                            pos = pos2;
                        }
                        if (tb) {
                            pos2 = pos + CONVENTION_DIRECT;
                            tbi = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                            this.mT1_CWI = (byte) (tbi & 15);
                            this.mT1_BWI = (byte) (tbi >> 4);
                            Util.logDebug("TB" + i + ": T1 CWI = " + Util.formatByte(this.mT1_CWI) + ", T1 BWI = " + Util.formatByte(this.mT1_BWI));
                            pos = pos2;
                        }
                        if (tc) {
                            pos2 = pos + CONVENTION_DIRECT;
                            tci = this.mRaw[pos] & MotionEventCompat.ACTION_MASK;
                            if ((tci & CONVENTION_DIRECT) != CONVENTION_DIRECT) {
                            }
                            this.mT1_CRC = (tci & CONVENTION_DIRECT) != CONVENTION_DIRECT;
                            Util.logDebug("TC" + i + ": T1 CRC = " + Util.formatByte(tci & CONVENTION_DIRECT));
                            t1Block = true;
                            pos = pos2;
                            if (i > CONVENTION_INVERSE) {
                            }
                            pos2 = pos;
                            if ((this.mProtocols & (CONVENTION_DIRECT << T)) != 0) {
                                this.mProtocols |= CONVENTION_DIRECT << T;
                                this.mProtocolCnt += CONVENTION_DIRECT;
                            }
                            i += CONVENTION_DIRECT;
                            pos = pos2;
                        }
                    }
                    pos2 = pos;
                    t1Block = true;
                    pos = pos2;
                    if (i > CONVENTION_INVERSE) {
                    }
                    pos2 = pos;
                    if ((this.mProtocols & (CONVENTION_DIRECT << T)) != 0) {
                        this.mProtocols |= CONVENTION_DIRECT << T;
                        this.mProtocolCnt += CONVENTION_DIRECT;
                    }
                    i += CONVENTION_DIRECT;
                    pos = pos2;
                }
                Util.logDebug("parsing of interface characters done");
                Util.logDebug("protocols: " + Util.formatByte(this.mProtocols) + " (" + this.mProtocolCnt + ")");
                if (this.mProtocolCnt == 0) {
                    this.mProtocols |= CONVENTION_DIRECT;
                    this.mProtocolCnt = CONVENTION_DIRECT;
                    Util.logDebug("no further protocols given, adding T=0");
                }
                if (ta2 != -1) {
                    this.mProtocol = (byte) (ta2 & 15);
                } else {
                    this.mProtocol = (byte) firstOffer;
                }
                this.mF = Fi[this.mFI - 1];
                this.mD = Di[this.mDI - 1];
                Util.logDebug("using protocol T=" + this.mProtocol);
                // TODO this one enters infinite loop. FUCK
                if (tck) {
                    byte verify = (byte) 0;
                    i = CONVENTION_DIRECT;
                    while (true) {
                        length = this.mRaw.length;
                        if (i < length) {
                            verify = (byte) (this.mRaw[i] ^ verify);
                            i += CONVENTION_DIRECT;
                        } else if (verify != 0) {
                            Util.logError("TCK verification failed: " + Util.formatByte(verify));
                            return false;
                        } else
                            break;
                    }
                }
                return true;
            }
        }
        Util.logError("wrong ATR length: " + this.mRaw.length);
        return false;
    }

    public boolean supportsProtocol(int protocol) {
        return (this.mProtocols & protocol) != 0;
    }
}
