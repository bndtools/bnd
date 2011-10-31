package aQute.lib.deployer.obr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import aQute.bnd.service.url.URLConnector;

public class DefaultURLConnector implements URLConnector {

	public InputStream connect(URL url) throws IOException {
		if (url == null)
			throw new IOException("Can't connect to null URL");
		return url.openStream();
	}

}
