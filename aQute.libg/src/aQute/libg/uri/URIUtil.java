package aQute.libg.uri;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
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

}
