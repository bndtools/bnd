package aQute.bnd.url;

import java.net.*;

import aQute.bnd.service.*;
import aQute.bnd.service.url.*;

/**
 * Will iterate over the current plugins to find a matching URLConnectionHandler
 * and in the end use the default connector if no alternative is found.
 */
public class MultiURLConnectionHandler implements URLConnectionHandler {

	private Registry	registry;

	public MultiURLConnectionHandler(Registry registry) {
		this.registry = registry;
	}

	public void handle(URLConnection connection) throws Exception {
		for (URLConnectionHandler h : registry.getPlugins(URLConnectionHandler.class)) {
			h.handle(connection);
		}
	}

	public boolean matches(URL url) {
		return true;
	}

}
