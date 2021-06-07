package aQute.libg.gzip;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class GZipUtils {

	/**
	 * Determines whether the specified stream contains gzipped data, by
	 * checking for the GZIP magic number, and returns a stream capable of
	 * reading those data.
	 *
	 * @throws IOException
	 */
	public static InputStream detectCompression(InputStream stream) throws IOException {
		InputStream buffered;
		if (stream.markSupported())
			buffered = stream;
		else
			buffered = new BufferedInputStream(stream);

		buffered.mark(2);
		int magic = readUnsignedShort(buffered);
		buffered.reset();

		InputStream result;
		if (magic == GZIPInputStream.GZIP_MAGIC)
			result = new GZIPInputStream(buffered);
		else
			result = buffered;
		return result;
	}

	/*
	 * Reads unsigned short in Intel byte order.
	 */
	private static int readUnsignedShort(InputStream in) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if ((b1 | b2) < 0) {
			throw new EOFException();
		}
		return ((b1 << 0) + (b2 << 8));
	}
}
