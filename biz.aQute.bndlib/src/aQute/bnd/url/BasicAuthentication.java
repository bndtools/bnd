package aQute.bnd.url;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.base64.Base64;
import aQute.lib.converter.Converter;
import aQute.libg.cryptography.SHA1;
import aQute.service.reporter.Reporter;

/**
 * Provide Http Basic Authentication. This URL Connection Handler plugin will
 * add basic authentication to the matching URL Connections. The following
 * properties must be specified.
 * <ul>
 * <li>{@link aQute.bnd.service.url.URLConnectionHandler#MATCH MATCH} — The URL
 * {@link aQute.libg.glob.Glob Glob} expressions
 * <li>{@code .password} — The password for basic authentication
 * <li>{@code user} — The user ID
 * </ul>
 */
@aQute.bnd.annotation.plugin.BndPlugin(name = "url.basic.authentication", parameters = BasicAuthentication.Config.class)
public class BasicAuthentication extends DefaultURLConnectionHandler {
	private final static Logger logger = LoggerFactory.getLogger(BasicAuthentication.class);

	interface Config extends DefaultURLConnectionHandler.Config {
		String user();

		String _password();
	}

	private static final String	HEADER_AUTHORIZATION	= "Authorization";
	private static final String	PREFIX_BASIC_AUTH		= "Basic ";
	private String				password;
	private String				user;
	private String				authentication;
	private String				sha;

	public BasicAuthentication() {

	}

	public BasicAuthentication(String user, String password, Reporter reporter) {
		this.user = user;
		this.password = password;
		this.setReporter(reporter);
		init(null);
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		super.setProperties(map);
		Config config = Converter.cnv(Config.class, map);
		this.password = config._password();
		this.user = config.user();

		init(map);
	}

	private void init(Map<String, String> map) {
		if (this.password == null) {
			error("No .password property set on this plugin %s", map);
		}
		if (this.user == null) {
			error("No user property set on this plugin %s", map);
		}
		String authString = user + ":" + password;
		try {
			String encoded = Base64.encodeBase64(authString.getBytes(UTF_8));
			this.authentication = PREFIX_BASIC_AUTH + encoded;
			sha = SHA1.digest(password.getBytes())
				.asHex();
		} catch (Exception e) {
			// cannot happen, UTF-8 is always present
		}
	}

	@Override
	public void handle(URLConnection connection) {
		if (connection instanceof HttpURLConnection && matches(connection) && password != null && user != null) {
			if (!(connection instanceof HttpsURLConnection))
				logger.debug("using basic authentication with http instead of https, this is very insecure: {}",
					connection.getURL());

			connection.setRequestProperty(HEADER_AUTHORIZATION, authentication);
		}
	}

	@Override
	public String toString() {
		return "BasicAuthentication [password=" + sha + ", user=" + user + "]";
	}

}
