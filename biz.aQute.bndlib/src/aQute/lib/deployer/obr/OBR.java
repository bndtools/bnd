package aQute.lib.deployer.obr;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.lib.deployer.*;

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
		if (cacheDirStr != null)
			cacheDir = new File(cacheDirStr);
	}
	
	private FileRepo lookupCachedFileRepo() {
		if (registry != null) {
			List<FileRepo> repos = registry.getPlugins(FileRepo.class);
			for (FileRepo repo : repos) {
				if ("cache".equals(repo.getName()))
					return repo;
			}
		}
		return null;
	}

	public List<URL> getOBRIndexes() {
		return locations;
	}

	public synchronized File getCacheDirectory() {
		if (cacheDir == null) {
			FileRepo cacheRepo = lookupCachedFileRepo();
			if (cacheRepo != null) {
				File temp = new File(cacheRepo.getRoot(), ".obr");
				temp.mkdirs();
				if (temp.exists())
					cacheDir = temp;
			}
		}
		return cacheDir;
	}
	
	public void setCacheDirectory(File cacheDir) {
		this.cacheDir = cacheDir;
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

	public String getLocation() {
		if ( locations == null)
			return "[]";
		else
			return locations.toString();
	}

}
