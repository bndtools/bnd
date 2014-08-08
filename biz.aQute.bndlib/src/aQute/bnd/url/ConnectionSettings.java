package aQute.bnd.url;

import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.lib.converter.*;

/**
 * Generic connection setter can set a number of basic properties if applicable
 * and can add generic headers. See {@link #config} for the options. For the
 * propeties any property key that starts with an upper case is considered to be
 * a header.
 */
@aQute.bnd.annotation.plugin.BndPlugin(name="url.settings", parameters=ConnectionSettings.Config.class)
public class ConnectionSettings extends DefaultURLConnectionHandler {
	final Map<String,String>	headers	= new HashMap<String,String>();
	Config						config;

	/**
	 * Options to set.
	 */
	interface Config extends  DefaultURLConnectionHandler.Config {
		int connectTimeout();

		int readTimeout();

		boolean useCaches();

		int chunk();

		boolean noredirect();
	}

	/**
	 * Handle the connection
	 */
	public void handle(URLConnection connection) throws Exception {
		if (matches(connection)) {
			if (config.connectTimeout() != 0)
				connection.setConnectTimeout(config.connectTimeout());
			if (config.readTimeout() != 0)
				connection.setConnectTimeout(config.readTimeout());

			for (Entry<String,String> entry : headers.entrySet()) {
				if (Character.isUpperCase(entry.getKey().charAt(0)))
					connection.setRequestProperty(entry.getKey(), entry.getValue());
			}

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
	public void setProperties(Map<String,String> map) throws Exception {
		super.setProperties(map);
		for (Entry<String,String> entry : headers.entrySet()) {
			if (Character.isUpperCase(entry.getKey().charAt(0)))
				headers.put(entry.getKey(), entry.getValue());
		}
		config = Converter.cnv(Config.class, map);
	}

}
