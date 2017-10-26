package aQute.bnd.osgi;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;

public interface Resource extends Closeable {
	InputStream openInputStream() throws Exception;

	void write(OutputStream out) throws Exception;

	long lastModified();

	void setExtra(String extra);

	String getExtra();

	long size() throws Exception;

	ByteBuffer buffer() throws Exception;

	static Resource fromURL(URL url) throws IOException {
		return new URLResource(url);
	}
}
