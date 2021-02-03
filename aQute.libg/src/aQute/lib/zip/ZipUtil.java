package aQute.lib.zip;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
	 * Add a ZIP extra field from a String.
	 * <p>
	 * The String is UTF-8 encoded and the extra field uses the Header ID
	 * {@link #EXTID_BND}.
	 *
	 * @param extra The extra field to modify by adding or replacing the
	 *            {@link #EXTID_BND} header. May be {@code null}.
	 * @param value The String value to be contained in the ZIP extra field.
	 * @return A ZIP extra field including the {@link #EXTID_BND} header with
	 *         the UTF-8 encoded String.
	 * @see <a href=
	 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Section
	 *      4.5 - Extensible data fields</a>
	 */
	public static byte[] extraFieldFromString(byte[] extra, String value) {
		byte[] utf8 = value.getBytes(UTF_8);
		int length = Short.BYTES * 2 + utf8.length;
		if (extra == null) {
			if (length > 0xFFFF) {
				throw new IllegalArgumentException("extra data too long");
			}

			return ByteBuffer.allocate(length)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) EXTID_BND)
				.putShort((short) utf8.length)
				.put(utf8)
				.array();
		}

		ByteBuffer original = ByteBuffer.wrap(extra)
			.order(ByteOrder.LITTLE_ENDIAN);
		final int limit = original.limit();
		length += limit;

		int extPosition = 0;
		int extSize = 0;
		while (original.remaining() > (Short.BYTES * 2)) {
			int id = Short.toUnsignedInt(original.getShort());
			int size = Short.toUnsignedInt(original.getShort());
			if ((size < 0) || (size > original.remaining())) {
				break; // invalid extra field
			}
			if (id == EXTID_BND) {
				// we are replacing an existing EXTID_BND record
				extPosition = original.position() - Short.BYTES * 2;
				extSize = Short.BYTES * 2 + size;
				length -= extSize;
				break;
			}
			original.position(original.position() + size);
		}

		if (length > 0xFFFF) {
			throw new IllegalArgumentException("extra data too long");
		}

		ByteBuffer bb = ByteBuffer.allocate(length)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) EXTID_BND)
			.putShort((short) utf8.length)
			.put(utf8);
		original.rewind();
		if (extSize > 0) { // we are replacing an existing EXTID_BND record
			// copy from before existing EXTID_BND record
			original.limit(extPosition);
			bb.put(original);
			// move to after existing EXTID_BND record
			original.limit(limit)
				.position(extPosition + extSize);
		}
		return bb.put(original)
			.array();
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
		ByteBuffer original = ByteBuffer.wrap(extra)
			.order(ByteOrder.LITTLE_ENDIAN);
		while (original.remaining() > (Short.BYTES * 2)) {
			int id = Short.toUnsignedInt(original.getShort());
			int size = Short.toUnsignedInt(original.getShort());
			if ((size < 0) || (size > original.remaining())) {
				original.position(original.position() - Short.BYTES * 2);
				break; // invalid extra field
			}
			if (id == EXTID_BND) {
				original.limit(original.position() + size);
				break; // UTF-8 encoded string
			}
			original.position(original.position() + size);
		}
		if (original.hasRemaining()) {
			return UTF_8.decode(original)
				.toString();
		}
		return null;
	}
}
