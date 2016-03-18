package aQute.bnd.service.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.Date;

/**
 * Represents a data stream that has a tag associated with it; the primary
 * use-case is an HTTP response stream with an ETag header.
 * 
 * @author Neil Bartlett
 */
public class TaggedData {

	private final URLConnection	con;
	private final int			responseCode;
	private final String		etag;
	private final InputStream	in;
	private final URI			url;

	@Deprecated
	public TaggedData(String tag, InputStream inputStream, int responseCode, long modified, URI url) {
		throw new RuntimeException();
	}

	@Deprecated
	public TaggedData(String tag, InputStream inputStream, int responseCode) {
		throw new RuntimeException();
	}

	@Deprecated
	public TaggedData(String tag, InputStream inputStream) {
		throw new RuntimeException();
	}

	public TaggedData(URLConnection con, InputStream in) throws Exception {
		this.con = con;
		this.in = in;
		this.etag = con.getHeaderField("ETag");
		this.responseCode = con instanceof HttpURLConnection ? ((HttpURLConnection) con).getResponseCode() : -1;
		this.url = con.getURL().toURI();
	}

	/**
	 * Returns the ETag for the retrieved resource, or {@code null} if the ETag
	 * was not provided by the server.
	 */
	public String getTag() {
		return etag;
	}

	/**
	 * Returns the input stream containing the resource data.
	 * 
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		return con.getInputStream();
	}

	public int getResponseCode() {
		return responseCode;
	}

	public long getModified() {
		return con.getLastModified();
	}

	public boolean hasPayload() throws IOException {
		return in != null;
	}

	public URI getUrl() {
		return url;
	}

	public URLConnection getConnection() {
		return con;
	}

	@Override
	public String toString() {
		return "TaggedData [tag=" + getTag() + ", code=" + getResponseCode() + ", modified=" + new Date(getModified())
				+ ", url=" + getUrl() + "]";
	}

	public boolean isOk() {
		return getResponseCode() / 100 == 2;
	}

}
