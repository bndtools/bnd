package org.osgi.service.indexer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.List;
import java.util.jar.Manifest;

/**
 * A resource that is to be indexed.
 */
public interface Resource {
	/** the name of the name attribute */
	static String NAME = "name";

	/** the name of the location attribute */
	static String LOCATION = "location";

	/** the name of the size attribute */
	static String SIZE = "size";

	/** the name of the lastmodified attribute */
	static String LAST_MODIFIED = "lastmodified";

	/**
	 * @return the value for the location attribute
	 */
	String getLocation();

	/**
	 * @return the properties
	 */
	Dictionary<String, Object> getProperties();

	/**
	 * @return the size of the resource
	 */
	long getSize();

	/**
	 * @return an input stream from which the resource can be read
	 * @throws IOException
	 *             when an I/O error occurred
	 */
	InputStream getStream() throws IOException;

	/**
	 * @return the manifest of the resource
	 * @throws IOException
	 *             when an I/O error occurred
	 */
	Manifest getManifest() throws IOException;

	/**
	 * @param prefix
	 *            the prefix of the children that must be returned (typically
	 *            the directory in the JAR)
	 * @return a list of children under the specified prefix (typically the
	 *         entries in the directory)
	 * @throws IOException
	 *             when an I/O error occurred
	 */
	List<String> listChildren(String prefix) throws IOException;

	/**
	 * @param path
	 *            the path of the child (typically the path in the JAR)
	 * @return the child on the specified path, as a resource
	 * @throws IOException
	 *             when an I/O error occurred
	 */
	Resource getChild(String path) throws IOException;

	/**
	 * Close the resource
	 */
	void close();
}
