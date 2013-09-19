package aQute.bnd.url;

import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.bnd.service.*;
import aQute.lib.converter.*;

/**
 * Will iterate over the current plugins to find a matching URLConnectionHandler
 * and in the end use the default connector if no alternative is found.
 */
public class ConnectionSettings extends DefaultURLConnectionHandler {
	Config config;
	interface Config {
		int connectTimeout();
		int readTimeout();
		boolean useCaches();
		int chunk();
		boolean noredirect();
	}
	final Map<String,String> headers = new HashMap<String,String>(); 
	
	public ConnectionSettings(Registry registry) {
		this.registry = registry;
	}

	public void handle(URLConnection connection) throws Exception {
		if ( matches(connection)) {
			if ( config.connectTimeout() != 0)
				connection.setConnectTimeout(config.connectTimeout());
			if ( config.readTimeout() != 0)
				connection.setConnectTimeout(config.readTimeout());

			for ( Entry<String,String> entry : headers.entrySet()) {
				if ( Character.isUpperCase(entry.getKey().charAt(0)))
					connection.setRequestProperty(entry.getKey(), entry.getValue());
			}
			
			if ( connection instanceof HttpURLConnection) {
				HttpURLConnection http = (HttpURLConnection) connection;
				if (config.chunk() > 0)
					http.setChunkedStreamingMode(config.chunk());

				http.setInstanceFollowRedirects(!config.noredirect());
				
			}
 		}
	}
	
	public void setProperties(Map<String,String> map) throws Exception {
		for ( Entry<String,String> entry : headers.entrySet()) {
			if ( Character.isUpperCase(entry.getKey().charAt(0)))
				headers.put(entry.getKey(), entry.getValue());
		}
		config = Converter.cnv(Config.class, map);
	}


}
