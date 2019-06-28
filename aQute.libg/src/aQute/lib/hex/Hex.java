package aQute.lib.hex;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

/*
 * Hex converter.
 *
 * TODO Implement string to byte[]
 */
public class Hex {
	private final static Pattern	HEX_P	= Pattern.compile("(?:\\p{XDigit}{2})+");

	final static char[]				HEX		= {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};

	public final static byte[] toByteArray(String string) {
		Objects.requireNonNull(string, "The hex string must not be null.");
		string = string.trim();
		if ((string.length() & 1) != 0)
			throw new IllegalArgumentException("a hex string must have an even length");

		byte[] out = new byte[string.length() / 2];
		for (int i = 0; i < out.length; i++) {
			int high = nibble(string.charAt(i * 2)) << 4;
			int low = nibble(string.charAt(i * 2 + 1));
			out[i] = (byte) (high + low);
		}
		return out;
	}

	public static String toHex(byte b) {
		char low = HEX[b & 0xF];
		char high = HEX[(b & 0xF0) >> 4];
		return new String(new char[] {
			high, low
		});
	}

	public final static int nibble(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';

		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;

		throw new IllegalArgumentException("Not a hex digit: " + c);
	}

	public final static String toHexString(byte data[]) {
		StringBuilder sb = new StringBuilder();
		try {
			append(sb, data);
		} catch (IOException e) {
			// cannot happen with sb
		}
		return sb.toString();
	}

	public final static void append(Appendable sb, byte[] data) throws IOException {
		for (int i = 0; i < data.length; i++) {
			sb.append(nibble(data[i] >> 4));
			sb.append(nibble(data[i]));
		}
	}

	public final static char nibble(int i) {
		return HEX[i & 0xF];
	}

	public static boolean isHex(String pub) {
		return HEX_P.matcher(pub)
			.matches();
	}

	public static boolean isHexCharacter(char c) {
		if (c < '0')
			return false;

		if (c <= '9')
			return true;

		if (c < 'A')
			return false;

		if (c <= 'F')
			return true;

		// lower case are higher than upper case in Unicode!

		if (c < 'a')
			return false;

		return c <= 'f';
	}

	public static String separated(byte[] bytes, String separator) {
		String del = "";
		StringBuilder sb = new StringBuilder();
		for (byte x : bytes) {
			sb.append(del);
			sb.append(toHex(x));
			del = separator;
		}
		return sb.toString();
	}
}
