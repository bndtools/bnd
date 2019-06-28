package aQute.bnd.service.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface URLConnector {

	/**
	 * Connect to the specified URL.
	 *
	 * @param url
	 * @throws IOException
	 * @throws Exception
	 */
	InputStream connect(URL url) throws IOException, Exception;

	/**
	 * Connect to the specified URL, also returning the ETag if available.
	 *
	 * @param url The remote URL.
	 * @return An instance of {@link TaggedData}; note that the
	 *         {@link TaggedData#getTag()} method <strong>may</strong> return
	 *         {@code null} if the resource has no tag.
	 * @throws IOException @since 1.1
	 */
	TaggedData connectTagged(URL url) throws Exception;

	/**
	 * Connect to the specified URL while providing the last known tag for the
	 * remote resource; the response will be {@code null} if the remote resource
	 * is unchanged.
	 *
	 * @param url The remote URL.
	 * @param tag The last known tag value for the resource.
	 * @return An instance of {@link TaggedData}, or {@code null} if the
	 *         resource has not modified (i.e., if it has the same tag value).
	 * @throws IOException @since 1.1
	 * @throws Exception
	 */
	TaggedData connectTagged(URL url, String tag) throws Exception;

}
