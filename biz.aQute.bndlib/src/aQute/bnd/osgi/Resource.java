package aQute.bnd.osgi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.Locale;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.URLResource.JarURLUtil;

public interface Resource extends Closeable {
	InputStream openInputStream() throws Exception;

	void write(OutputStream out) throws Exception;

	long lastModified();

	/**
	 * Use {@link #encodeExtra(byte[])} to properly encode the ZIP extra field
	 * structured binary data into the specified String.
	 *
	 * @param extra A String encoding the ZIP extra field.
	 */
	void setExtra(String extra);

	/**
	 * Use {@link #decodeExtra(String)} to properly decode the ZIP extra field
	 * structured binary data from the returned String.
	 *
	 * @return A String encoding the ZIP extra field.
	 */
	String getExtra();

	long size() throws Exception;

	ByteBuffer buffer() throws Exception;

	static Resource fromURL(URL url) throws IOException {
		return fromURL(url, null);
	}

	static Resource fromURL(URL url, HttpClient client) throws IOException {
		String protocol = url.getProtocol()
			.toLowerCase(Locale.ROOT);
		if (protocol.equals("file")) {
			URI uri = URI.create(url.toExternalForm());
			Path path = new File(uri.getSchemeSpecificPart()).toPath()
				.toAbsolutePath();
			return new FileResource(path);
		}
		if (protocol.equals("jar")) {
			JarURLUtil util = new JarURLUtil(url);
			URL jarFileURL = util.getJarFileURL();
			if (jarFileURL.getProtocol()
				.equalsIgnoreCase("file")) {
				URI uri = URI.create(jarFileURL.toExternalForm());
				Path path = new File(uri.getSchemeSpecificPart()).toPath()
					.toAbsolutePath();
				String entryName = util.getEntryName();
				if (entryName == null) {
					return new FileResource(path);
				}
				return new ZipResource(path, entryName);
			}
		}
		return new URLResource(url, protocol.equals("jrt") ? null : client);
	}

	/**
	 * Encode the ZIP extra field as a String.
	 * <p>
	 * Since the Resource API uses a String as the storage format for the extra
	 * field and the extra field is structured binary data, we encode the byte
	 * array as a char array in a String. Since the byte array can have an odd
	 * length, we must also encode the array length so that we can decode into
	 * the correct byte array length.
	 *
	 * @param extra A ZIP extra field.
	 * @return A String encoding of the specified ZIP extra field.
	 * @see <a href=
	 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Section
	 *      4.5 - Extensible data fields</a>
	 */
	static String encodeExtra(byte[] extra) {
		final int length = extra.length;
		if (length > 0xFFFF) {
			throw new IllegalArgumentException("extra data too long");
		}
		// we allocate an even length to hold all the bytes in chars
		ByteBuffer bb = ByteBuffer.allocate(Short.BYTES + length + length % 2)
			.order(ByteOrder.LITTLE_ENDIAN);
		CharBuffer cb = bb.asCharBuffer();
		bb.putShort((short) length);
		bb.put(extra, 0, length);
		return cb.toString();
	}

	/**
	 * Decode a String to a ZIP extra field.
	 * <p>
	 * Since the Resource API uses a String as the storage format for the extra
	 * field and the extra field is structured binary data, we encode the byte
	 * array as a char array in a String. Since the byte array can have an odd
	 * length, we must also encode the array length so that we can decode into
	 * the correct byte array length.
	 *
	 * @param encoded A String encoding of the ZIP extra field.
	 * @return The ZIP extra field encoded in the specified string.
	 * @see <a href=
	 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Section
	 *      4.5 - Extensible data fields</a>
	 */
	static byte[] decodeExtra(String encoded) {
		ByteBuffer bb = ByteBuffer.allocate(encoded.length() * Character.BYTES)
			.order(ByteOrder.LITTLE_ENDIAN);
		CharBuffer cb = bb.asCharBuffer();
		cb.put(encoded);
		final int length = Short.toUnsignedInt(bb.getShort());
		if (length != (bb.remaining() - length % 2)) {
			throw new IllegalArgumentException("invalid encoding");
		}
		byte[] extra = new byte[length];
		bb.get(extra, 0, length);
		return extra;
	}
}
