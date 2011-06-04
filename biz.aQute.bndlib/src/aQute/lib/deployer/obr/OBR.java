package aQute.lib.deployer.obr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import aQute.bnd.service.Plugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.libg.reporter.Reporter;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class OBR implements Plugin, RepositoryPlugin {
	
	public static final String LOCATION = "location";
	public static final String CACHE = "cache";
	
	static final String FILE_URI_PREFIX = "file:";
	
	boolean initialised = false;
	final Map<String, SortedMap<Version, Resource>> pkgResourceMap = new HashMap<String, SortedMap<Version, Resource>>();
	final Map<String, SortedMap<Version, Resource>> bsnMap = new HashMap<String, SortedMap<Version, Resource>>();
	
	Reporter reporter;
	URL[] locations;
	File cacheDir;

	public void setProperties(Map<String, String> map) {
		try {
			String locationsStr = map.get(LOCATION);
			if (locationsStr == null)
				throw new IllegalArgumentException("Location must be set of an OBR plugin");
			locations = parseLocations(locationsStr);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid location property", e);
		}
		
		String cacheDirStr = map.get(CACHE);
		cacheDir = cacheDirStr != null ? new File(cacheDirStr) : null;
	}
	
	URL[] parseLocations(String locationsStr) throws MalformedURLException {
		List<URL> urls = new ArrayList<URL>();
		
		StringTokenizer tok = new StringTokenizer(locationsStr, ",");
		while (tok.hasMoreTokens()) {
			String urlStr = tok.nextToken().trim();
			urls.add(new URL(urlStr));
		}
		
		return (URL[]) urls.toArray(new URL[urls.size()]);
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}
	
	public void setLocations(URL[] urls) {
		this.locations = urls;
	}
	
	public URL[] getLocations() {
		return locations;
	}
	
	public void setCacheDir(File cacheDir) {
		this.cacheDir = cacheDir;
	}
	
	public File getCacheDir() {
		return cacheDir;
	}

	
	synchronized void reset() {
		initialised = false;
	}
	
	synchronized void init() {
		if (!initialised) {
			bsnMap.clear();
			IResourceListener bsnMapper = new IResourceListener() {
				public boolean processResource(Resource resource) {
					String bsn = resource.getSymbolicName();
					
					Version version;
					String versionStr = resource.getVersion();
					try {
						version = new Version(versionStr);
					} catch (Exception e) {
						version = new Version("0.0.0");
					}
					
					SortedMap<Version, Resource> versionMap = bsnMap.get(bsn);
					if (versionMap == null) {
						versionMap = new TreeMap<Version, Resource>();
						bsnMap.put(bsn, versionMap);
					}
					versionMap.put(version, resource);
					
					return true;
				}
			};
			
			pkgResourceMap.clear();
			IResourceListener pkgMapper = new IResourceListener() {
				public boolean processResource(Resource resource) {
					List<Capability> capabilities = resource.getCapabilities();
					for (Capability capability : capabilities) {
						if (CapabilityType.PACKAGE.getTypeName().equals(capability.getName())) {
							String pkgName = null;
							String versionStr = null;
							
							for (Property prop : capability.getProperties()) {
								if (CapabilityType.PACKAGE.getTypeName().equals(prop.getName()))
									pkgName = prop.getValue();
								else if (Constants.VERSION_ATTRIBUTE.equals(prop.getName()))
									versionStr = prop.getValue();
							}
							
							Version version;
							try {
								version = new Version(versionStr);
							} catch (Exception e) {
								version = new Version("0.0.0");
							}
							
							if (pkgName != null) {
								SortedMap<Version, Resource> versionMap = pkgResourceMap.get(pkgName);
								if (versionMap == null) {
									versionMap = new TreeMap<Version, Resource>();
									pkgResourceMap.put(pkgName, versionMap);
								}
								versionMap.put(version, resource);
							}
						}
					}
					return true;
				}
			};
			
			IResourceListener[] listeners = new IResourceListener[] { bsnMapper, pkgMapper };
			
			if (locations != null) for (URL location : locations) {
				try {
					InputStream stream = location.openStream();
					readIndex(location.toString(), stream, listeners);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			initialised = true;
		}
	}
	
	/**
	 * @return Whether to continue parsing other indexes
	 * @throws IOException 
	 */
	boolean readIndex(String baseUrl, InputStream stream, IResourceListener[] listeners) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		try {
			parser.parse(stream, new OBRSAXHandler(baseUrl, listeners));
			return true;
		} catch (StopParseException e) {
			return false;
		} finally {
			stream.close();
		}
	}

	public File[] get(String bsn, String rangeStr) throws Exception {
		init();
		
		// If the range is set to "project", we cannot resolve it.
		if ("project".equals(rangeStr))
			return null;
		
		
		SortedMap<Version, Resource> versionMap = bsnMap.get(bsn);
		if (versionMap == null || versionMap.isEmpty())
			return null;
		Resource[] resources = narrowVersions(versionMap, rangeStr);
		
		List<File> files = mapResourcesToFiles(resources);
		return (File[]) files.toArray(new File[files.size()]);
	}

	Resource[] narrowVersions(SortedMap<Version, Resource> versionMap, String rangeStr) {
		Resource[] resources;
		if ("latest".equals(rangeStr)) {
			Version highest = versionMap.lastKey();
			resources = new Resource[] { versionMap.get(highest) };
		} else {
			VersionRange range = rangeStr != null ? new VersionRange(rangeStr) : null;
			
			// optimisation: skip versions definitely less than the range
			if (range != null && range.getLow() != null)
				versionMap = versionMap.tailMap(range.getLow());
			
			List<Resource> matched = new ArrayList<Resource>(versionMap.size());
			for (Version version : versionMap.keySet()) {
				if (range == null || range.includes(version))
					matched.add(versionMap.get(version));
				
				// optimisation: skip versions definitely higher than the range
				if (range != null && range.isRange() && version.compareTo(range.getHigh()) >= 0)
					break;
			}
			resources = (Resource[]) matched.toArray(new Resource[matched.size()]);
		}
		return resources;
	}
	
	List<File> mapResourcesToFiles(Resource[] resources) throws Exception {
		List<File> list = new ArrayList<File>(resources.length);
		for (Resource resource : resources) {
			File file = mapResourceToFile(resource);
			if (file != null) list.add(file);
		}
		return list;
	}
	
	File mapResourceToFile(Resource resource) throws Exception {
		File result;
		
		String urlStr = resource.getUrl();
		if (urlStr.startsWith(FILE_URI_PREFIX)) {
			String path = urlStr.substring(FILE_URI_PREFIX.length());
			if (path.length() > 0 && path.charAt(0) != '/') {
				String baseUrlStr = resource.getBaseUrl();
				if (baseUrlStr != null && baseUrlStr.startsWith(FILE_URI_PREFIX)) {
					File baseFile = new File(baseUrlStr.substring(FILE_URI_PREFIX.length()));
					File baseDir = baseFile.getParentFile();
					result = new File(baseDir, path);
				} else {
					result = new File(path);
				}
			} else {
				result = new File(path);
			}
		} else {
			URL url = new URL(urlStr);
			result = getOrCreateCacheFile(url);
		}
		
		return result;
	}

	File getOrCreateCacheFile(URL url) throws IOException {
		File result;
		ensureCacheDirExists();
		result = mapPath(url.getFile());
		if (result.exists()) {
			if (!result.isFile())
				throw new IOException(String.format("Cannot create cache file '%s': a directory or other node with that name exists.", result.getAbsolutePath()));
		} else {
			copyToFile(url, result);
		}
		return result;
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

	void copyToFile(URL url, File file) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = url.openStream();
			out = new FileOutputStream(file);
			
			byte[] buf = new byte[1024];
			for(;;) {
				int bytes = in.read(buf, 0, 1024);
				if (bytes < 0) break;
				out.write(buf, 0, bytes);
			}
		} finally {
			try { if (in != null) in.close(); } catch (IOException e) {};
			try { if (out != null) in.close(); } catch (IOException e) {};
		}
	}

	File mapPath(String path) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		path = path.replace('/', '_');
		return new File(cacheDir, path);
	}
	

	public File get(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
		File result;
		if (bsn != null)
			result = resolveBundle(bsn, range, strategy);
		else {
			String pkgName = properties.get("package");
			if (pkgName != null)
				result = resolvePackage(pkgName, range, strategy);
			else
				throw new IllegalArgumentException("Cannot resolve bundle: neither bsn nor package specified.");
		}
		return result;
	}

	File resolveBundle(String bsn, String rangeStr, Strategy strategy) throws Exception {
		if (rangeStr == null) rangeStr = "0.0.0";
		
		if (strategy == Strategy.EXACT) {
			return findExactMatch(bsn, rangeStr, bsnMap);
		}
		
		File[] files = get(bsn, rangeStr);
		File selected;
		if (files == null || files.length == 0)
			selected = null;
		else {
			switch(strategy) {
			case LOWEST:
				selected = files[0];
				break;
			default:
				selected = files[files.length - 1];
			}
		}
		return selected;
	}

	File resolvePackage(String pkgName, String rangeStr, Strategy strategy) throws Exception {
		init();
		if (rangeStr == null) rangeStr = "0.0.0";
		
		SortedMap<Version, Resource> versionMap = pkgResourceMap.get(pkgName);
		if (versionMap == null)
			return null;
		
		Resource[] resources = narrowVersions(versionMap, rangeStr);
		List<File> files = mapResourcesToFiles(resources);
		
		File selected;
		if (files == null || files.isEmpty())
			selected = null;
		else {
			switch (strategy) {
			case LOWEST:
				selected = files.get(0);
				break;
			default:
				selected = files.get(files.size() - 1);
			}
		}
		return selected;
	}

	File findExactMatch(String identity, String version, Map<String, SortedMap<Version, Resource>> resourceMap) throws Exception {
		Resource resource;
		VersionRange range = new VersionRange(version);
		if (range.isRange()) return null;
		
		SortedMap<Version, Resource> versions = resourceMap.get(identity);
		resource = versions.get(range.getLow());
		
		return mapResourceToFile(resource);
	}
	
	public boolean canWrite() {
		return false;
	}

	public File put(Jar jar) throws Exception {
		throw new UnsupportedOperationException("Read-only repository.");
	}

	public List<String> list(String regex) throws Exception {
		init();
		Pattern pattern = regex != null ? Pattern.compile(regex) : null;
		List<String> result = new LinkedList<String>();
		
		for (String bsn : bsnMap.keySet()) {
			if (pattern == null || pattern.matcher(bsn).matches())
				result.add(bsn);
		}
		
		return result;
	}

	public List<Version> versions(String bsn) throws Exception {
		init();
		SortedMap<Version, Resource> versionMap = bsnMap.get(bsn);
		List<Version> list = new ArrayList<Version>(versionMap.size());
		list.addAll(versionMap.keySet());
		return list;
	}

	public String getName() {
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < locations.length; i++) {
			if (i > 0) builder.append(',');
			builder.append(locations[i].toString());
		}
		
		return builder.toString();
	}

}
