package aQute.libg.uri;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class URIUtil {
	private static final Pattern WINDOWS_FILE_PATTERN = Pattern.compile("(?:\\p{Alpha}:[\\\\/]|\\\\\\\\|//)");

	/**
	 * Resolves a URI reference against a base URI. Work-around for bugs in
	 * java.net.URI
	 * (e.g.http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4708535)
	 *
	 * @param baseURI
	 * @param reference
	 * @return the resolved {@code URI}
	 * @throws URISyntaxException
	 */
	public static URI resolve(URI baseURI, String reference) throws URISyntaxException {
		if (reference.isEmpty()) {
			return new URI(baseURI.getScheme(), baseURI.getSchemeSpecificPart(), null);
		}

		// A Windows path such as "C:\Users" is interpreted as a URI with
		// a scheme of "C". Use a regex that matches the colon-backslash
		// combination to handle this specifically as an absolute file URI.
		if (WINDOWS_FILE_PATTERN.matcher(reference)
			.lookingAt()) {
			return new File(reference).toURI();
		}

		reference = reference.replace('\\', '/');
		URI refURI;
		try {
			refURI = new URI(reference);
		} catch (URISyntaxException e) {
			refURI = new URI(null, reference, null);
		}
		return baseURI.resolve(refURI);
	}

	private static final URI EMPTYURI = URI.create("");

	/**
	 * Attempts to fetch a path on the file system for the given URI string.
	 * Tries a few more tricks than the standard method of
	 * <tt>Paths.get(new URI(uriString))</tt> - it can handle plain paths, and
	 * also nested URI pseudo-schemes like <tt>reference:</tt> and
	 * <tt>jar:</tt>. Examples:
	 * <ul>
	 * <li>/path/to/file => /path/to/file</li>
	 * <li>reference:file:/path/to/file => /path/to/file</li>
	 * <li>jar:file:/path/to/file.jar!/some/contained/element =>
	 * /path/to/file.jar</li>
	 * <li>http://server/path/to/file => <tt>null</tt></li>
	 * </ul>
	 *
	 * @param uriString The URI string for which we are attempting to construct
	 *            a path.
	 * @return The method's best guess as to which file on the local filesystem
	 *         this URI refers, or an empty Optional if it's invalid or not a
	 *         local filesystem URI.
	 */
	public static Optional<Path> pathFromURI(String uriString) {
		if (uriString == null) {
			return Optional.empty();
		}

		try {
			URI uri = resolve(EMPTYURI, uriString);
			return pathFromURI(uriString, uri);
		} catch (URISyntaxException e1) {
			return Optional.empty();
		}
	}

	/**
	 * Attempts to fetch a path on the file system for the given URI. Tries a
	 * few more tricks than the standard method of <tt>Paths.get(uri)</tt> - it
	 * can handle plain paths, and also nested URI pseudo-schemes like
	 * <tt>reference:</tt> and <tt>jar:</tt>. Examples:
	 * <ul>
	 * <li>/path/to/file => /path/to/file</li>
	 * <li>reference:file:/path/to/file => /path/to/file</li>
	 * <li>jar:file:/path/to/file.jar!/some/contained/element =>
	 * /path/to/file.jar</li>
	 * <li>http://server/path/to/file => <tt>null</tt></li>
	 * </ul>
	 *
	 * @param uri The URI for which we are attempting to construct a path.
	 * @return The method's best guess as to which file on the local filesystem
	 *         this URI refers, or an empty Optional if it's invalid or not a
	 *         local filesystem URI.
	 */
	public static Optional<Path> pathFromURI(URI uri) {
		if (uri == null) {
			return Optional.empty();
		}
		return pathFromURI(uri.getSchemeSpecificPart(), uri);
	}

	private static Optional<Path> pathFromURI(String uriString, URI uri) {
		try {
			while (!EMPTYURI.equals(uri)) {
				final String scheme = uri.getScheme();
				if (scheme == null) {
					try {
						return Optional.of(Paths.get(uriString));
					} catch (InvalidPathException e) {
						return Optional.empty();
					}
				}

				switch (scheme.toLowerCase(Locale.ROOT)) {
					case "file" :
						return Optional.of(Paths.get(uri));
					case "jar" :
					case "bundle" :
					case "zip" :
						uriString = uri.getSchemeSpecificPart();
						int index = uriString.indexOf("!/");
						if (index >= 0) {
							uriString = uriString.substring(0, index);
						}
						uri = resolve(EMPTYURI, uriString);
						break;
					case "reference" :
						uriString = uri.getSchemeSpecificPart();
						uri = resolve(EMPTYURI, uriString);
						break;
					default :
						uriString = uri.getSchemeSpecificPart();
						uri = new URI(uriString);
						if (uri.getScheme() == null) {
							return Optional.empty();
						}
						break;
				}
			}
		} catch (URISyntaxException e1) {}
		return Optional.empty();
	}

	/**
	 * Answer if the given URL is on the local system or remote
	 */
	public static boolean isRemote(URI uri) {
		String scheme = uri.getScheme();
		if (scheme == null)
			return false;

		switch (scheme.toLowerCase(Locale.ROOT)) {
			case "file" :
			case "jar" :
			case "data" :
				return false;

			default :
				return true;

		}
	}

	public static int getDefaultPort(String scheme) {
		switch (scheme) {
			case "http" :
				return 80;
			case "https" :
				return 443;
			case "ftp" :
				return 20;
		}
		return -1;
	}

	public static String encodePath(String path) {
		if ((path == null) || path.isEmpty()) {
			return path;
		}
		byte[] bytes = path.getBytes(UTF_8);
		final int length = bytes.length;
		byte[] encoded = new byte[length * 3];
		int i = 0;
		for (byte b : bytes) {
			if (isPathAllowed(b)) {
				encoded[i++] = b;
			} else {
				encoded[i++] = '%';
				encoded[i++] = (byte) Character.forDigit((b >> 4) & 0xF, 16);
				encoded[i++] = (byte) Character.forDigit(b & 0xF, 16);
			}
		}
		if (i > length) {
			return new String(encoded, 0, i, UTF_8);
		}
		return path;
	}

	static boolean isPathAllowed(int b) {
		switch (b) {
			case '/' : // path_segments
			case ';' : // segment
			case ':' : // pchar
			case '@' :
			case '&' :
			case '=' :
			case '+' :
			case '$' :
			case ',' :
			case '-' : // mark
			case '_' :
			case '.' :
			case '!' :
			case '~' :
			case '*' :
			case '\'' :
			case '(' :
			case ')' :
				return true;
			default :
				return isAlphanum(b);
		}
	}

	static boolean isAlphanum(int b) {
		return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z');
	}

	public static String decode(String source) {
		if ((source == null) || source.isEmpty()) {
			return source;
		}
		final int length = source.length();
		byte[] decoded = new byte[length];
		int i = 0;
		for (int j = 0; j < length; j++) {
			int c = source.charAt(j);
			if (c == '%') {
				decoded[i++] = (byte) ((Character.digit(source.charAt(++j), 16) << 4)
					| Character.digit(source.charAt(++j), 16));
			} else {
				decoded[i++] = (byte) c;
			}
		}
		if (i < length) {
			return new String(decoded, 0, i, UTF_8);
		}
		return source;
	}

}
