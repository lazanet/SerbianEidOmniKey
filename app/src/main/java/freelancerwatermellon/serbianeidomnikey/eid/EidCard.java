package freelancerwatermellon.serbianeidomnikey.eid;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.smartcardio.ATR;
import android.smartcardio.Card;
import android.smartcardio.CardChannel;
import android.smartcardio.CardException;
import android.smartcardio.CommandAPDU;
import android.smartcardio.ResponseAPDU;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import freelancerwatermellon.serbianeidomnikey.MyException;
import freelancerwatermellon.serbianeidomnikey.eid.EidInfo.Tag;
import freelancerwatermellon.serbianeidomnikey.utils.Utils;

@SuppressLint({"UseSparseArrays", "DefaultLocale"})
public abstract class EidCard {

    /**
     * Document data
     */
    protected static final byte[] DOCUMENT_FILE = {0x0F, 0x02};
    /**
     * Personal data
     */
    protected static final byte[] PERSONAL_FILE = {0x0F, 0x03};
    /**
     * Place of residence
     */
    protected static final byte[] RESIDENCE_FILE = {0x0F, 0x04};


    // private CardChannel channel;
    /**
     * Personal photo in JPEG format
     */
    protected static final byte[] PHOTO_FILE = {0x0F, 0x06};
    // tags: 1545 - 1553
    static final Map<Integer, Tag> DOCUMENT_TAGMAPPER = new HashMap<Integer, Tag>() {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        {
            put(1545, Tag.NULL); // = SRB (issuing authority country code?)
            put(1546, Tag.DOC_REG_NO);
            put(1547, Tag.NULL); // = ID
            put(1548, Tag.NULL); // = ID<docRegNo>
            put(1549, Tag.ISSUING_DATE);
            put(1550, Tag.EXPIRY_DATE);
            put(1551, Tag.ISSUING_AUTHORITY);
            put(1552, Tag.NULL); // = SC
            put(1553, Tag.NULL); // = SC
        }
    };
    // tags: 1558 - 1567
    static final Map<Integer, Tag> PERSONAL_TAGMAPPER = new HashMap<Integer, Tag>() {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        {
            put(1558, Tag.PERSONAL_NUMBER);
            put(1559, Tag.SURNAME);
            put(1560, Tag.GIVEN_NAME);
            put(1561, Tag.PARENT_GIVEN_NAME);
            put(1562, Tag.SEX);
            put(1563, Tag.PLACE_OF_BIRTH);
            put(1564, Tag.COMMUNITY_OF_BIRTH);
            put(1565, Tag.STATE_OF_BIRTH);
            put(1566, Tag.DATE_OF_BIRTH);
            put(1567, Tag.NULL); // = SRB (state of birth country code?)
        }
    };

    // elementary file names
    // tags: 1568 .. 1578
    static final Map<Integer, Tag> RESIDENCE_TAGMAPPER = new HashMap<Integer, Tag>() {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        {
            put(1568, Tag.STATE);
            put(1569, Tag.COMMUNITY);
            put(1570, Tag.PLACE);
            put(1571, Tag.STREET);
            put(1572, Tag.HOUSE_NUMBER);
            put(1573, Tag.HOUSE_LETTER);
            put(1574, Tag.ENTRANCE);
            put(1575, Tag.FLOOR);
            put(1578, Tag.APPARTMENT_NUMBER);
            put(1580, Tag.ADDRESS_DATE); // = default 01010001
            // AddressLabel ?
        }
    };
    private static final int BLOCK_SIZE = 0xFB;
    protected Card mCard;
    protected CardChannel mChannel;
    String log_str = "";

    public EidCard(final Card card) throws IllegalArgumentException,
            SecurityException, IllegalStateException {

        this.mCard = card;
        this.mChannel = card.getBasicChannel();

        // channel = card.getBasicChannel();
    }

    public static EidCard fromCard(Card card) throws CardException,
            MyException {

        ATR atrBytes = card.getATR();
        //card.disconnect(true);
        if (EidCardApollo.isKnownAtr(atrBytes.getBytes()))
            return new EidCardApollo(card);

        if (EidCardGemalto.isKnownAtr(atrBytes.getBytes()))
            return new EidCardGemalto(card);

        throw new IllegalArgumentException(String.format(
                "EidCard: Card is not recognized as Serbian eID. Card ATR: %s",
                Utils.bytes2HexString(atrBytes.getBytes())));
    }

    /**
     * Factory "selection" method
     */
    protected static boolean isKnownAtr(final byte[] atrBytes) {
        return false;
    }

    /**
     * Subdivides the byte array into byte sub-arrays, keyed by their tags
     * <p>
     * Encoding sequence is a repeated sequence of the following.
     * <ol>
     * <li>The tag, encoded as little-endian unsigned 16-bit number (just like
     * char in Java)
     * <li>The length of data, in bytes, as unsigned little-endian 16-bit number
     * <li>The data bytes, as many as determined by length.
     * </ol>
     * [tag 16bit LE] [len 16bit LE] [len bytes of data] | [fld] [06] ...
     *
     * @return a map of integer tags to corresponding byte chunks.
     */
    public static Map<Integer, byte[]> parseTlv(final byte[] bytes) {
        HashMap<Integer, byte[]> out = new HashMap<Integer, byte[]>();

        // [fld 16bit LE] [len 16bit LE] [len bytes of data] | [fld] [06] ...

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(
                ByteOrder.LITTLE_ENDIAN);

        // repeat as long as we have next tag and len...
        while (buffer.remaining() > 4) {
            char tag = buffer.getChar();
            char length = buffer.getChar();
            byte[] range = new byte[(int) length];
            buffer.get(range);
            out.put((int) tag, range);
        }

        return out;
    }

    protected byte[] mergeByteArrays(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Selects the elementary file to read, based on the name passed in.
     *
     * @throws CardException
     * @throws MyException
     */

    /**
     * Read EF contents, selecting by file path.
     * <p>
     * The file length is read at 4B offset from the file. The method is not
     * thread safe. Exclusive card access should be enabled before calling the
     * method.
     * <p>
     * TODO: Refactor to be in line with ISO7816-4 and BER-TLV, removing "magic"
     * headers
     *
     * @throws CardException
     * @throws MyException
     */
    abstract protected byte[] readElementaryFile(byte[] name, boolean strip_tag)
            throws MyException, CardException;

    /**
     * Reads the content of the selected file starting at offset, at most length
     * bytes
     *
     * @throws CardException
     * @throws MyException
     */
    protected byte[] readBinary(int offset, int length) throws CardException,
            MyException {
        int readSize = Math.min(length, BLOCK_SIZE);
        ResponseAPDU response = mChannel.transmit(
                new CommandAPDU(0x00, 0xB0, offset >> 8, offset & 0xFF, readSize));
        log_str += String.format("Read:size=%d offset=%d length=%d bytes=%s", response.getBytes().length, offset, length, Utils.bytes2HexString(response.getBytes()));
        if (response.getSW() != 0x9000) {
            throw new CardException(
                    log_str += String.format("Read binary failed:size=%d, offset=%d, length=%d, status=%s",
                            response.getBytes().length, offset, length, Utils.int2HexString(response.getSW())));
        }

        return response.getData();
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        while (length > 0) {
//            int readSize = Math.min(length, BLOCK_SIZE);
//            ResponseAPDU response = mChannel.transmit(
//                    new CommandAPDU(0x00, 0xB0, offset >> 8, offset & 0xFF, readSize));
//            log_str += String.format("offset=%d,len=%d,bytes=%s\n",offset, length, Utils.bytes2HexString(response.getBytes()));
//            if (response.getSW() != 0x9000) {
//                throw new CardException(
//                        log_str += String.format("Read binary failed:size=%d, offset=%d, length=%d, status=%s",
//                                response.getBytes().length,offset, length, Utils.int2HexString(response.getSW())));
//            }
//            try {
//                out.write(response.getData());
//                offset += response.getData().length;
//                length -= response.getData().length;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return out.toByteArray();
    }

    abstract protected void selectAid() throws CardException, MyException;

    /**
     * Selects the elementary file to read, based on the name passed in.
     */
    protected byte[] selectFile(final byte[] name) throws CardException {
        return selectFile(name, 0);
    }

    protected byte[] selectFile(final byte[] name, int ne) throws CardException {
        ResponseAPDU response = mChannel.transmit(new CommandAPDU(0x00, 0xA4, 0x08, 0x00, name, ne));
        if (response.getSW() != 0x9000) {
            throw new CardException(
                    String.format("Select failed: name=%s, status=%s",
                            Utils.bytes2HexString(name), Utils.int2HexString(response.getSW())));
        }
        return response.getData();
    }

    /**
     * Reads the photo data from the card.
     *
     * @throws MyException
     * @throws CardException
     */
    public Bitmap readEidPhoto() throws CardException, MyException {
        try {
            // Read binary into buffer
            mCard.beginExclusive();
            // mChannel = mCard.getBasicChannel();

            byte[] bytes = readElementaryFile(PHOTO_FILE, true);

            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } finally {
            mCard.endExclusive();
        }
    }

    /**
     * Add all raw tags to EidInfo builder.
     *
     * @param builder   EidInfo builder
     * @param rawTagMap Parsed map of raw byte strings by TLV code
     * @param tagMapper Map translating Tag codes into EidInfo tags; use
     *                  {@code Tag.NULL} if tag should be silently ignored
     * @return Raw map of unknown tags
     */
    protected Map<Integer, byte[]> addAllToBuilder(EidInfo.Builder builder,
                                                   final Map<Integer, byte[]> rawTagMap,
                                                   final Map<Integer, Tag> tagMapper) {

        Map<Integer, byte[]> unknownTags = new HashMap<Integer, byte[]>();

        for (Map.Entry<Integer, byte[]> entry : rawTagMap.entrySet()) {
            if (tagMapper.containsKey(entry.getKey())) {
                // tag is known, ignore if null or decode and add value to the
                // builder
                Tag tag = tagMapper.get(entry.getKey());
                if (tag == Tag.NULL) {
                    continue;
                }

                String value = Utils.bytes2UTF8String(entry.getValue());
                builder.addValue(tag, value);

            } else if (entry.getValue().length > 0) {
                // tag is unknown, copy for return
                unknownTags.put(entry.getKey(), entry.getValue());
            }
        }

        return unknownTags;
    }

    public EidInfo readEidInfo() throws CardException, MyException {
        try {
            mCard.beginExclusive();
            mChannel = mCard.getBasicChannel();
            Map<Integer, byte[]> document = parseTlv(readElementaryFile(
                    DOCUMENT_FILE, false));
            Map<Integer, byte[]> personal = parseTlv(readElementaryFile(
                    PERSONAL_FILE, false));
            Map<Integer, byte[]> residence = parseTlv(readElementaryFile(
                    RESIDENCE_FILE, false));

            EidInfo.Builder builder = new EidInfo.Builder();
            document = addAllToBuilder(builder, document, DOCUMENT_TAGMAPPER);
            personal = addAllToBuilder(builder, personal, PERSONAL_TAGMAPPER);
            residence = addAllToBuilder(builder, residence, RESIDENCE_TAGMAPPER);

            // log all unknown tags so all users can report bugs easily
            StringBuilder unknownString = new StringBuilder();
            if (!document.isEmpty()) {
                unknownString.append("DOCUMENT:\n"
                        + Utils.map2UTF8String(document));
            }
            if (!personal.isEmpty()) {
                unknownString.append("PERSONAL:\n"
                        + Utils.map2UTF8String(personal));
            }
            if (!residence.isEmpty()) {
                unknownString.append("RESIDENCE:\n"
                        + Utils.map2UTF8String(residence));
            }
            if (unknownString.length() > 0) {
                // logger.error(
                // "Some unknown tags found on a card. Please send this info to "
                // +
                // "<grakic@devbase.net> and contribute to the development.\n" +
                // unknownString.toString());
            }

            return builder.build();

        } finally {
            mCard.endExclusive();
        }
    }

    /**
     * Converts the HEX string to byte array.
     *
     * @param hexString the HEX string.
     * @return the byte array.
     */
    private byte[] toByteArray(String hexString) {

        int hexStringLength = hexString.length();
        byte[] byteArray = null;
        int count = 0;
        char c;
        int i;

        // Count number of hex characters
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f') {
                count++;
            }
        }

        byteArray = new byte[(count + 1) / 2];
        boolean first = true;
        int len = 0;
        int value;
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                value = c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                value = c - 'a' + 10;
            } else {
                value = -1;
            }

            if (value >= 0) {

                if (first) {

                    byteArray[len] = (byte) (value << 4);

                } else {

                    byteArray[len] |= value;
                    len++;
                }

                first = !first;
            }
        }

        return byteArray;
    }

    /**
     * Converts the integer to HEX string.
     *
     * @param i the integer.
     * @return the HEX string.
     */
    private String toHexString(int i) {

        String hexString = Integer.toHexString(i);
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }

        return hexString.toUpperCase(Locale.US);
    }

    /**
     * Converts the byte array to HEX string.
     *
     * @param buffer the buffer.
     * @return the HEX string.
     */
    private String toHexString(byte[] buffer) {

        String bufferString = "";

        for (int i = 0; i < buffer.length; i++) {

            String hexChar = Integer.toHexString(buffer[i] & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = "0" + hexChar;
            }

            bufferString += hexChar.toUpperCase(Locale.US) + " ";
        }

        return bufferString;
    }

    private boolean answerSucess(byte[] sw, int len) {
        if (len >= 2) {
            byte sw1 = sw[len - 2];
            byte sw2 = sw[len - 1];
            if ((sw1 == (byte) 0x90 && sw2 == (byte) 0x00))
                return true;
        }
        return false;
    }

}
