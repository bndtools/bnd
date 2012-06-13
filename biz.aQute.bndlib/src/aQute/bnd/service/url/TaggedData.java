package aQute.bnd.service.url;

import java.io.*;

/**
 * Represents a data stream that has a tag associated with it; the primary
 * use-case is an HTTP response stream with an ETag header.
 * 
 * @author Neil Bartlett
 */
public class TaggedData {

	private final String		tag;
	private final InputStream	inputStream;

	public TaggedData(String tag, InputStream inputStream) {
		this.tag = tag;
		this.inputStream = inputStream;
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

}
