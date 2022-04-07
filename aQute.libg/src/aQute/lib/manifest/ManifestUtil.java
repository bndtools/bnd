package aQute.lib.manifest;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Unfortunately we have to write our own manifest :-( because of a stupid bug
 * in the manifest code. It tries to handle UTF-8 but the way it does it it
 * makes the bytes platform dependent. So the following code outputs the
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
public final class ManifestUtil {
	private static final Comparator<Name>	nameComparator	= Comparator.comparing(Name::toString,
		String.CASE_INSENSITIVE_ORDER);
	private static final Name				NAME			= new Name("Name");
	private static final byte[]				EOL				= new byte[] {
		'\r', '\n'
	};
	private static final byte[]				SEPARATOR		= new byte[] {
		':', ' '
	};
	private static final int				MAX_LENGTH		= 72 - EOL.length;

	private ManifestUtil() {}

	public static void write(Manifest manifest, OutputStream out) throws IOException {
		Attributes mainAttributes = manifest.getMainAttributes();
		Stream<Entry<Name, String>> sortedAttributes = sortedAttributes(mainAttributes);

		Name versionName = Name.MANIFEST_VERSION;
		String versionValue = mainAttributes.getValue(versionName);
		if (versionValue == null) {
			versionName = Name.SIGNATURE_VERSION;
			versionValue = mainAttributes.getValue(versionName);
		}
		if (versionValue != null) {
			writeEntry(out, versionName, versionValue);
			Name filterName = versionName;
			// Name.equals is case-insensitive
			sortedAttributes = sortedAttributes.filter(e -> !Objects.equals(e.getKey(), filterName));
		}
		writeAttributes(out, sortedAttributes);
		out.write(EOL);

		for (Iterator<Entry<String, Attributes>> iterator = manifest.getEntries()
			.entrySet()
			.stream()
			.sorted(Entry.comparingByKey())
			.iterator(); iterator.hasNext();) {
			Entry<String, Attributes> entry = iterator.next();
			writeEntry(out, NAME, entry.getKey());
			writeAttributes(out, sortedAttributes(entry.getValue()));
			out.write(EOL);
		}

		out.flush();
	}

	/**
	 * Write out an entry, handling proper unicode and line length constraints
	 */
	private static void writeEntry(OutputStream out, Name name, String value) throws IOException {
		int width = write(out, 0, name.toString());
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
		for (int position = 0, limit = bytes.length, remaining; (remaining = limit - position) > 0;) {
			if (width >= MAX_LENGTH) {
				out.write(EOL);
				out.write(' ');
				width = 1;
			}
			int count = Math.min(MAX_LENGTH - width, remaining);
			out.write(bytes, position, count);
			position += count;
			width += count;
		}
		return width;
	}

	/**
	 * Output an Attributes map. We sort the map keys.
	 *
	 * @param value the attributes
	 * @param out the output stream
	 * @throws IOException when something fails
	 */
	private static void writeAttributes(OutputStream out, Stream<Entry<Name, String>> attributes) throws IOException {
		for (Iterator<Entry<Name, String>> iterator = attributes.iterator(); iterator.hasNext();) {
			Entry<Name, String> attribute = iterator.next();
			writeEntry(out, attribute.getKey(), attribute.getValue());
		}
	}

	/**
	 * Sort the attributes by key.
	 *
	 * @param attributes the attributes
	 * @throws A sorted stream of the attributes
	 */
	private static Stream<Entry<Name, String>> sortedAttributes(Attributes attributes) {
		Stream<Entry<Name, String>> sorted = coerce(attributes).entrySet()
			.stream()
			.sorted(Entry.comparingByKey(nameComparator));
		return sorted;
	}

	/**
	 * Coerce Attributes to Map<Name,String>.
	 *
	 * @param attribute the attribute
	 * @return A map.
	 */
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	private static Map<Name, String> coerce(Attributes attributes) {
		return (Map) attributes;
	}
}
