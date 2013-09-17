package aQute.bnd.deployer.http;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import aQute.bnd.service.*;
import aQute.bnd.service.progress.*;
import aQute.bnd.service.url.*;
import aQute.service.reporter.*;

public class DefaultURLConnector implements URLConnector, Plugin, RegistryPlugin {

	private static final String	HEADER_IF_NONE_MATCH	= "If-None-Match";
	private static final String	HEADER_ETAG				= "ETag";
	private static final int	RESPONSE_NOT_MODIFIED	= 304;

	private boolean				disableServerVerify		= false;
	private Reporter			reporter				= null;
	private Registry			registry				= null;

	public InputStream connect(URL url) throws IOException {
		if (url == null)
			throw new IOException("Can't connect to null URL");
		TaggedData data = connectTagged(url);
		return data.getInputStream();
	}

	public void setProperties(Map<String,String> map) {
		disableServerVerify = "true".equalsIgnoreCase(map.get(HttpsUtil.PROP_DISABLE_SERVER_CERT_VERIFY));
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public TaggedData connectTagged(URL url) throws IOException {
		return connectTagged(url, null);
	}

	public TaggedData connectTagged(URL url, String tag) throws IOException {
		TaggedData result;

		URLConnection connection = url.openConnection();
		try {
			if (disableServerVerify)
				HttpsUtil.disableServerVerification(connection);
		}
		catch (GeneralSecurityException e) {
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

			httpConnection.connect();

			int responseCode = httpConnection.getResponseCode();
			if (responseCode == RESPONSE_NOT_MODIFIED) {
				result = null;
				httpConnection.disconnect();
			} else {
				String responseTag = httpConnection.getHeaderField(HEADER_ETAG);
				// TODO: get content-size from the http header
				
				InputStream stream = createProgressWrappedStream(connection.getInputStream(), "Downloading " + url, -1);
				result = new TaggedData(responseTag, stream);
			}
		} else {
			// Non-HTTP so ignore all this tagging malarky
			InputStream stream = createProgressWrappedStream(connection.getInputStream(), "Downloading " + url, -1);
			result = new TaggedData(null, stream);
		}

		return result;
	}

	private InputStream createProgressWrappedStream(InputStream inputStream, String name, int size) {
		if (registry == null)
			return inputStream;
		ProgressPlugin progressPlugin = registry.getPlugin(ProgressPlugin.class);
		if (progressPlugin == null)
			return inputStream;
		
		return new ProgressWrappingStream(inputStream, name, size, progressPlugin);
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

}
