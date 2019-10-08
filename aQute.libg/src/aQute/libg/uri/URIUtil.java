package aQute.libg.uri;

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
	 * @param uriString The string containing the URI for which we are
	 *            attempting to construct a path.
	 * @return The method's best guess as to which file on the local filesystem
	 *         this URI refers, or <tt>null</tt> if it's invalid or not a local
	 *         filesystem URI.
	 */
	public static Optional<Path> pathFromURI(String uriString) {
		if (uriString == null) {
			return Optional.empty();
		}

		try {
			URI uri = resolve(EMPTYURI, uriString);
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
		} catch (URISyntaxException e1) {
		}
		return Optional.empty();
	}
}
