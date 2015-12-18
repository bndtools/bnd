package aQute.bnd.osgi;

import java.io.InputStream;
import java.io.OutputStream;

public interface Resource {
	InputStream openInputStream() throws Exception;

	void write(OutputStream out) throws Exception;

	long lastModified();

	void setExtra(String extra);

	String getExtra();

	long size() throws Exception;
}
