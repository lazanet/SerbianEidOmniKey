package freelancerwatermellon.serbianeidomnikey.eid;

import android.smartcardio.Card;
import android.smartcardio.CardException;
import android.smartcardio.CommandAPDU;
import android.smartcardio.ResponseAPDU;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import freelancerwatermellon.serbianeidomnikey.MyException;
import freelancerwatermellon.serbianeidomnikey.utils.Utils;

public class EidCardGemalto extends EidCard {

    /**
     * The list of known card ATRs, used to identify this smartcard.
     */
    public static final byte[] CARD_ATR = {
            (byte) 0x3B, (byte) 0xFF, (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x81, (byte) 0x31,
            (byte) 0x80, (byte) 0x43, (byte) 0x80, (byte) 0x31, (byte) 0x80, (byte) 0x65, (byte) 0xB0,
            (byte) 0x85, (byte) 0x02, (byte) 0x01, (byte) 0xF3, (byte) 0x12, (byte) 0x0F, (byte) 0xFF,
            (byte) 0x82, (byte) 0x90, (byte) 0x00, (byte) 0x79
    };
    static final byte[] SERBIAN_EID_AID = {
            (byte) 0xF3, (byte) 0x81, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x53, (byte) 0x45,
            (byte) 0x52, (byte) 0x49, (byte) 0x44, (byte) 0x01
    };

    protected EidCardGemalto(Card card) throws CardException, MyException {
        super(card);
        // Select aid
        selectAid();
    }

    /**
     * Factory "selection" method
     */
    protected static boolean isKnownAtr(byte[] atrBytes) {
        return Arrays.equals(atrBytes, CARD_ATR);
    }

    protected byte[] readElementaryFile(final byte[] name, boolean strip_tag) throws CardException, MyException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] fileinfo = selectFile(name, 4);

        // length hint from file info
        int length = ((0xFF & fileinfo[2]) << 8) + (0xFF & fileinfo[3]);
        int offset = 0;
        boolean known_real_length = false;

        while (length > 0) {
            byte[] data = readBinary(offset, length);

            if (!known_real_length) {
                // get length from outher tag, skip first 4 bytes (outher tag + length)
                // if strip_tag is true, skip 4 more bytes (inner tag + length) and return content
                length = ((0xFF & data[3]) << 8) + (0xFF & data[2]);
                int skip = strip_tag ? 8 : 4;
                out.write(data, skip, data.length - skip);
                if (strip_tag)
                    length += 4;
                known_real_length = true;
            } else out.write(data, 0, data.length);

            offset += data.length;
            length -= data.length;
        }
        return out.toByteArray();
    }

    @Override
    protected void selectAid() throws CardException, MyException {
        ResponseAPDU response = mChannel.transmit(
                new CommandAPDU(0x00, 0xA4, 0x04, 0x00, SERBIAN_EID_AID));
        if (response.getSW() != 0x9000) {
            throw new CardException(
                    String.format("Select AID failed: status=%s",
                            Utils.int2HexString(response.getSW())));
        }
    }
}