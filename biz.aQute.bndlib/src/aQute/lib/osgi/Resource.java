package aQute.lib.osgi;

import java.io.*;

public interface Resource {
	InputStream openInputStream() throws IOException ;
	void write(OutputStream out) throws IOException;
	long lastModified();
	void setExtra(String extra);
	String getExtra();
}
