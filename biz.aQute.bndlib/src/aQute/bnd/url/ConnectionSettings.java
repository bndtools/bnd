package aQute.bnd.url;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.stream.MapStream;
import aQute.lib.converter.Converter;

/**
 * Generic connection setter can set a number of basic properties if applicable
 * and can add generic headers. See {@link #config} for the options. For the
 * propeties any property key that starts with an upper case is considered to be
 * a header.
 */
@aQute.bnd.annotation.plugin.BndPlugin(name = "url.settings", parameters = ConnectionSettings.Config.class)
public class ConnectionSettings extends DefaultURLConnectionHandler {
	final Map<String, String>	headers	= new HashMap<>();
	Config						config;

	/**
	 * Options to set.
	 */
	interface Config extends DefaultURLConnectionHandler.Config {
		int connectTimeout();

		int readTimeout();

		boolean useCaches();

		int chunk();

		boolean noredirect();
	}

	/**
	 * Handle the connection
	 */
	@Override
	public void handle(URLConnection connection) throws Exception {
		if (matches(connection)) {
			if (config.connectTimeout() != 0)
				connection.setConnectTimeout(config.connectTimeout());
			if (config.readTimeout() != 0)
				connection.setConnectTimeout(config.readTimeout());

			MapStream.of(headers)
				.forEachOrdered(connection::setRequestProperty);

			if (connection instanceof HttpURLConnection) {
				HttpURLConnection http = (HttpURLConnection) connection;
				if (config.chunk() > 0)
					http.setChunkedStreamingMode(config.chunk());

				http.setInstanceFollowRedirects(!config.noredirect());

			}
		}
	}

	/**
	 * Set the properties.
	 */
	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		super.setProperties(map);
		MapStream.of(map)
			.filterKey(key -> Character.isUpperCase(key.charAt(0)))
			.forEachOrdered(headers::put);
		config = Converter.cnv(Config.class, map);
	}

}
