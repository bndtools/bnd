package aQute.lib.deployer.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import aQute.bnd.service.RepositoryPlugin;

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
 * -plugin: aQute.lib.repository.FixedIndexedRepo;locations='http://www.example.com/repository.xml';cache=${workspace}/.cache
 * </pre>
 * 
 * @author Neil Bartlett
 *
 */
public class FixedIndexedRepo extends AbstractIndexedRepo {
	
	public static final String PROP_LOCATIONS = "locations";
	@Deprecated
	public static final String PROP_CACHE = "cache";

	private String locations;
	protected File cacheDir;

	public void setProperties(Map<String, String> map) {
		super.setProperties(map);
		
		locations = map.get(PROP_LOCATIONS);
		
		String cacheDirStr = map.get(PROP_CACHE);
		if (cacheDirStr != null)
			cacheDir = new File(cacheDirStr);
	}
	
	@Override
	protected List<URL> loadIndexes() throws Exception {
		List<URL> result;
		try {
			if (locations != null)
				result = parseLocations(locations);
			else
				result = Collections.emptyList();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(String.format("Invalid location, unable to parse as URL list: %s", locations), e);
		}
		return result;
	}
	
	// This is still an ugly hack...
	private RepositoryPlugin lookupCacheRepo() {
		if (registry != null) {
			List<RepositoryPlugin> repos = registry.getPlugins(RepositoryPlugin.class);
			for (RepositoryPlugin repo : repos) {
				if ("cache".equals(repo.getName()))
					return repo;
			}
		}
		return null;
	}

	public synchronized File getCacheDirectory() {
		if (cacheDir == null) {
			RepositoryPlugin cacheRepo = lookupCacheRepo();
			if (cacheRepo != null) {
				File temp = new File(cacheRepo.getLocation(), ".obr");
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
		if (name != null && !name.equals(this.getClass().getName()))
			return name;
		
		return locations;
	}

	public String getLocation() {
		if ( locations == null)
			return "[]";
		else
			return locations.toString();
	}

}
