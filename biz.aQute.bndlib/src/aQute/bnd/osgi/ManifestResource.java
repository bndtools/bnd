package aQute.bnd.osgi;

import static aQute.bnd.exceptions.BiConsumerWithException.asBiConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import aQute.bnd.stream.MapStream;

/**
 * Bnd Resource for Manifest with correct support for writing the manifest to an
 * output stream.
 */
public class ManifestResource extends WriteResource {
	private final Manifest manifest;

	public ManifestResource(Manifest manifest) {
		this.manifest = requireNonNull(manifest);
	}

	public ManifestResource() {
		this(new Manifest());
	}

	public Manifest getManifest() {
		return manifest;
	}

	@Override
	public long lastModified() {
		return 0L;
	}

	/**
	 * Unfortunately we have to write our own manifest :-( because of a stupid
	 * bug in the manifest code. It tries to handle UTF-8 but the way it does it
	 * it makes the bytes platform dependent. So the following code outputs the
	 * manifest. A Manifest consists of
	 *
	 * <pre>
	 * 'Manifest-Version: 1.0\r\n'
	 * main-attributes * \r\n name-section
	 * main-attributes ::= attributes
	 * attributes ::= key ': ' value '\r\n'
	 * name-section ::= 'Name: ' name '\r\n' attributes
	 * </pre>
	 *
	 * Lines in the manifest should not exceed 72 bytes (! this is where the
	 * manifest screwed up as well when 16 bit unicodes were used).
	 * <p>
	 * As a bonus, we can now sort the manifest!
	 */
	@Override
	public void write(OutputStream out) throws IOException {
		writeEntry(out, "Manifest-Version", "1.0");
		attributes(manifest.getMainAttributes(), out);
		out.write(EOL);

		MapStream.of(manifest.getEntries())
			.sortedByKey()
			.forEachOrdered(asBiConsumer((key, attributes) -> {
				writeEntry(out, "Name", key);
				attributes(attributes, out);
				out.write(EOL);
			}));
		out.flush();
	}

	private final static byte[]	EOL			= new byte[] {
		'\r', '\n'
	};
	private final static byte[]	SEPARATOR	= new byte[] {
		':', ' '
	};

	/**
	 * Write out an entry, handling proper unicode and line length constraints
	 */
	private static void writeEntry(OutputStream out, String name, String value) throws IOException {
		int width = write(out, 0, name);
		width = write(out, width, SEPARATOR);
		write(out, width, value);
		out.write(EOL);
	}

	/**
	 * Convert a string to bytes with UTF-8 and then output in max 72 bytes
	 *
	 * @param out the output string
	 * @param width the current width
	 * @param s the string to output
	 * @return the new width
	 * @throws IOException when something fails
	 */
	private static int write(OutputStream out, int width, String s) throws IOException {
		byte[] bytes = s.getBytes(UTF_8);
		return write(out, width, bytes);
	}

	/**
	 * Write the bytes but ensure that the line length does not exceed 72
	 * characters. If it is more than 70 characters, we just put a cr/lf +
	 * space.
	 *
	 * @param out The output stream
	 * @param width The number of characters output in a line before this method
	 *            started
	 * @param bytes the bytes to output
	 * @return the number of characters in the last line
	 * @throws IOException if something fails
	 */
	private static int write(OutputStream out, int width, byte[] bytes) throws IOException {
		int w = width;
		for (byte b : bytes) {
			if (w >= 72 - EOL.length) { // we need to add the EOL!
				out.write(EOL);
				out.write(' ');
				w = 1;
			}
			out.write(b);
			w++;
		}
		return w;
	}

	/**
	 * Output an Attributes map. We sort the map keys.
	 *
	 * @param value the attributes
	 * @param out the output stream
	 * @throws IOException when something fails
	 */
	private static void attributes(Attributes value, OutputStream out) throws IOException {
		MapStream.of(value)
			.map((k, v) -> MapStream.entry(k.toString(), v.toString()))
			.filterKey(k -> !k.equalsIgnoreCase("Manifest-Version"))
			.sortedByKey(String.CASE_INSENSITIVE_ORDER)
			.forEachOrdered(asBiConsumer((k, v) -> writeEntry(out, k, v)));
	}
}
