package aQute.bnd.service.url;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Date;

/**
 * Represents a data stream that has a tag associated with it; the primary
 * use-case is an HTTP response stream with an ETag header.
 * 
 * @author Neil Bartlett
 */
public class TaggedData {

	private final String		tag;
	private final InputStream	inputStream;
	private final int			code;
	private final long			modified;
	private final URI			url;

	public TaggedData(String tag, InputStream inputStream, int responseCode, long modified, URI url) {
		this.tag = tag;
		this.inputStream = inputStream;
		this.code = responseCode;
		this.modified = modified;
		this.url = url;
	}

	public TaggedData(String tag, InputStream inputStream, int responseCode) {
		this(tag, inputStream, responseCode, 0, null);
	}
	public TaggedData(String tag, InputStream inputStream) {
		this(tag, inputStream, HttpURLConnection.HTTP_OK, 0, null);
	}

	/**
	 * Returns the ETag for the retrieved resource, or {@code null} if the ETag
	 * was not provided by the server.
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Returns the input stream containing the resource data.
	 */
	public InputStream getInputStream() {
		return inputStream;
	}

	public int getResponseCode() {
		return code;
	}

	public long getModified() {
		return modified;
	}

	public boolean hasPayload() {
		return inputStream != null;
	}

	public URI getUrl() {
		return this.url;
	}

	@Override
	public String toString() {
		return "TaggedData [tag=" + tag + ", code=" + code + ", modified=" + new Date(modified) + ", url=" + url + "]";
	}

}
