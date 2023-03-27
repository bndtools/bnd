package bnd.mr.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class HttpClient {

	public byte[] fetchBytes(URL url) throws IOException {
		try (InputStream stream = url.openStream()) {
			// For Java < 9 we need to use an external library!
			return IOUtils.toByteArray(stream);
		}
	}
}
