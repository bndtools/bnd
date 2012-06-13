package aQute.lib.osgi;

import java.io.*;

public interface Resource {
	InputStream openInputStream() throws Exception;

	void write(OutputStream out) throws Exception;

	long lastModified();

	void setExtra(String extra);

	String getExtra();

	long size() throws Exception;
}
