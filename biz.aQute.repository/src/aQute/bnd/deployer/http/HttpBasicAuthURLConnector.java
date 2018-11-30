package aQute.bnd.deployer.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.service.Plugin;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.service.url.URLConnector;
import aQute.lib.base64.Base64;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.glob.Glob;
import aQute.service.reporter.Reporter;

@Deprecated
@aQute.bnd.annotation.plugin.BndPlugin(name = "urlconnector", parameters = HttpBasicAuthURLConnector.Config.class)
public class HttpBasicAuthURLConnector implements URLConnector, Plugin {
	private final static Logger logger = LoggerFactory.getLogger(HttpBasicAuthURLConnector.class);

	@interface Config {
		String configs();

		boolean disableServerVerify() default false;
	}

	private static final String	PREFIX_PATTERN			= "pattern.";
	private static final String	PREFIX_USER				= "uid.";
	private static final String	PREFIX_PASSWORD			= "pwd.";

	private static final String	HEADER_AUTHORIZATION	= "Authorization";
	private static final String	PREFIX_BASIC_AUTH		= "Basic ";

	private static final String	HEADER_IF_NONE_MATCH	= "If-None-Match";
	private static final String	HEADER_ETAG				= "ETag";
	private static final int	RESPONSE_NOT_MODIFIED	= 304;

	private static class Mapping {
		String	name;
		Glob	urlPattern;
		String	user;
		String	pass;

		Mapping(String name, Glob urlPattern, String user, String pass) {
			this.name = name;
			this.urlPattern = urlPattern;
			this.user = user;
			this.pass = pass;
		}
	}

	private final AtomicBoolean	inited				= new AtomicBoolean(false);
	private final List<Mapping>	mappings			= new LinkedList<>();

	private Reporter			reporter;
	private String				configFileList;
	private boolean				disableSslVerify	= false;

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
		reporter.error(
			"Unfortunately we needed to break the HttpBasicAuthURLConnector plugin :-( Passwords and other communication settings are now down via the Http Client");
	}

	@Override
	public void setProperties(Map<String, String> map) {
		configFileList = map.get("configs");
		if (configFileList == null)
			throw new IllegalArgumentException("'configs' must be specified on HttpBasicAuthURLConnector");
		disableSslVerify = "true".equalsIgnoreCase(map.get(HttpsUtil.PROP_DISABLE_SERVER_CERT_VERIFY));
	}

	protected void init() {
		if (inited.compareAndSet(false, true)) {
			mappings.clear();

			StringTokenizer tokenizer = new StringTokenizer(configFileList, ",");
			while (tokenizer.hasMoreTokens()) {
				String configFileName = tokenizer.nextToken()
					.trim();

				File file = new File(configFileName);
				if (file.exists()) {
					Properties props = new UTF8Properties();

					try (InputStream stream = IO.stream(file)) {
						props.load(stream);

						for (Object key : props.keySet()) {
							String name = (String) key;

							if (name.startsWith(PREFIX_PATTERN)) {
								String id = name.substring(PREFIX_PATTERN.length());

								Glob glob = new Glob(props.getProperty(name));
								String uid = props.getProperty(PREFIX_USER + id);
								String pwd = props.getProperty(PREFIX_PASSWORD + id);

								mappings.add(new Mapping(id, glob, uid, pwd));
							}
						}
					} catch (IOException e) {
						if (reporter != null)
							reporter.error("Failed to load %s", configFileName);
					}
				}
			}
		}
	}

	@Override
	public InputStream connect(URL url) throws Exception {
		TaggedData data = connectTagged(url, null);
		if (data == null)
			throw new IOException("HTTP server did not respond with data.");

		return data.getInputStream();
	}

	@Override
	public TaggedData connectTagged(URL url) throws Exception {
		return connectTagged(url, null);
	}

	@Override
	public TaggedData connectTagged(URL url, String tag) throws Exception {
		init();

		for (Mapping mapping : mappings) {
			Matcher matcher = mapping.urlPattern.matcher(url.toString());
			if (matcher.find()) {
				logger.debug("Found username {}, password ***** for URL '{}'. Matched on pattern {}={}", mapping.user,
					url, mapping.name, mapping.urlPattern);
				return connectTagged(url, tag, mapping.user, mapping.pass);
			}
		}
		logger.debug("No username/password found for URL '{}'.", url);
		return connectTagged(url, tag, null, null);
	}

	private TaggedData connectTagged(URL url, String tag, String user, String pass) throws Exception {
		TaggedData result;

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		try {
			if (disableSslVerify)
				HttpsUtil.disableServerVerification(connection);
		} catch (GeneralSecurityException e) {
			if (reporter != null)
				reporter.error("Error attempting to disable SSL server certificate verification: %s", e);
			throw new IOException("Error attempting to disable SSL server certificate verification.");
		}

		// Add the authorization string using HTTP Basic Auth
		if (user != null && pass != null) {
			String authString = user + ":" + pass;
			String encoded = Base64.encodeBase64(authString.getBytes(UTF_8));
			connection.setRequestProperty(HEADER_AUTHORIZATION, PREFIX_BASIC_AUTH + encoded);
		}

		// Add the ETag
		if (tag != null)
			connection.setRequestProperty(HEADER_IF_NONE_MATCH, tag);

		connection.connect();

		int responseCode = connection.getResponseCode();
		if (responseCode == RESPONSE_NOT_MODIFIED)
			result = null;
		else {
			String responseTag = connection.getHeaderField(HEADER_ETAG);
			result = new TaggedData(connection, connection.getInputStream());
		}

		return result;
	}

}
