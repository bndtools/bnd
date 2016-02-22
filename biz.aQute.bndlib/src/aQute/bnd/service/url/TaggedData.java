package aQute.bnd.service.url;

import java.io.InputStream;
import java.net.HttpURLConnection;

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

	public TaggedData(String tag, InputStream inputStream, int responseCode) {
		this.tag = tag;
		this.inputStream = inputStream;
		this.code = responseCode;
	}

	public TaggedData(String tag, InputStream inputStream) {
		this(tag, inputStream, HttpURLConnection.HTTP_OK);
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

}
