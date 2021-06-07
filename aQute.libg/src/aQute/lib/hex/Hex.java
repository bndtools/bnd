package aQute.lib.hex;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Formatter;
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
		for (byte b : data) {
			sb.append(nibble(b >> 4));
			sb.append(nibble(b));
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
		return separated(bytes, 0, bytes.length, separator);
	}

	public static String separated(byte[] bytes, int start, int length, String separator) {

		assert start >= 0;
		assert length + start <= bytes.length;

		String del = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {

			byte x = bytes[i + start];
			sb.append(del);
			sb.append(toHex(x));
			del = separator;
		}
		return sb.toString();
	}

	/**
	 * Format a buffer to show the buffer in a table with 16 bytes per row, hex
	 * values and ascii values are shown.
	 *
	 * @param data the buffer
	 * @return a String with the formatted data
	 */
	public static String format(byte[] data) {
		if (data == null)
			return "";

		try (Formatter f = new Formatter()) {
			StringBuilder ascii = new StringBuilder(30);
			StringBuilder hex = new StringBuilder(30);

			for (int rover = 0; rover < data.length; rover += 16) {
				ascii.setLength(0);
				hex.setLength(0);

				for (int g = 0, p = rover; g < 2 && p < data.length; g++) {
					hex.append(' ');
					ascii.append("  ");

					for (int i = 0; i < 8 && p < data.length; i++) {
						byte c = data[p++];

						hex.append(' ');
						hex.append(toHex(c));
						if (c < ' ' || c > 0x7E)
							c = '.';
						ascii.append((char) c);
					}
				}
				f.format("0x%04x%-50s%s%n", rover, hex, ascii);
			}
			return f.toString();
		}
	}

	/**
	 * Format a buffer to show the buffer in a table with 16 bytes per row, hex
	 * values and ascii values are shown.
	 *
	 * @param data the buffer
	 * @return a String with the formatted data
	 */
	public static String format(ByteBuffer data) {
		if (data == null)
			return "";

		ByteBuffer bb = data.duplicate();
		try (Formatter f = new Formatter()) {
			StringBuilder ascii = new StringBuilder(30);
			StringBuilder hex = new StringBuilder(30);

			for (int rover = 0; bb.hasRemaining(); rover += 16) {
				ascii.setLength(0);
				hex.setLength(0);

				for (int g = 0; g < 2 && bb.hasRemaining(); g++) {
					hex.append(' ');
					ascii.append("  ");

					for (int i = 0; i < 8 && bb.hasRemaining(); i++) {
						byte c = bb.get();

						hex.append(' ');
						hex.append(toHex(c));
						if (c < ' ' || c > 0x7E)
							c = '.';
						ascii.append((char) c);
					}
				}
				f.format("0x%04x%-50s%s%n", rover, hex, ascii);
			}
			return f.toString();
		}
	}

	/**
	 * Check of a buffer is classified as binary or text. We assume a file is
	 * binary of it contains a 0 byte. Heuristics may differ in the future, this
	 * method is really to collect this decision in one place.
	 *
	 * @param data the buffer
	 * @return true of classified as binary
	 */
	public static boolean isBinary(byte[] data) {
		for (byte b : data) {
			if (b == 0)
				return true;
		}
		return false;
	}

	/**
	 * Check of a buffer is classified as binary or text. We assume a file is
	 * binary of it contains a 0 byte. Heuristics may differ in the future, this
	 * method is really to collect this decision in one place.
	 *
	 * @param data the buffer
	 * @return true of classified as binary
	 */
	public static boolean isBinary(ByteBuffer data) {
		ByteBuffer bb = data.duplicate();
		while (bb.hasRemaining()) {
			if (bb.get() == 0) {
				return true;
			}
		}
		return false;
	}

	public static void append(Appendable sb, byte ch) {
		try {
			sb.append(nibble(ch >>> 4));
			sb.append(nibble(ch >>> 0));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void append(Appendable sb, short ch) {
		append(sb, (byte) (0xFF & (ch >>> 8)));
		append(sb, (byte) (0xFF & (ch >>> 0)));
	}

	public static void append(Appendable sb, char ch) {
		append(sb, (short) ch);
	}

	public static void append(Appendable sb, int ch) {
		append(sb, (short) (0xFFFF & (ch >>> 16)));
		append(sb, (short) (0xFFFF & (ch >>> 0)));
	}

	public static void append(Appendable sb, long ch) {
		append(sb, (int) (0xFFFF_FFFF & (ch >>> 32)));
		append(sb, (int) (0xFFFF_FFFF & (ch >>> 0)));
	}
}
