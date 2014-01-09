package aQute.bnd.deployer.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import static aQute.bnd.deployer.repository.RepoConstants.*;

/**
 * A simple read-only OBR-based repository that uses a list of index locations
 * and a basic local cache.
 * <p>
 * <h2>Properties</h2>
 * <ul>
 * <li><b>locations:</b> comma-separated list of index URLs. <b>NB:</b> surround
 * with single quotes!</li>
 * <li><b>name:</b> repository name; defaults to the index URLs.
 * <li><b>cache:</b> local cache directory. May be omitted, in which case the
 * repository will only be able to serve resources with {@code file:} URLs.</li>
 * <li><b>location:</b> (deprecated) alias for "locations".
 * </ul>
 * <p>
 * <h2>Example</h2>
 * 
 * <pre>
 * -plugin: aQute.lib.repository.FixedIndexedRepo;locations='http://www.example.com/repository.xml';cache=${workspace}/.cache
 * </pre>
 * 
 * @author Neil Bartlett
 */
public class FixedIndexedRepo extends AbstractIndexedRepo {

	private static final String	EMPTY_LOCATION	= "";

	public static final String	PROP_LOCATIONS	= "locations";
	public static final String	PROP_CACHE		= "cache";

	private String				locations;
	protected File				cacheDir = new File(System.getProperty("user.home") + File.separator + DEFAULT_CACHE_DIR);

	public synchronized void setProperties(Map<String,String> map) {
		super.setProperties(map);

		locations = map.get(PROP_LOCATIONS);
		String cachePath = map.get(PROP_CACHE);
		if (cachePath != null) {
			cacheDir = new File(cachePath);
			if (!cacheDir.isDirectory())
				try {
					throw new IllegalArgumentException(String.format(
							"Cache path '%s' does not exist, or is not a directory.", cacheDir.getCanonicalPath()));
				}
				catch (IOException e) {
					throw new IllegalArgumentException("Could not get cacheDir canonical path", e);
				}
		}
	}

	@Override
	protected List<URI> loadIndexes() throws Exception {
		List<URI> result;
		try {
			if (locations != null)
				result = parseLocations(locations);
			else
				result = Collections.emptyList();
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException(String.format("Invalid location, unable to parse as URL list: %s",
					locations), e);
		}
		return result;
	}

	public synchronized File getCacheDirectory() {
		return cacheDir;
	}

	public void setCacheDirectory(File cacheDir) {
		if (cacheDir == null)
			throw new IllegalArgumentException("null cache directory not permitted");
		this.cacheDir = cacheDir;
	}

	@Override
	public synchronized String getName() {
		if (name != null && !name.equals(this.getClass().getName()))
			return name;

		return locations;
	}

	public String getLocation() {
		if (locations == null)
			return EMPTY_LOCATION;
		else
			return locations.toString();
	}

}
