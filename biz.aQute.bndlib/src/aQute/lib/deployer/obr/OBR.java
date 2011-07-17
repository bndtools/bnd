package aQute.lib.deployer.obr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.xml.sax.SAXException;

import aQute.bnd.build.ResolverMode;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Jar;
import aQute.libg.generics.Create;
import aQute.libg.reporter.Reporter;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class OBR implements Plugin, RepositoryPlugin {
	
	@Deprecated
	public static final String LOCATION = "location";
	public static final String LOCATIONS = "locations";
	
	public static final String CACHE = "cache";
	
	static final String FILE_SCHEME = "file";
	
	boolean initialised = false;
	final Map<String, SortedMap<Version, Resource>> pkgResourceMap = new HashMap<String, SortedMap<Version, Resource>>();
	final Map<String, SortedMap<Version, Resource>> bsnMap = new HashMap<String, SortedMap<Version, Resource>>();
	
	Reporter reporter;
	URL[] locations;
	File cacheDir;

	public void setProperties(Map<String, String> map) {
		String locationsStr = map.get(LOCATIONS);
		
		// backwards compatibility
		if (locationsStr == null) locationsStr = map.get(LOCATION);
		
		try {
			if (locationsStr == null)
				throw new IllegalArgumentException("Location must be set on an OBR plugin");
			locations = parseLocations(locationsStr);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(String.format("Invalid location, unable to parse as URL list: %s", locationsStr), e);
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
					for (Capability capability : resource.getCapabilities()) {
						if (CapabilityType.PACKAGE.getTypeName().equals(capability.getName())) {
							String pkgName = null;
							String versionStr = null;
							
							for (Property prop : capability.getProperties()) {
								if (Property.PACKAGE.equals(prop.getName()))
									pkgName = prop.getValue();
								else if (Property.VERSION.equals(prop.getName()))
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
		List<Resource> resources = narrowVersionsByVersionRange(versionMap, rangeStr);
		List<File> files = mapResourcesToFiles(resources);
		
		return (File[]) files.toArray(new File[files.size()]);
	}
	
	List<Resource> narrowVersionsByFilter(String pkgName, SortedMap<Version, Resource> versionMap, Filter filter) {
		List<Resource> result = new ArrayList<Resource>(versionMap.size());
		
		Dictionary<String, String> dict = new Hashtable<String, String>();
		dict.put("package", pkgName);
		
		for (Version version : versionMap.keySet()) {
			dict.put("version", version.toString());
			if (filter.match(dict))
				result.add(versionMap.get(version));
		}
		
		return result;
	}

	List<Resource> narrowVersionsByVersionRange(SortedMap<Version, Resource> versionMap, String rangeStr) {
		List<Resource> result;
		if ("latest".equals(rangeStr)) {
			Version highest = versionMap.lastKey();
			result = Create.list(new Resource[] { versionMap.get(highest) });
		} else {
			VersionRange range = rangeStr != null ? new VersionRange(rangeStr) : null;
			
			// optimisation: skip versions definitely less than the range
			if (range != null && range.getLow() != null)
				versionMap = versionMap.tailMap(range.getLow());
			
			result = new ArrayList<Resource>(versionMap.size());
			for (Version version : versionMap.keySet()) {
				if (range == null || range.includes(version))
					result.add(versionMap.get(version));
				
				// optimisation: skip versions definitely higher than the range
				if (range != null && range.isRange() && version.compareTo(range.getHigh()) >= 0)
					break;
			}
		}
		return result;
	}
	
	void filterResourcesByResolverMode(Collection<Resource> resources, ResolverMode mode) {
		assert mode != null;
		
		Properties modeCapability = new Properties();
		modeCapability.setProperty(CapabilityType.MODE.getTypeName(), mode.name());
		
		for (Iterator<Resource> iter = resources.iterator(); iter.hasNext(); ) {
			Resource resource = iter.next();
			
			Require modeRequire = resource.findRequire(CapabilityType.MODE.getTypeName());
			if (modeRequire == null)
				continue;
			else if (modeRequire.getFilter() == null)
				iter.remove();
			else {
				try {
					Filter filter = FrameworkUtil.createFilter(modeRequire.getFilter());
					if (!filter.match(modeCapability))
						iter.remove();
				} catch (InvalidSyntaxException e) {
					if (reporter != null)
						reporter.error("Error parsing mode filter requirement on resource %s: %s", resource.getUrl(), modeRequire.getFilter());
					iter.remove();
				}
			}
		}
	}
	
	List<File> mapResourcesToFiles(Collection<Resource> resources) throws Exception {
		List<File> result = new ArrayList<File>(resources.size());
		
		for (Resource resource : resources) {
			result.add(mapResourceToFile(resource));
		}
		
		return result;
	}
	
	File mapResourceToFile(Resource resource) throws Exception {
		return mapUrlToFile(resource.getBaseUrl(), resource.getUrl());
	}

	File mapUrlToFile(String baseUrlStr, String urlStr) throws Exception {
		File result;
		
		URI baseUri = new URI(baseUrlStr);
		URI uri = new URI(urlStr);
		if (FILE_SCHEME.equals(uri.getScheme())) {
			String path = uri.getSchemeSpecificPart();
			if (path.length() > 0 && path.charAt(0) != '/')
				uri = new URI(null, null, path, null);
		}
		uri = baseUri.resolve(uri);
		
		if (FILE_SCHEME.equals(uri.getScheme())) {
			result = new File(uri.getPath());
		} else {
			result = getOrCreateCacheFile(uri);
		}
		
		return result;
	}
	
	File getOrCreateCacheFile(URI uri) throws IOException {
		File result;
		
		ensureCacheDirExists();
		result = mapPath(uri.getPath());
		if (result.exists()) {
			if (!result.isFile())
				throw new IOException(String.format("Cannot create cache file '%s': a directory or other node with that name exists.", result.getAbsolutePath()));
		} else {
			copyToFile(uri, result);
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

	void copyToFile(URI uri, File file) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = uri.toURL().openStream();
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
			String pkgName = properties.get(CapabilityType.PACKAGE.getTypeName());
			
			String modeName = properties.get(CapabilityType.MODE.getTypeName());
			ResolverMode mode = (modeName != null) ? ResolverMode.valueOf(modeName) : null;
			
			if (pkgName != null)
				result = resolvePackage(pkgName, range, strategy, mode, properties);
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

	File resolvePackage(String pkgName, String rangeStr, Strategy strategy, ResolverMode mode, Map<String, String> props) throws Exception {
		init();
		if (rangeStr == null) rangeStr = "0.0.0";
		
		SortedMap<Version, Resource> versionMap = pkgResourceMap.get(pkgName);
		if (versionMap == null)
			return null;
		
		// Was a filter expression supplied?
		Filter filter = null;
		String filterStr = props.get("filter");
		if (filterStr != null) {
			filter = FrameworkUtil.createFilter(filterStr);
		}
		
		// Narrow the resources by version range string or filter.
		List<Resource> resources;
		if (filter != null)
			resources = narrowVersionsByFilter(pkgName, versionMap, filter);
		else
			resources = narrowVersionsByVersionRange(versionMap, rangeStr);
		
		// Remove resources that are invalid for the current resolution mode
		if (mode != null)
			filterResourcesByResolverMode(resources, mode);
		
		// Select the most suitable one
		Resource selected;
		if (resources == null || resources.isEmpty())
			selected = null;
		else {
			switch (strategy) {
			case LOWEST:
				selected = resources.get(0);
				break;
			default:
				selected = resources.get(resources.size() - 1);
			}
			expandPackageUses(pkgName, selected, props);
		}
		return selected != null ? mapResourceToFile(selected) : null;
	}

	void expandPackageUses(String pkgName, Resource resource, Map<String, String> props) {
		List<String> internalUses = new LinkedList<String>();
		Map<String, Require> externalUses = new HashMap<String, Require>();
		
		internalUses.add(pkgName);
		
		Capability capability = resource.findPackageCapability(pkgName);
		Property usesProp = capability.findProperty(Property.USES);
		if (usesProp != null) {
			StringTokenizer tokenizer = new StringTokenizer(usesProp.getValue(), ",");
			while (tokenizer.hasMoreTokens()) {
				String usesPkgName = tokenizer.nextToken();
				Capability usesPkgCap = resource.findPackageCapability(usesPkgName);
				if (usesPkgCap != null)
					internalUses.add(usesPkgName);
				else {
					Require require = resource.findPackageRequire(usesPkgName);
					if (require != null)
						externalUses.put(usesPkgName, require);
				}
			}
		}
		props.put("packages", listToString(internalUses));
		props.put("import-uses", formatPackageRequires(externalUses));
	}
	
	String listToString(List<?> list) {
		StringBuilder builder = new StringBuilder();
		
		int count = 0;
		for (Object item : list) {
			if (count++ > 0) builder.append(',');
			builder.append(item);
		}
		
		return builder.toString();
	}

	String formatPackageRequires(Map<String, Require> externalUses) {
		StringBuilder builder = new StringBuilder();
		
		int count = 0;
		for (Entry<String, Require> entry : externalUses.entrySet()) {
			String pkgName = entry.getKey();
			String filter = entry.getValue().getFilter();

			if (count++ > 0)
				builder.append(',');
			builder.append(pkgName);
			builder.append(";filter='");
			builder.append(filter);
			builder.append('\'');
		}
		
		return builder.toString();
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
