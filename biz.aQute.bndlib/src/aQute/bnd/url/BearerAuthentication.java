package aQute.bnd.url;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.converter.Converter;
import aQute.service.reporter.Reporter;

/**
 * Provide Http Bearer Authentication. This URL Connection Handler plugin will
 * add bearer authentication to the matching URL Connections. The following
 * properties must be specified.
 * <ul>
 * <li>{@link aQute.bnd.service.url.URLConnectionHandler#MATCH MATCH} — The URL
 * {@link aQute.libg.glob.Glob Glob} expressions
 * <li>{@code .oauth2Token} — The password for basic authentication
 * </ul>
 */
@aQute.bnd.annotation.plugin.BndPlugin(name = "url.bearer.authentication", parameters = BearerAuthentication.Config.class)
public class BearerAuthentication extends DefaultURLConnectionHandler {
	private final static Logger logger = LoggerFactory.getLogger(BearerAuthentication.class);

	interface Config extends DefaultURLConnectionHandler.Config {
		String _oauth2Token();
	}

	private static final String	HEADER_AUTHORIZATION	= "Authorization";
	private static final String	PREFIX_BEARER_AUTH		= "Bearer ";
	private String				oauth2Token;
	private String				authentication;

	public BearerAuthentication() {}

	public BearerAuthentication(String oauth2Token, Reporter reporter) {
		setReporter(reporter);
		this.oauth2Token = oauth2Token;
		this.authentication = PREFIX_BEARER_AUTH + oauth2Token;
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		super.setProperties(map);
		Config config = Converter.cnv(Config.class, map);
		this.oauth2Token = config._oauth2Token();
		this.authentication = PREFIX_BEARER_AUTH + oauth2Token;
	}

	@Override
	public void handle(URLConnection connection) {
		if (connection instanceof HttpURLConnection && matches(connection) && oauth2Token != null) {
			if (!(connection instanceof HttpsURLConnection))
				logger.debug("using bearer authentication with http instead of https, this is very insecure: {}",
					connection.getURL());

			connection.setRequestProperty(HEADER_AUTHORIZATION, authentication);
		}
	}

	@Override
	public String toString() {
		return "BearerAuthentication [oauth2Token=" + oauth2Token + "]";
	}
}
