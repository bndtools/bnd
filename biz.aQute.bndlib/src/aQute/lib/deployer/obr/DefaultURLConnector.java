package aQute.lib.deployer.obr;

import java.io.*;
import java.net.*;

import aQute.bnd.service.url.*;

public class DefaultURLConnector implements URLConnector {

	public InputStream connect(URL url) throws IOException {
		if (url == null)
			throw new IOException("Can't connect to null URL");
		return url.openStream();
	}

}
