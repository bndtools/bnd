package aQute.lib.osgi;

import java.io.*;
import java.net.*;

public class URLResource implements Resource {
	URL	url;
	String	extra;
	
	public URLResource(URL url) {
		this.url = url;
	}

	public InputStream openInputStream() throws IOException {
		return url.openStream();
	}

	public String toString() {
		return ":" + url.getPath() + ":";
	}

	public void write(OutputStream out) throws Exception {
		FileResource.copy(this, out);
	}

	public long lastModified() {
		return -1;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
}
