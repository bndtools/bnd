package aQute.lib.deployer.obr;

import java.io.*;
import java.net.*;

import aQute.bnd.service.url.*;

public class DefaultURLConnector implements URLConnector {

	private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
	private static final String HEADER_ETAG = "ETag";
	private static final int RESPONSE_NOT_MODIFIED = 304;

	public InputStream connect(URL url) throws IOException {
		if (url == null)
			throw new IOException("Can't connect to null URL");
		return url.openStream();
	}

	public TaggedData connectTagged(URL url) throws IOException {
		return connectTagged(url, null);
	}

	public TaggedData connectTagged(URL url, String tag) throws IOException {
		TaggedData result;

		URLConnection connection = url.openConnection();
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
				result = new TaggedData(responseTag, connection.getInputStream());
			}
		} else {
			// Non-HTTP so ignore all this tagging malarky
			result = new TaggedData(null, connection.getInputStream());
		}

		return result;
	}

}
