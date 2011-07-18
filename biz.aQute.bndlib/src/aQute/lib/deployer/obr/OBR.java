package aQute.lib.deployer.obr;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A simple read-only OBR-based repository that uses a list of index locations
 * and a basic local cache.
 * 
 * <p>
 * <h2>Properties</h2>
 * <ul>
 * <li><b>locations:</b> comma-separated list of index URLs. <b>NB:</b> surround with single quotes!</li>
 * <li><b>name:</b> repository name; defaults to the index URLs.
 * <li><b>cache:</b> local cache directory. May be omitted, in which case the repository will only be
 * able to serve resources with {@code file:} URLs.</li>
 * <li><b>location:</b> (deprecated) alias for "locations".
 * </ul>
 * 
 * <p>
 * <h2>Example</h2>
 * 
 * <pre>
 * -plugin: aQute.lib.deployer.obr.OBR;locations='http://www.example.com/repository.xml';cache=${workspace}/.cache
 * </pre>
 * 
 * @author Neil Bartlett
 *
 */
public class OBR extends AbstractBaseOBR {
	
	public static final String PROP_LOCATIONS = "locations";
	@Deprecated
	public static final String PROP_LOCATION = "location";
	public static final String PROP_CACHE = "cache";

	protected List<URL> locations;
	protected File cacheDir;
	
	public void setProperties(Map<String, String> map) {
		super.setProperties(map);
		
		String locationsStr = map.get(PROP_LOCATIONS);
		// backwards compatibility
		if (locationsStr == null) locationsStr = map.get(PROP_LOCATION);
		
		try {
			if (locationsStr != null)
				locations = parseLocations(locationsStr);
			else
				locations = Collections.emptyList();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(String.format("Invalid location, unable to parse as URL list: %s", locationsStr), e);
		}
		
		String cacheDirStr = map.get(PROP_CACHE);
		cacheDir = cacheDirStr != null ? new File(cacheDirStr) : null;
	}
	
	public List<URL> getOBRIndexes() {
		return locations;
	}
	
	void ensureCacheDirExists() throws IOException {
		assert cacheDir != null;
		
		if (cacheDir.exists()) {
			if (!cacheDir.isDirectory())
				throw new IOException(String.format("Cannot create cache directory '%s' because a file or special node with that name exists.", cacheDir.getAbsolutePath()));
		} else {
			if (!cacheDir.mkdirs())
				throw new IOException(String.format("Failed to create cache directory '%s'.", cacheDir.getAbsolutePath()));
		}
	}
	
	@Override
	protected File getOrCreateCacheFile(URI uri) throws IOException {
		File result;
		
		ensureCacheDirExists();
		result = mapPath(uri.getPath());
		if (result.exists()) {
			if (!result.isFile())
				throw new IOException(String.format("Cannot create cache file '%s': a directory or other node with that name exists.", result.getAbsolutePath()));
		} else {
			downloadToFile(uri, result);
		}
		return result;
	}
	
	private File mapPath(String path) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		path = path.replace('/', '_');
		return new File(cacheDir, path);
	}


	@Override
	public String getName() {
		if (name != null && name != this.getClass().getName())
			return name;
		
		StringBuilder builder = new StringBuilder();
		
		int count = 0;
		for (URL location : locations) {
			if (count++ > 0 ) builder.append(',');
			builder.append(location);
		}
		return builder.toString();
	}

	public void setLocations(URL[] urls) {
		this.locations = Arrays.asList(urls);
	}

	public File getCacheDir() {
		return cacheDir;
	}
	
	public void setCacheDir(File cacheDir) {
		this.cacheDir = cacheDir;
	}
}
