package bnd.mr.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class HttpClient {

	public byte[] fetchBytes(URL url) throws IOException {
		try (InputStream stream = url.openStream()) {
			// For Java >= 9 we can use the build-in
			return stream.readAllBytes();
		}
	}
}
