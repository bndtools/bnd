package aQute.bnd.service.url;

import java.net.*;

/**
 * This interface is used to sign urls, set options, etc. Anybody that interacts
 * with a URL should allow all the {@link URLConnectionHandler} plugins to
 * interact with the URLConnection. There are a number of known implementations:
 * <ul>
 * <li>{@link BndAuthentication} — Authenticates using bnd's built in private
 * key (see {@link Settings}
 * <li>{@link BasicAuthentication} — Http Basic Authentication
 * <li>{@link HttpsVerification} — Can add certificats for verification of Https
 * or disable verification
 * <li>{@link ConnectionSettings} — Can set arbitrary headers on an Http(s)
 * connection
 * </ul>
 * URLConnection are allows matched to the URL. This
 * {@link URLConnectionHandler#MATCH} is a comma separated list of Glob
 * expressions that must match the canonical URL string representation.
 */
public interface URLConnectionHandler {

	/**
	 * Configuration property for the matcher. A comma separated list of
	 * {@link Glob} expressions. If no match is given, all URLs match.
	 */
	String	MATCH	= "match";

	/**
	 * If the corresponding URL matches, modify the connection in the
	 * parameterized way.
	 * 
	 * @param connection
	 *            The connection to modify
	 * @throws Exception
	 */
	void handle(URLConnection connection) throws Exception;

	/**
	 * Answer if this handler matches the given URL
	 * 
	 * @param url
	 *            the url to match
	 * @return true if matched, false if not.
	 */
	boolean matches(URL url);
}
