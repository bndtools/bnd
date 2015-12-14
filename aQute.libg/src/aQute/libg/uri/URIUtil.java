package aQute.libg.uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URIUtil {

	private static final Pattern DRIVE_LETTER_PATTERN = Pattern.compile("(.):\\\\(.*)");

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
		URI resolved;
		boolean emptyRef = reference.isEmpty();
		if (emptyRef) {
			resolved = baseURI.resolve(URI.create("#"));
			String resolvedStr = resolved.toASCIIString();
			resolved = URI.create(resolvedStr.substring(0, resolvedStr.indexOf('#')));
		} else {
			// A Windows path such as "C:\Users" is interpreted as a URI with
			// a scheme of "C". Use a regex that matches the colon-backslash
			// combination to handle this specifically as an absolute file URI.
			Matcher driveLetterMatcher = DRIVE_LETTER_PATTERN.matcher(reference);
			if (driveLetterMatcher.matches()) {
				String path = "///" + reference.replace('\\', '/');
				resolved = new URI("file", null, path, null);
				// URI(scheme, host, path, fragment)
			} else {
				resolved = baseURI.resolve(reference.replace('\\', '/'));
			}
		}

		return resolved;
	}

}
