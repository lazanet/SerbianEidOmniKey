package freelancerwatermellon.serbianeidomnikey.cardreadermanager.ccid;

import freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util;

public class PduAnalyzer {
    public static final int COMMAND_CASE_1 = 1;
    public static final int COMMAND_CASE_2_EXTENDED = 5;
    public static final int COMMAND_CASE_2_SHORT = 2;
    public static final int COMMAND_CASE_3_EXTENDED = 6;
    public static final int COMMAND_CASE_3_SHORT = 3;
    public static final int COMMAND_CASE_4_EXTENDED = 7;
    public static final int COMMAND_CASE_4_SHORT = 4;
    private static final int CCID_HEADER_LENGTH = 10;
    private static final int LE_EXTENDED_MAX = 65536;
    private static final int LE_SHORT_MAX = 256;
    private static final int PDU_HEADER_LENGTH = 10;
    private byte[] mCommand;
    private int mCommandCase;
    private int mLc;
    private int mLe;

    public PduAnalyzer() {
        this.mCommandCase = 0;
        this.mCommand = null;
        this.mLe = 0;
        this.mLc = 0;
    }

    public boolean putCommand(byte[] command) {
        if (command == null) {
            Util.logError("given command == null");
            return false;
        } else if (command.length < COMMAND_CASE_4_SHORT) {
            Util.logError("given command < 4 bytes");
            return false;
        } else {
            int commandCase = commandCase(command);
            this.mCommandCase = commandCase;
            if (commandCase == 0) {
                Util.logError("cannot determine command case");
                return false;
            }
            this.mCommand = new byte[command.length];
            System.arraycopy(command, 0, this.mCommand, 0, command.length);
            return true;
        }
    }

    public byte[] getCommand() {
        return this.mCommand;
    }

    public int getCommandCase() {
        return this.mCommandCase;
    }

    public int getLe() {
        return this.mLe;
    }

    public int getLc() {
        return this.mLc;
    }

    public boolean extendedModeNecessary(int maxCCIDMessageLength) {
        Util.logDebug("Lc = " + this.mLc + ", Le = " + this.mLe + ", dwMaxCCIDMessageLength = " + maxCCIDMessageLength);
        if (this.mCommandCase <= COMMAND_CASE_4_SHORT) {
            Util.logError("TPDU mode NOT necessary");
            return false;
        }
        int maxDataLength = (maxCCIDMessageLength - 10) - 10;
        if (this.mLc > maxDataLength || this.mLe > maxDataLength) {
            Util.logError("extended mode necessary");
            return true;
        }
        Util.logError("extended mode NOT necessary");
        return false;
    }

    private int commandCase(byte[] command) {
        int commandCase;
        switch (command.length) {
            case 0 /*0*/:
            case COMMAND_CASE_1 /*1*/:
            case COMMAND_CASE_2_SHORT /*2*/:
            case COMMAND_CASE_3_SHORT /*3*/:
                Util.logError("unexpected command length " + Util.formatByte(command.length));
                return 0;
            case COMMAND_CASE_4_SHORT /*4*/:
                commandCase = COMMAND_CASE_1;
                break;
            case COMMAND_CASE_2_EXTENDED /*5*/:
                this.mLe = command[COMMAND_CASE_4_SHORT] & 0xff;
                if (this.mLe == 0) {
                    this.mLe = LE_SHORT_MAX;
                }
                commandCase = COMMAND_CASE_2_SHORT;
                break;
            default:
                if (command[COMMAND_CASE_4_SHORT] != 0) {
                    this.mLc = command[COMMAND_CASE_4_SHORT] & 0xff;
                    if (command.length == this.mLc + COMMAND_CASE_2_EXTENDED) {
                        commandCase = COMMAND_CASE_3_SHORT;
                        break;
                    } else if (command.length == (this.mLc + COMMAND_CASE_2_EXTENDED) + COMMAND_CASE_1) {
                        this.mLe = command[command.length - 1] & 0xff;
                        if (this.mLe == 0) {
                            this.mLe = LE_SHORT_MAX;
                        }
                        commandCase = COMMAND_CASE_4_SHORT;
                        break;
                    } else {
                        Util.logError("unexpected command length " + Util.formatByte(command.length));
                        return 0;
                    }
                } else if (command.length == COMMAND_CASE_4_EXTENDED) {
                    this.mLe = ((command[COMMAND_CASE_2_EXTENDED] << 8) & 0xff00) | (command[COMMAND_CASE_3_EXTENDED] & 0xff);
                    if (this.mLe == 0) {
                        this.mLe = LE_EXTENDED_MAX;
                    }
                    commandCase = COMMAND_CASE_2_EXTENDED;
                    break;
                } else if (command.length > COMMAND_CASE_4_EXTENDED) {
                    this.mLc = ((command[COMMAND_CASE_2_EXTENDED] << 8) & 0xff00) | (command[COMMAND_CASE_3_EXTENDED] & 0xff);
                    if (command.length == this.mLc + COMMAND_CASE_4_EXTENDED) {
                        commandCase = COMMAND_CASE_3_EXTENDED;
                        break;
                    } else if (command.length == (this.mLc + COMMAND_CASE_4_EXTENDED) + COMMAND_CASE_2_SHORT) {
                        this.mLe = ((command[command.length - 2] << 8) & 0xff00) | (command[command.length - 1] & 0xff);
                        if (this.mLe == 0) {
                            this.mLe = LE_EXTENDED_MAX;
                        }
                        commandCase = COMMAND_CASE_4_EXTENDED;
                        break;
                    } else {
                        Util.logError("unexpected command length " + Util.formatByte(command.length));
                        return 0;
                    }
                } else {
                    Util.logError("unexpected command length " + Util.formatByte(command.length));
                    return 0;
                }
        }
        Util.logDebug("decoded command, ISO7816-4/A: case " + commandCase);
        return commandCase;
    }
}
