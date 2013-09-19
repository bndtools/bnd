package aQute.bnd.service.url;

import java.net.*;

public interface URLConnectionHandler {
	String MATCH = "match";
	void handle( URLConnection connection ) throws Exception;

	boolean matches( URL url);
}
