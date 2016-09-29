package freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid;

import java.util.ArrayList;

import freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util;

public class T0Handler {
    private static final int INS_ENVELOPE = 194;
    private static final int INS_GET_RESPONSE = 192;
    private byte mCLA;
    private byte[] mCommand;
    private int mCommandCase;
    private ArrayList<byte[]> mFollowupCommands;
    private byte mINS;
    private int mLa;
    private int mLc;
    private int mLe;
    private int mLm;
    private int mLx;
    private byte mP1;
    private byte mP2;
    private byte[] mResponse;
    private byte[] mResponseData;
    private int mResponseDataPos;
    private byte mSW1;
    private byte mSW2;

    public T0Handler(PduAnalyzer pduAnalyzer) {
        this.mCommand = null;
        this.mCommandCase = 0;
        this.mFollowupCommands = null;
        this.mResponse = null;
        this.mResponseData = null;
        this.mResponseDataPos = 0;
        this.mCLA = (byte) 0;
        this.mINS = (byte) 0;
        this.mP1 = (byte) 0;
        this.mP2 = (byte) 0;
        this.mLe = 0;
        this.mLc = 0;
        this.mSW1 = (byte) 0;
        this.mSW2 = (byte) 0;
        this.mLa = 0;
        this.mLm = 0;
        this.mLx = 0;
        byte[] command = pduAnalyzer.getCommand();
        this.mCommand = new byte[command.length];
        System.arraycopy(command, 0, this.mCommand, 0, command.length);
        this.mCLA = command[0];
        this.mINS = command[1];
        this.mP1 = command[2];
        this.mP2 = command[3];
        this.mLe = pduAnalyzer.getLe();
        this.mLc = pduAnalyzer.getLc();
        this.mCommandCase = pduAnalyzer.getCommandCase();
        this.mFollowupCommands = new ArrayList();
    }

    public boolean putResponse(byte[] response) {
        if (response == null) {
            Util.logError("given response == null");
            return false;
        } else if (response.length < 2) {
            Util.logError("given response < 2 bytes");
            return false;
        } else {
            this.mSW1 = response[response.length - 2];
            this.mSW2 = response[response.length - 1];
            this.mLa = 0;
            this.mLx = 0;
            this.mResponse = new byte[response.length];
            System.arraycopy(response, 0, this.mResponse, 0, response.length);
            Util.logDebug("received " + response.length + " bytes");
            Util.logDebug(Util.byteArrayToString(this.mResponse));
            return true;
        }
    }

    public byte[] getCommand(boolean asEscapeCommand) {
        Util.logDebug("returning TPDU to be sent via " + (asEscapeCommand ? "PC_to_RDR_Escape" : "PC_to_RDR_XfrBlock"));
        byte[] command = getCommand();
        if (!asEscapeCommand) {
            return command;
        }
        byte[] escapeCommand = new byte[(command.length + 5)];
        escapeCommand[0] = (byte) 27;
        if (this.mCommandCase == 1) {
            Util.logDebug("case 1 (escape)");
            escapeCommand[1] = (byte) 5;
            escapeCommand[2] = (byte) 0;
            escapeCommand[3] = (byte) 0;
            escapeCommand[4] = (byte) 0;
        }
        if (this.mCommandCase == 2) {
            Util.logDebug("case 2s (escape)");
            escapeCommand[1] = (byte) 5;
            escapeCommand[2] = (byte) 0;
            if (this.mLe == CcidDescriptor.FEAT_SET_ICC) {
                escapeCommand[3] = (byte) 0;
                escapeCommand[4] = (byte) 1;
            } else {
                escapeCommand[3] = (byte) this.mLe;
                escapeCommand[4] = (byte) 0;
            }
        }
        if (this.mCommandCase == 3) {
            Util.logDebug("case 3s (escape)");
            if (this.mLc == CcidDescriptor.FEAT_SET_ICC) {
                escapeCommand[1] = (byte) 0;
                escapeCommand[2] = (byte) 1;
            } else {
                escapeCommand[1] = (byte) (this.mLc + 5);
                escapeCommand[2] = (byte) 0;
            }
            escapeCommand[3] = (byte) 0;
            escapeCommand[4] = (byte) 0;
        }
        if (this.mCommandCase == 4) {
            Util.logDebug("case 4s (escape)");
            if (this.mLc == CcidDescriptor.FEAT_SET_ICC) {
                escapeCommand[1] = (byte) 0;
                escapeCommand[2] = (byte) 1;
            } else {
                escapeCommand[1] = (byte) (this.mLc + 5);
                escapeCommand[2] = (byte) 0;
            }
            if (this.mLe == CcidDescriptor.FEAT_SET_ICC) {
                escapeCommand[3] = (byte) 0;
                escapeCommand[4] = (byte) 1;
            } else {
                escapeCommand[3] = (byte) this.mLe;
                escapeCommand[4] = (byte) 0;
            }
        }
        if (this.mCommandCase >= 5) {
            Util.logError("command case " + this.mCommandCase + ", but shall be sent via PC_to_RDR_Escape");
            return null;
        }
        System.arraycopy(command, 0, escapeCommand, 5, command.length);
        return escapeCommand;
    }

    private byte[] getCommand() {
        byte[] asTPDU;
        if (this.mCommandCase == 1) {
            Util.logDebug("case 1");
            asTPDU = new byte[(this.mCommand.length + 1)];
            System.arraycopy(this.mCommand, 0, asTPDU, 0, this.mCommand.length);
            asTPDU[this.mCommand.length] = (byte) 0;
            return asTPDU;
        } else if (this.mCommandCase == 2 || this.mCommandCase == 3) {
            Util.logDebug("case 2s / 3s");
            asTPDU = new byte[this.mCommand.length];
            System.arraycopy(this.mCommand, 0, asTPDU, 0, this.mCommand.length);
            return asTPDU;
        } else if (this.mCommandCase == 4) {
            Util.logDebug("case 4s");
            asTPDU = new byte[(this.mCommand.length - 1)];
            System.arraycopy(this.mCommand, 0, asTPDU, 0, this.mCommand.length - 1);
            return asTPDU;
        } else if (this.mCommandCase == 5) {
            Util.logDebug("case 2e");
            asTPDU = new byte[5];
            System.arraycopy(this.mCommand, 0, asTPDU, 0, 4);
            if (this.mLe <= CcidDescriptor.FEAT_SET_ICC) {
                asTPDU[4] = (byte) (this.mLe & 255);
                this.mCommandCase = 2;
                Util.logDebug("actually case 2s");
                return asTPDU;
            }
            asTPDU[4] = (byte) 0;
            return asTPDU;
        } else if (this.mCommandCase == 6 && this.mLc < CcidDescriptor.FEAT_SET_ICC) {
            Util.logDebug("case 3e.1");
            asTPDU = new byte[(this.mLc + 5)];
            System.arraycopy(this.mCommand, 0, asTPDU, 0, 4);
            asTPDU[4] = (byte) (this.mLc & 255);
            System.arraycopy(this.mCommand, 7, asTPDU, 5, this.mLc);
            return asTPDU;
        } else if (this.mCommandCase == 7 && this.mLc < CcidDescriptor.FEAT_SET_ICC) {
            Util.logDebug("case 4e.1");
            asTPDU = new byte[(this.mLc + 5)];
            System.arraycopy(this.mCommand, 0, asTPDU, 0, 4);
            asTPDU[4] = (byte) (this.mLc & 255);
            System.arraycopy(this.mCommand, 7, asTPDU, 5, this.mLc);
            return asTPDU;
        } else if ((this.mCommandCase == 6 || this.mCommandCase == 7) && this.mLc > 255) {
            Util.logDebug("case 3e.2 / 4e.2");
            int pos = 0;
            while (pos < this.mLc) {
                int len = this.mLc - pos;
                if (len > 255) {
                    len = 255;
                }
                queueFollowupCommand(this.mCLA, (byte) -62, (byte) 0, (byte) 0, len, this.mCommand, pos);
                pos += len;
            }
            asTPDU = (byte[]) this.mFollowupCommands.get(0);
            this.mFollowupCommands.remove(0);
            return asTPDU;
        } else {
            Util.logError("unexpected command case " + this.mCommandCase);
            return null;
        }
    }

    public byte[] getNextCommand() {
        if (this.mFollowupCommands == null || this.mFollowupCommands.size() == 0) {
            return null;
        }
        byte[] c = (byte[]) this.mFollowupCommands.get(0);
        this.mFollowupCommands.remove(0);
        Util.logDebug(this.mFollowupCommands.size() + " commands left");
        return c;
    }

    public byte[] getResponse() {
        if (this.mCommandCase == 1 || this.mCommandCase == 3) {
            Util.logDebug("case 1 / case 3s");
            return this.mResponse;
        }
        if (this.mCommandCase == 2) {
            if (this.mResponse.length == this.mLe + 2) {
                Util.logDebug("case 2s.1 - Le accepted");
                return this.mResponse;
            } else if (this.mResponse.length == 2) {
                switch ((this.mSW1 >> 4) & 15) {
                    case 6 /*6*/:
                        if (this.mSW1 == 103) {
                            Util.logDebug("case 2S.2: Le definitely not accepted");
                            return this.mResponse;
                        } else if (this.mSW1 == 108) {
                            Util.logDebug("case 2s.3 - Le not accepted, La indicated");
                            return this.mResponse;
                        } else {
                            Util.logDebug("case 2S.1: mSW1 = " + Util.formatByte(this.mSW1));
                            return this.mResponse;
                        }
                    case 9 /*9*/:
                        return this.mResponse;
                    default:
                        return null;
                }
            } else if (this.mLa != 0) {
                if (this.mResponse.length != this.mLa + 2) {
                    this.mLa = 0;
                    Util.logError("case 2S.3, second response length (" + this.mResponse.length + ") != mLa+2 (" + (this.mLa + 2) + ")");
                    return null;
                }
                int len;
                Util.logDebug("case 2s.3, second response");
                if (this.mLa > this.mLe) {
                    len = this.mLe;
                } else {
                    len = this.mLa;
                }
                byte[] asAPDU = new byte[(len + 2)];
                System.arraycopy(this.mResponse, 0, asAPDU, 0, len);
                asAPDU[len] = this.mSW1;
                asAPDU[len + 1] = this.mSW2;
                this.mLa = 0;
                return asAPDU;
            }
        }
        if (this.mCommandCase == 4) {
            if (this.mResponse.length == 2) {
                switch ((this.mSW1 >> 4) & 15) {
                    case 6 /*6*/:
                        if (this.mSW1 != 97) {
                            Util.logDebug("case 4s.1 - command not accepted");
                            return this.mResponse;
                        }
                        Util.logDebug("case 4s.3");
                        this.mLx = this.mSW2 > this.mLe ? this.mLe : this.mSW2;
                        queueFollowupCommand(this.mCLA, (byte) -64, (byte) 0, (byte) 0, this.mLx);
                        return null;
                    case 9 /*9*/:
                        if (this.mSW1 == 144 || this.mSW2 == 0) {
                            Util.logDebug("case 4s.2 - command accepted");
                            queueFollowupCommand(this.mCLA, (byte) -64, (byte) 0, (byte) 0, this.mLe);
                            this.mCommandCase = 2;
                            return null;
                        }
                        Util.logDebug("case 4s.4");
                        return this.mResponse;
                }
            }
            if (this.mLx != 0) {
                if (this.mResponse.length != this.mLx + 2) {
                    this.mLx = 0;
                    Util.logError("case 4S.3, second response length (" + this.mResponse.length + ") != mLx+2 (" + (this.mLx + 2) + ")");
                    return null;
                }
                Util.logDebug("case 4s.3, second response");
                this.mLx = 0;
                return this.mResponse;
            }
        }
        if (this.mCommandCase == 5) {
            Util.logDebug("case 2e.2");
            switch ((this.mSW1 >> 4) & 15) {
                case 6 /*6*/:
                    if (this.mSW1 == 97) {
                        int i;
                        if (this.mResponseData == null) {
                            this.mResponseData = new byte[(this.mLe + 2)];
                            this.mResponseDataPos = 0;
                        }
                        System.arraycopy(this.mResponse, 0, this.mResponseData, this.mResponseDataPos, this.mResponse.length - 2);
                        this.mResponseDataPos += this.mResponse.length - 2;
                        this.mLm = this.mLe - this.mResponseDataPos;
                        if (this.mSW2 == 0) {
                            i = CcidDescriptor.FEAT_SET_ICC;
                        } else {
                            i = this.mSW2;
                        }
                        this.mLx = i;
                        if (this.mLm == 0) {
                            this.mResponseData[this.mLe] = (byte) (this.mSW1 & 255);
                            this.mResponseData[this.mLe + 1] = (byte) (this.mSW2 & 255);
                            return this.mResponseData;
                        } else if (this.mLm > 0) {
                            queueFollowupCommand(this.mCLA, (byte) -64, (byte) 0, (byte) 0, this.mLx > this.mLm ? this.mLm : this.mLx);
                            return null;
                        }
                    }
                    if (this.mSW1 == 108) {
                        queueFollowupCommand(this.mCLA, this.mINS, this.mP1, this.mP2, this.mSW2);
                        this.mLa = this.mSW2;
                        this.mCommandCase = 2;
                        return null;
                    }
                    Util.logError("case 2E.2: SW1 = " + Util.formatByte(this.mSW1));
                    return this.mResponse;
                case 9 /*9*/:
                    return this.mResponse;
            }
        }
        if (this.mCommandCase == 6) {
            if (this.mLc < CcidDescriptor.FEAT_SET_ICC) {
                Util.logDebug("case 3e.1");
                return this.mResponse;
            }
            Util.logDebug("case 3e.2");
            if (this.mSW1 == 109) {
                return this.mResponse;
            }
            if (this.mFollowupCommands.size() > 0) {
                if (this.mSW1 == 144 && this.mSW2 == 0) {
                    return null;
                }
                Util.logError("case 3E.2: SW1 = " + Util.formatByte(this.mSW1) + ", SW2 = " + Util.formatByte(this.mSW2));
                return this.mResponse;
            } else if (this.mLe <= 0) {
                return this.mResponse;
            } else {
                this.mLc = 0;
                this.mCommandCase = 7;
            }
        }
        if (this.mCommandCase == 7) {
            if (this.mLc < CcidDescriptor.FEAT_SET_ICC) {
                Util.logDebug("case 4e.1");
                switch ((this.mSW1 >> 4) & 15) {
                    case 6 /*6*/:
                        if (this.mSW1 != 97) {
                            return this.mResponse;
                        }
                        this.mCommandCase = 5;
                        return getResponse();
                    case 9 /*9*/:
                        if (this.mSW1 != 144) {
                            Util.logError("case 4E.1: mSW1 = " + Util.formatByte(this.mSW1));
                            return null;
                        } else if (this.mLe < 257) { //
                            queueFollowupCommand(this.mCLA, (byte) -64, (byte) 0, (byte) 0, this.mLe & 255);
                            this.mCommandCase = 2;
                            return null;
                        } else {
                            queueFollowupCommand(this.mCLA, (byte) -64, (byte) 0, (byte) 0, 0);
                            this.mCommandCase = 5;
                            return null;
                        }
                }
            }
            Util.logDebug("case 4e.2");
            this.mCommandCase = 6;
            return getResponse();
        }
        Util.logError("case " + this.mCommandCase + ", but no match (SW1 = " + Util.formatByte(this.mSW1) + ", SW2 = " + Util.formatByte(this.mSW2) + ")");
        return null;
    }

    private void queueFollowupCommand(byte cla, byte ins, byte p1, byte p2, int lc, byte[] data, int dataStart) {
        byte[] command;
        if (data != null) {
            command = new byte[(data.length + 5)];
        } else {
            command = new byte[5];
        }
        command[0] = cla;
        command[1] = ins;
        command[2] = p1;
        command[3] = p2;
        command[4] = (byte) (lc & 255);
        if (data != null) {
            System.arraycopy(data, dataStart, command, 5, lc);
        }
        this.mFollowupCommands.add(command);
    }

    private void queueFollowupCommand(byte cla, byte ins, byte p1, byte p2, int le) {
        queueFollowupCommand(cla, ins, p1, p2, le, null, 0);
    }
}
