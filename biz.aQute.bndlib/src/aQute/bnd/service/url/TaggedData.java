package aQute.bnd.service.url;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.Date;

import javax.xml.ws.http.HTTPException;

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
	private final File			file;

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
		this(con, in, null);
	}
	public TaggedData(URLConnection con, InputStream in, File file) throws Exception {
		this.con = con;
		this.responseCode = con instanceof HttpURLConnection ? ((HttpURLConnection) con).getResponseCode() : -1;
		this.in = in == null && con != null && (responseCode / 100 == 2) ? con.getInputStream() : in;
		this.file = file;
		this.etag = con.getHeaderField("ETag");
		this.url = con.getURL().toURI();
	}

	public TaggedData(URI url, int responseCode, File file) throws Exception {
		this.file = file;
		this.con = null;
		this.in = null;
		this.etag = "";
		this.responseCode = responseCode;
		this.url = url;
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
		return in;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public long getModified() {
		if (con != null)
			return con.getLastModified();
		return -1;
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
				+ ", url=" + getUrl() + ", state=" + getState() + "]";
	}

	public boolean isOk() {
		return getResponseCode() / 100 == 2;
	}

	public boolean isNotModified() {
		return responseCode == HttpURLConnection.HTTP_NOT_MODIFIED;
	}

	public void throwIt() {
		throw new HTTPException(responseCode);
	}

	public State getState() {
		if (isNotFound())
			return State.NOT_FOUND;
		if (isNotModified())
			return State.UNMODIFIED;
		if (isOk())
			return State.UPDATED;

		return State.OTHER;
	}

	public boolean isNotFound() {
		return responseCode == 404;
	}

	public File getFile() {
		return file;
	}
}
