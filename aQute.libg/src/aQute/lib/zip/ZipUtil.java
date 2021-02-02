package aQute.lib.zip;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.TimeZone;
import java.util.zip.ZipEntry;

/**
 * This class provides utilities to work with zip files.
 * http://www.opensource.apple.com/source/zip/zip-6/unzip/unzip/proginfo/extra.
 * fld
 */
public class ZipUtil {
	private static final TimeZone tz = TimeZone.getDefault();

	public static long getModifiedTime(ZipEntry entry) {
		long time = entry.getTime();
		time += tz.getOffset(time);
		return Math.min(time, System.currentTimeMillis() - 1);
	}

	public static void setModifiedTime(ZipEntry entry, long utc) {
		utc -= tz.getOffset(utc);
		entry.setTime(utc);
	}

	enum State {
		begin,
		one,
		two,
		segment
	}

	/**
	 * Clean the input path to avoid ZipSlip issues.
	 * <p>
	 * All double '/', '.' and '..' path entries are resolved and removed. The
	 * returned path will have a '/' at the end when the input path has a '/' at
	 * the end. A leading '/' is stripped. An empty string is unmodified.
	 *
	 * @param path ZipEntry path. Must not be {@code null}.
	 * @return Cleansed ZipEntry path.
	 * @throws UncheckedIOException If the entry used '..' relative paths to
	 *             back up past the start of the path.
	 */

	public static String cleanPath(final String path) {
		final StringBuilder out = new StringBuilder();
		final int length = requireNonNull(path).length();
		State state = State.begin;
		int level = 0;

		for (int i = length - 1; i >= 0; i--) {
			char c = path.charAt(i);
			switch (state) {
				case begin :
					switch (c) {
						case '/' :
							if (i == length - 1)
								out.append('/');
							break;

						case '.' :
							state = State.one;
							break;

						default :
							state = State.segment;
							if (level >= 0)
								out.append(c);
							break;
					}
					break;

				case one :
					switch (c) {
						case '/' :
							state = State.begin;
							break;

						case '.' :
							state = State.two;
							break;

						default :
							state = State.segment;
							if (level >= 0)
								out.append('.')
									.append(c);
							break;
					}
					break;
				case two :
					switch (c) {
						case '/' :
							state = State.begin;
							level--;
							break;

						default :
							state = State.segment;
							if (level >= 0)
								out.append('.')
									.append('.')
									.append(c);
							break;
					}
					break;

				case segment :
					switch (c) {
						case '/' :
							state = State.begin;
							if (level < 0) {
								level++;
								break;
							}
							// FALL THROUGH
						default :
							if (level >= 0)
								out.append(c);
							break;
					}
					break;
			}
		}

		int last = out.length() - 1;

		if (last > 0 && out.charAt(last) == '/')
			out.setLength(last);

		if (out.length() == length)
			return path;

		if ((state == State.one && level == -1) || state == State.two || level < -1)
			throw new UncheckedIOException(new IOException("Entry path is outside of zip file: " + path));

		return out.reverse()
			.toString();
	}

	public static boolean isCompromised(String path) {
		try {
			cleanPath(path);
			return false;
		} catch (UncheckedIOException e) {
			return true;
		}
	}

	public static final int EXTID_BND = 0xBDEA;

	/**
	 * Create a ZIP extra field from a String.
	 * <p>
	 * The String is UTF-8 encoded and the extra field uses the Header ID
	 * {@link #EXTID_BND}.
	 *
	 * @param value The String value to be contained in the ZIP extra field.
	 * @return A ZIP extra field with the UTF-8 encoded String.
	 * @see <a href=
	 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Section
	 *      4.5 - Extensible data fields</a>
	 */
	public static byte[] extraFieldFromString(String value) {
		byte[] utf8 = value.getBytes(UTF_8);
		final int length = utf8.length;
		if (length > 0xFFFF) {
			throw new IllegalArgumentException("extra string too long");
		}
		byte[] extra = new byte[Short.BYTES * 2 + length];
		putUnsignedShort(extra, 0, EXTID_BND);
		putUnsignedShort(extra, Short.BYTES, length);
		System.arraycopy(utf8, 0, extra, Short.BYTES * 2, length);
		return extra;
	}

	/**
	 * Extract a String from a ZIP extra field.
	 * <p>
	 * The Header ID {@link #EXTID_BND} is searched for in the specified extra
	 * field. If found, the UTF-8 encoded value is converted to a String and
	 * returned.
	 * <p>
	 * If the specified extra field is not valid, the extra field is considered
	 * to be a UTF-8 encoded value and is converted to a String and returned.
	 *
	 * @return The String value contained in the ZIP extra field or {@code null}
	 *         is there is no {@link #EXTID_BND} Header ID and the ZIP extra
	 *         field data is not invalid.
	 * @see <a href=
	 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Section
	 *      4.5 - Extensible data fields</a>
	 */
	public static String stringFromExtraField(byte[] extra) {
		final int length = extra.length;
		if (length > 0xFFFF) {
			throw new IllegalArgumentException("extra data too long");
		}
		int offset = 0;
		while ((offset + Short.BYTES * 2) < length) {
			int id = getUnsignedShort(extra, offset);
			int size = getUnsignedShort(extra, offset + Short.BYTES);
			if ((size < 0) || ((offset + Short.BYTES * 2 + size) > length)) {
				break; // invalid extra field
			}
			offset += Short.BYTES * 2;
			switch (id) {
				case EXTID_BND :
					return new String(extra, offset, size, UTF_8);
				default :
					break;
			}
			offset += size;
		}
		if (offset < length) {
			return new String(extra, offset, length - offset, UTF_8);
		}
		return null;
	}

	private static void putUnsignedShort(byte[] bytes, int offset, int value) {
		bytes[offset] = (byte) value;
		bytes[offset + 1] = (byte) (value >> 8);
	}

	private static int getUnsignedShort(byte[] bytes, int offset) {
		return Byte.toUnsignedInt(bytes[offset]) | (Byte.toUnsignedInt(bytes[offset + 1]) << 8);
	}
}
