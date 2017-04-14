package aQute.bnd.osgi;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import aQute.lib.io.IO;

public class URLResource implements Resource {
	URL		url;
	String	extra;
	long	size	= -1;

	public URLResource(URL url) {
		this.url = url;
	}

	public InputStream openInputStream() throws IOException {
		return url.openStream();
	}

	@Override
	public String toString() {
		return ":" + url.getPath() + ":";
	}

	public void write(OutputStream out) throws Exception {
		IO.copy(this.openInputStream(), out);
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

	public long size() throws Exception {
		if (size >= 0)
			return size;

		try {
			if (url.getProtocol().equals("file:")) {
				File file = new File(url.getPath());
				if (file.isFile())
					return size = file.length();
			} else {
				URLConnection con = url.openConnection();
				if (con instanceof HttpURLConnection) {
					HttpURLConnection http = (HttpURLConnection) con;
					http.setRequestMethod("HEAD");
					http.connect();
					String l = http.getHeaderField("Content-Length");
					if (l != null) {
						return size = Long.parseLong(l);
					}
				}
			}
		} catch (Exception e) {
			// Forget this exception, we do it the hard way
		}
		try (InputStream in = openInputStream(); DataInputStream din = new DataInputStream(in)) {
			long result = din.skipBytes(Integer.MAX_VALUE);
			while (in.read() >= 0) {
				result += din.skipBytes(Integer.MAX_VALUE);
			}
			size = result;
		}
		return size;
	}

}
