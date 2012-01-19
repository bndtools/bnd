package aQute.lib.deployer.obr;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.xml.sax.*;

import aQute.bnd.build.*;
import aQute.bnd.service.*;
import aQute.bnd.service.ResourceHandle.Location;
import aQute.bnd.service.url.*;
import aQute.lib.deployer.obr.CachingURLResourceHandle.CachingMode;
import aQute.lib.filter.*;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

/**
 * Abstract base class for OBR-based repositories.
 * 
 * <p>
 * The repository implementation is read-only by default. To implement a writable
 * repository, subclasses should override {@link #canWrite()} and {@link #put(Jar)}.
 * 
 * @author Neil Bartlett
 *
 */
public abstract class AbstractBaseOBR implements RegistryPlugin, Plugin, RemoteRepositoryPlugin, OBRIndexProvider {
	
	public static final String PROP_NAME = "name";
	public static final String PROP_RESOLUTION_MODE = "mode";
	public static final String PROP_RESOLUTION_MODE_ANY = "any";
	
	public static final String REPOSITORY_FILE_NAME = "repository.xml";
	
	protected Registry registry;
	protected Reporter reporter;
	protected String name = this.getClass().getName();
	protected Set<OBRResolutionMode> supportedModes = EnumSet.allOf(OBRResolutionMode.class);

	private boolean initialised = false;
	private final Map<String, SortedMap<Version, Resource>> pkgResourceMap = new HashMap<String, SortedMap<Version, Resource>>();
	private final Map<String, SortedMap<Version, Resource>> bsnMap = new HashMap<String, SortedMap<Version, Resource>>();
	
	protected void addResourceToIndex(Resource resource) {
		addBundleSymbolicNameToIndex(resource);
		addPackagesToIndex(resource);
	}

	protected synchronized void reset() {
		initialised = false;
	}

	/**
	 * Initialize the indexes prior to main initialisation of internal
	 * data structures. This implementation does nothing, but subclasses
	 * may override if they need to perform such initialisation.
	 * @throws Exception 
	 */
	protected void initialiseIndexes() throws Exception {
	}

	protected final synchronized void init() throws Exception {
		if (!initialised) {
			bsnMap.clear();
			pkgResourceMap.clear();
			
			initialiseIndexes();
			
			final URLConnector connector = getConnector();
			IRepositoryListener listener = new IRepositoryListener() {
				public boolean processResource(Resource resource) {
					addResourceToIndex(resource);
					return true;
				}

				public boolean processReferral(String fromUrl, Referral referral, int maxDepth, int currentDepth) {
					try {
						URL indexLocation = new URL(referral.getUrl());
						try {
							CachingURLResourceHandle indexHandle = new CachingURLResourceHandle(indexLocation.toExternalForm(), null, getCacheDirectory(), connector, CachingMode.PreferRemote);
							indexHandle.setReporter(reporter);
							return readIndex(indexLocation.toString(), new FileInputStream(indexHandle.request()), this);
						} catch (Exception e) {
							if (reporter != null) reporter.error("Unable to read referral index at URL '%s' from parent index '%s': %s", indexLocation, fromUrl, e);
						}
						
					} catch (MalformedURLException e) {
						if (reporter != null) reporter.error("Invalid referral URL '%s' from parent index '%s': %s", referral.getUrl(), fromUrl, e);
					}
					return false;
				}
			};
			
			Collection<URL> indexes = getOBRIndexes();
			for (URL indexLocation : indexes) {
				try {
					CachingURLResourceHandle indexHandle = new CachingURLResourceHandle(indexLocation.toExternalForm(), null, getCacheDirectory(), connector, CachingMode.PreferRemote);
					indexHandle.setReporter(reporter);
					File indexFile = indexHandle.request();
					readIndex(indexLocation.toExternalForm(), new FileInputStream(indexFile), listener);
				} catch (Exception e) {
					if (reporter != null) reporter.error("Unable to read index at URL '%s': %s", indexLocation, e);
				}
			}
			
			initialised = true;
		}
	}
	
	private URLConnector getConnector() {
		URLConnector connector;
		synchronized (this) {
			connector = registry != null ? registry.getPlugin(URLConnector.class) : null;
		}
		if (connector == null)
			connector = new DefaultURLConnector();
		return connector;
	}

	public synchronized final void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public synchronized void setProperties(Map<String, String> map) {
		if (map.containsKey(PROP_NAME))
			name = map.get(PROP_NAME);
		
		if (map.containsKey(PROP_RESOLUTION_MODE)) {
			supportedModes = EnumSet.noneOf(OBRResolutionMode.class);
			StringTokenizer tokenizer = new StringTokenizer(map.get(PROP_RESOLUTION_MODE), ",");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				if (PROP_RESOLUTION_MODE_ANY.equalsIgnoreCase(token))
					supportedModes = EnumSet.allOf(OBRResolutionMode.class);
				else {
					try {
						supportedModes.add(OBRResolutionMode.valueOf(token));
					} catch (Exception e) {
						if (reporter != null) reporter.error("Unknown OBR resolution mode: " + token);
					}
				}
			}
		}
	}
	
	public File[] get(String bsn, String range) throws Exception {
		ResourceHandle[] handles = getHandles(bsn, range);
		
		return requestAll(handles);
	}
	
	protected static File[] requestAll(ResourceHandle[] handles) throws IOException {
		File[] result = (handles == null) ? new File[0] : new File[handles.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = handles[i].request();
		}
		return result;
	}

	protected ResourceHandle[] getHandles(String bsn, String rangeStr) throws Exception {
		init();
		
		// If the range is set to "project", we cannot resolve it.
		if ("project".equals(rangeStr))
			return null;
		
		
		SortedMap<Version, Resource> versionMap = bsnMap.get(bsn);
		if (versionMap == null || versionMap.isEmpty())
			return null;
		List<Resource> resources = narrowVersionsByVersionRange(versionMap, rangeStr);
		List<ResourceHandle> handles = mapResourcesToHandles(resources);
		
		return (ResourceHandle[]) handles.toArray(new ResourceHandle[handles.size()]);
	}
	
	public synchronized void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}
	
	public File get(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
		ResourceHandle handle = getHandle(bsn, range, strategy, properties);
		return handle != null ? handle.request() : null;
	}
	
	public ResourceHandle getHandle(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
		ResourceHandle result;
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
		List<Version> list;
		if (versionMap != null) {
			list = new ArrayList<Version>(versionMap.size());
			list.addAll(versionMap.keySet());
		} else {
			list = Collections.emptyList();
		}
		return list;
	}

	public synchronized String getName() {
		return name;
	}

	void addBundleSymbolicNameToIndex(Resource resource) {
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
	}

	void addPackagesToIndex(Resource resource) {
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
	}

	/**
	 * @return Whether to continue parsing other indexes
	 * @throws IOException 
	 */
	boolean readIndex(String baseUrl, InputStream stream, IRepositoryListener listener) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		try {
			parser.parse(stream, new OBRSAXHandler(baseUrl, listener));
			return true;
		} catch (StopParseException e) {
			return false;
		} finally {
			stream.close();
		}
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
					Filter filter = new Filter(modeRequire.getFilter());
					if (!filter.match(modeCapability))
						iter.remove();
				} catch (IllegalArgumentException e) {
					synchronized (this) {
						if (reporter != null) reporter.error("Error parsing mode filter requirement on resource %s: %s", resource.getUrl(), modeRequire.getFilter());
					}
					iter.remove();
				}
			}
		}
	}
	
	List<ResourceHandle> mapResourcesToHandles(Collection<Resource> resources) throws Exception {
		List<ResourceHandle> result = new ArrayList<ResourceHandle>(resources.size());
		
		for (Resource resource : resources) {
			ResourceHandle handle = mapResourceToHandle(resource);
			if (handle != null)
				result.add(handle);
		}
		
		return result;
	}
	
	ResourceHandle mapResourceToHandle(Resource resource) throws Exception {
		ResourceHandle result = null;
		
		CachingURLResourceHandle handle ;
		try {
			handle = new CachingURLResourceHandle(resource.getUrl(), resource.getBaseUrl(), getCacheDirectory(), getConnector(), CachingMode.PreferCache);
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Broken link in repository index: " + e.getMessage());
		}
		if (handle.getLocation() == Location.local || getCacheDirectory() != null)
			result = handle;
		
		return result;
	}

	ResourceHandle resolveBundle(String bsn, String rangeStr, Strategy strategy) throws Exception {
		if (rangeStr == null) rangeStr = "0.0.0";
		
		if (strategy == Strategy.EXACT) {
			return findExactMatch(bsn, rangeStr, bsnMap);
		}
		
		ResourceHandle[] handles = getHandles(bsn, rangeStr);
		ResourceHandle selected;
		if (handles == null || handles.length == 0)
			selected = null;
		else {
			switch(strategy) {
			case LOWEST:
				selected = handles[0];
				break;
			default:
				selected = handles[handles.length - 1];
			}
		}
		return selected;
	}

	ResourceHandle resolvePackage(String pkgName, String rangeStr, Strategy strategy, ResolverMode mode, Map<String, String> props) throws Exception {
		init();
		if (rangeStr == null) rangeStr = "0.0.0";
		
		SortedMap<Version, Resource> versionMap = pkgResourceMap.get(pkgName);
		if (versionMap == null)
			return null;
		
		// Was a filter expression supplied?
		Filter filter = null;
		String filterStr = props.get("filter");
		if (filterStr != null) {
			filter = new Filter(filterStr);
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
		return selected != null ? mapResourceToHandle(selected) : null;
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

	ResourceHandle findExactMatch(String identity, String version, Map<String, SortedMap<Version, Resource>> resourceMap) throws Exception {
		Resource resource;
		VersionRange range = new VersionRange(version);
		if (range.isRange())
			return null;
		
		SortedMap<Version, Resource> versions = resourceMap.get(identity);
		resource = versions.get(range.getLow());
		
		return mapResourceToHandle(resource);
	}
	
	/**
	 * Utility function for parsing lists of URLs.
	 * 
	 * @param locationsStr
	 *            Comma-separated list of URLs
	 * @throws MalformedURLException
	 */
	protected static List<URL> parseLocations(String locationsStr) throws MalformedURLException {
		StringTokenizer tok = new StringTokenizer(locationsStr, ",");
		List<URL> urls = new ArrayList<URL>(tok.countTokens());
		while (tok.hasMoreTokens()) {
			String urlStr = tok.nextToken().trim();
			urls.add(new URL(urlStr));
		}
		return urls;
	}

	public Set<OBRResolutionMode> getSupportedModes() {
		return supportedModes;
	}

	public String toString() {
		return getName();
	}
}
