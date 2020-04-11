package aQute.lib.zip;

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
}
