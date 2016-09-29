package freelancerwatermellon.serbianeidomnikey.eid;

import android.smartcardio.Card;
import android.smartcardio.CardException;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import freelancerwatermellon.serbianeidomnikey.MyException;
import freelancerwatermellon.serbianeidomnikey.utils.Utils;

public class EidCardApollo extends EidCard {

    /**
     * The list of known card ATRs, used to identify this smartcard.
     */
    public static final byte[] CARD_ATR = {
            (byte) 0x3B, (byte) 0xB9, (byte) 0x18, (byte) 0x00, (byte) 0x81, (byte) 0x31, (byte) 0xFE,
            (byte) 0x9E, (byte) 0x80, (byte) 0x73, (byte) 0xFF, (byte) 0x61, (byte) 0x40, (byte) 0x83,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xDF
    };
    /**
     * Intermediate CA gradjani public X.509 certificate
     */
    protected static final byte[] INTERM_CERT_FILE = {0x0F, 0x11};
    /**
     * Public X.509 certificate for qualified (Non Repudiation) signing
     */
    protected static final byte[] SIGNING_CERT_FILE = {0x0F, 0x10};
    /**
     * Public X.509 certificate for authentication
     */
    protected static final byte[] AUTH_CERT_FILE = {0x0F, 0x08};

    protected EidCardApollo(Card card) {
        super(card);
    }

    /**
     * Factory "selection" method
     */
    protected static boolean isKnownAtr(byte[] atrBytes) {
        return Arrays.equals(atrBytes, CARD_ATR);
    }

    @Override
    protected byte[] readElementaryFile(final byte[] name, boolean strip_tag) throws CardException, MyException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        selectFile(name);

        // Read header bytes from the EF
        byte[] header = readBinary(0, 6);

        // Empty files are filled with 0xFF
        if (Utils.allEquals(0xFF, header))
            return out.toByteArray();

        // Total EF length: data as 16bit LE at 4B offset
        int length = ((0xFF & header[5]) << 8) + (0xFF & header[4]);
        int offset = 6;
        if (strip_tag) {
            length -= 4;
            offset += 4;
        }

        // Read binary into buffer
        while (length > 0) {
            byte[] data = readBinary(offset, length);
            out.write(data, 0, data.length);
            offset += data.length;
            length -= data.length;
        }
        return out.toByteArray();
    }

    @Override
    protected void selectAid() throws CardException, MyException {
        return;
    }
}