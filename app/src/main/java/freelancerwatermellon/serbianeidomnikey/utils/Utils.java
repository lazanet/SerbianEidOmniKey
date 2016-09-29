package freelancerwatermellon.serbianeidomnikey.utils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

public class Utils {

	private Utils() {
	}

	/**
	 * Formats an integer as a hex string of little-endian, non-zero-padded
	 * bytes.
	 * 
	 * @param i
	 *            the integer to format
	 * @return the formatted string representing the integer
	 */
	public static String int2HexString(final int i) {
		return bytes2HexString(asByteArray(i >>> 24, i >>> 16, i >>> 8, i));
	}

	private static byte asByte(int value) {
		return (byte) (value & 0xFF);
	}

	public static byte[] asByteArray(int... values) {
		byte[] valueBytes = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			valueBytes[i] = asByte(values[i]);
		}
		return valueBytes;
	}

	public static int bytes2IntLE(byte... bytes) {
		int ret = 0;

		if (bytes.length > 3)
			ret += (bytes[bytes.length - 4] & 0xFF) << 24;
		if (bytes.length > 2)
			ret += (bytes[bytes.length - 3] & 0xFF) << 16;
		if (bytes.length > 1)
			ret += (bytes[bytes.length - 2] & 0xFF) << 8;
		if (bytes.length > 0)
			ret += (bytes[bytes.length - 1] & 0xFF) << 0;

		return ret;
	}

	/**
	 * Formats an array of bytes as a string.
	 * <p>
	 * Silently skips leading zero bytes.
	 * <p>
	 * 
	 * @param bytes
	 *            the bytes to print
	 * @return formatted byte string, e.g. an array of 0x00, 0x01, 0x02, 0x03
	 *         gets printed as: "01:02:03"
	 */
	public static String bytes2HexString(byte... bytes) {
		return bytes2HexStringWithSeparator(":", bytes);
	}

	public static String bytes2HexStringCompact(byte... bytes) {
		return bytes2HexStringWithSeparator("", bytes);
	}

	private static String bytes2HexStringWithSeparator(String separator,
													   byte... bytes) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for (byte b : bytes) {
			if (first && b == 0x00) {
				continue;
			}
			if (!first) {
				sb.append(separator);
			}
			sb.append(String.format("%02X", b));
			first = false;
		}
		return sb.toString();
	}

	/**
	 * Interprets an array of bytes as an string in a given charset.
	 * 
	 * @param charsetName
	 *            Charset name known to Java String class (UTF-8,
	 *            ISO-8859-1,...).
	 * @param bytes
	 *            the bytes to convert to string, {@code null} allowed.
	 */
	public static String bytes2String(String charsetName, byte... bytes) {
		try {
			return new String(bytes, charsetName);
		} catch (UnsupportedEncodingException ex) {
			return null;
		}
	}

	/**
	 * Interprets an array of bytes as an UTF-8 string.
	 * <p>
	 * Failing that, interprets as ISO-8859-1 or return a hex string.
	 * 
	 * @param bytes
	 *            the bytes to convert to string, {@code null} allowed.
	 */
	public static String bytes2UTF8String(byte... bytes) {
		if (bytes == null) {
			return "";
		}
		String ret;
		if ((ret = bytes2String("UTF-8", bytes)) != null) {
			return ret;
		} else if ((ret = bytes2String("ISO-8859-1", bytes)) != null) {
			return ret;
		} else {
			return bytes2HexString(bytes);
		}
	}

	public static byte[] getTlvLengthBytesAtOffset(byte[] data, int bytesFrom) {
		int lenByteSize = 1;
		if ((data[bytesFrom] & 0x80) == 0x80) {
			lenByteSize += (0x7F & data[bytesFrom]);
		}
		if (lenByteSize > 1) {
			bytesFrom++;
			lenByteSize -= 1;
		}
		return Arrays.copyOfRange(data, bytesFrom, bytesFrom + lenByteSize);
	}

	// yyyymmdd
	public static String stringToDate(String s) {
		if (s == null)
			return "";
		if (s.length() == 8) {
			String day = s.substring(6, 8);
			String month = s.substring(4, 6);
			String year = s.substring(0, 4);
			return day + "." + month + "." + year + ".";
		} else {
			return s;
		}
	}

	public static boolean allEquals(int needle, byte... bytes) {
		int i = 0;
		while (i < bytes.length && bytes[i] == needle)
			i++;
		return i == bytes.length;
	}

	/**
	 * Formats a map of objects to byte arrays for printing.
	 * <p>
	 * Each map entry is written out as a string line, map key first, then an
	 * equals sign, then an UTF-8 string interpreted from the bytes, then the
	 * recovered bytes written out as strings.
	 * <p>
	 * The map keys are sorted by the natural order of the key type.
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * 42 = Hello World ()
	 * 
	 * <pre>
	 * 
	 * @param <T> a comparable key type (comparable for sorting)
	 * @param map the map to format
	 * @return the formatted string, one line each.
	 */
	public static <T extends Comparable<T>> String map2UTF8String(
			Map<T, byte[]> map) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<?, byte[]> entry : map.entrySet()) {
			byte[] value = entry.getValue();
			builder.append(String.format("%d = %s (%s)\n", entry.getKey(),
					bytes2UTF8String(value), bytes2HexString(value)));
		}
		return builder.toString();
	}
}
