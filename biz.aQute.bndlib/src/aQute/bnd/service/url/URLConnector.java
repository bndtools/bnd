package aQute.bnd.service.url;

import java.io.*;
import java.net.*;

public interface URLConnector {
	InputStream connect(URL url) throws IOException;
}
