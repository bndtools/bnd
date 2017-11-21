package aQute.bnd.deployer.obr;

import static aQute.bnd.deployer.repository.RepoConstants.DEFAULT_CACHE_DIR;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.deployer.repository.AbstractIndexedRepo;
import aQute.bnd.service.Actionable;

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
 *  -plugin:
 * aQute.lib.deployer.obr.OBR;locations='http://www.example.com/repository.xml';
 * cache=${workspace}/.cache
 * </pre>
 * 
 * @author Neil Bartlett
 */
public class OBR extends AbstractIndexedRepo implements Actionable {

	private static final String	EMPTY_LOCATION	= "";

	public static final String	PROP_LOCATIONS	= "locations";
	public static final String	PROP_CACHE		= "cache";

	private String				locations;
	protected File				cacheDir		= new File(
			System.getProperty("user.home") + File.separator + DEFAULT_CACHE_DIR);

	public synchronized void setProperties(Map<String,String> map) {
		map = Conversions.convertConfig(map);
		super.setProperties(map);

		locations = map.get(PROP_LOCATIONS);
		String cachePath = map.get(PROP_CACHE);
		if (cachePath != null) {
			cacheDir = new File(cachePath);
			if (!cacheDir.isDirectory())
				try {
					throw new IllegalArgumentException(String.format(
							"Cache path '%s' does not exist, or is not a directory.", cacheDir.getCanonicalPath()));
				} catch (IOException e) {
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
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(
					String.format("Invalid location, unable to parse as URL list: %s", locations), e);
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

	public void setLocations(String locations) throws MalformedURLException, URISyntaxException {
		parseLocations(locations); // for verification right syntax
		this.locations = locations;
	}

	public File getRoot() {
		return cacheDir;
	}

	/**
	 * Provide a menu to refresh. The target is Repository [, Symbolic Name (String)
	 * [, version (Version)]].
	 */

	@Override
	public Map<String,Runnable> actions(Object... target) throws Exception {
		Map<String,Runnable> map = new HashMap<String,Runnable>();
		map.put("Refresh", new Runnable() {

			@Override
			public void run() {
				try {
					refresh();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		return map;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String title(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
