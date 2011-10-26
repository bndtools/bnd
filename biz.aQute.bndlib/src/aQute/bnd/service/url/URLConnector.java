package aQute.bnd.service.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface URLConnector {
	InputStream connect(URL url) throws IOException;
}
