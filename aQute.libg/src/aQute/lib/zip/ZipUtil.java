package aQute.lib.zip;

import java.util.TimeZone;
import java.util.zip.ZipEntry;

/**
 * This class provides utilities to work with zip files.
 * http://www.opensource.apple.com/source/zip/zip-6/unzip/unzip/proginfo/extra.
 * fld
 */
public class ZipUtil {
	static TimeZone tz = TimeZone.getDefault();

	public static long getModifiedTime(ZipEntry entry) {
		long time = entry.getTime();
		time += tz.getOffset(time);
		return Math.min(time, System.currentTimeMillis() - 1);
	}

	public static void setModifiedTime(ZipEntry entry, long utc) {
		utc -= tz.getOffset(utc);
		entry.setTime(utc);
	}

}
