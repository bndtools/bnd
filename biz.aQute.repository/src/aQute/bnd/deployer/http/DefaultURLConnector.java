package aQute.bnd.deployer.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.deployer.repository.ProgressWrappingStream;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.progress.ProgressPlugin;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.service.url.URLConnector;
import aQute.service.reporter.Reporter;

@Deprecated
public class DefaultURLConnector implements URLConnector, Plugin, RegistryPlugin {

	@interface Config {
		boolean disableServerVerify();
	}

	private static final String	HEADER_IF_NONE_MATCH	= "If-None-Match";
	private static final String	HEADER_ETAG				= "ETag";
	private static final String	HEADER_LOCATION			= "Location";
	private static final int	RESPONSE_NOT_MODIFIED	= 304;

	private boolean				disableServerVerify		= false;
	private Reporter			reporter				= null;
	private Registry			registry				= null;

	@Override
	public InputStream connect(URL url) throws IOException {
		if (url == null)
			throw new IOException("Can't connect to null URL");
		TaggedData data = connectTagged(url);
		return data.getInputStream();
	}

	@Override
	public void setProperties(Map<String, String> map) {
		disableServerVerify = "true".equalsIgnoreCase(map.get(HttpsUtil.PROP_DISABLE_SERVER_CERT_VERIFY));
	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
		reporter.error(
			"Unfortunately we needed to break the HttpBasicAuthURLConnector plugin :-( Passwords and other communication settings are now down via the Http Client");
	}

	@Override
	public TaggedData connectTagged(URL url) throws IOException {
		return connectTagged(url, null);
	}

	@Override
	public TaggedData connectTagged(URL url, String tag) throws IOException {
		return connectTagged(url, tag, new HashSet<>());
	}

	public TaggedData connectTagged(URL url, String tag, Set<String> loopDetect) throws IOException {
		try {
			TaggedData result;

			loopDetect.add(url.toString());
			URLConnection connection = url.openConnection();
			try {
				if (disableServerVerify)
					HttpsUtil.disableServerVerification(connection);
			} catch (GeneralSecurityException e) {
				if (reporter != null)
					reporter.error("Error attempting to disable SSL server certificate verification: %s", e);
				throw new IOException("Error attempting to disable SSL server certificate verification.");
			}

			if (connection instanceof HttpURLConnection) {
				// Turn on caching and send the ETag
				HttpURLConnection httpConnection = (HttpURLConnection) connection;
				httpConnection.setUseCaches(true);
				if (tag != null)
					httpConnection.setRequestProperty(HEADER_IF_NONE_MATCH, tag);

				httpConnection.setInstanceFollowRedirects(false);
				httpConnection.connect();

				int responseCode = httpConnection.getResponseCode();
				if (responseCode == RESPONSE_NOT_MODIFIED) {
					result = null;
					httpConnection.disconnect();
				} else if (responseCode >= 300 && responseCode < 400) {
					String location = httpConnection.getHeaderField(HEADER_LOCATION);
					if (location == null)
						throw new IOException("HTTP server returned redirect status but Location header was missing.");

					try {
						URL resolved = url.toURI()
							.resolve(location)
							.toURL();
						if (reporter != null)
							reporter.warning("HTTP address redirected from %s to %s", url.toString(),
								resolved.toString());
						if (loopDetect.contains(resolved.toString()))
							throw new IOException(
								String.format("Detected loop in HTTP redirect from '%s' to '%s'.", url, resolved));
						if (Thread.currentThread()
							.isInterrupted())
							throw new IOException("Interrupted");
						result = connectTagged(resolved, tag, loopDetect);
					} catch (URISyntaxException e) {
						throw new IOException(
							String.format("Failed to resolve location '%s' against origin URL: %s", location, url), e);
					}
				} else {
					String responseTag = httpConnection.getHeaderField(HEADER_ETAG);
					// TODO: get content-size from the http header

					InputStream stream = createProgressWrappedStream(connection.getInputStream(), "Downloading " + url,
						-1);
					result = new TaggedData(connection, stream);
				}
			} else {
				// Non-HTTP so ignore all this tagging malarky
				InputStream stream = createProgressWrappedStream(connection.getInputStream(), "Downloading " + url, -1);
				result = new TaggedData(connection, stream);
			}

			return result;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private InputStream createProgressWrappedStream(InputStream inputStream, String name, int size) {
		if (registry == null)
			return inputStream;
		final List<ProgressPlugin> progressPlugins = registry.getPlugins(ProgressPlugin.class);
		if (progressPlugins == null || progressPlugins.isEmpty())
			return inputStream;

		return new ProgressWrappingStream(inputStream, name, size, progressPlugins);
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

}
