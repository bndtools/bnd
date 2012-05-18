package aQute.lib.deployer.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.osgi.service.log.LogService;

import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RemoteRepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.service.ResourceHandle;
import aQute.bnd.service.ResourceHandle.Location;
import aQute.bnd.service.url.URLConnector;
import aQute.lib.deployer.http.DefaultURLConnector;
import aQute.lib.deployer.repository.CachingURLResourceHandle.CachingMode;
import aQute.lib.deployer.repository.api.BaseResource;
import aQute.lib.deployer.repository.api.CheckResult;
import aQute.lib.deployer.repository.api.Decision;
import aQute.lib.deployer.repository.api.IRepositoryContentProvider;
import aQute.lib.deployer.repository.api.IRepositoryListener;
import aQute.lib.deployer.repository.api.Referral;
import aQute.lib.deployer.repository.providers.ObrContentProvider;
import aQute.lib.deployer.repository.providers.R5RepoContentProvider;
import aQute.lib.filter.Filter;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;
import aQute.libg.generics.Create;
import aQute.libg.gzip.GZipUtils;
import aQute.libg.reporter.Reporter;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

/**
 * Abstract base class for indexed repositories.
 * 
 * <p>
 * The repository implementation is read-only by default. To implement a writable
 * repository, subclasses should override {@link #canWrite()} and {@link #put(Jar)}.
 * 
 * @author Neil Bartlett
 *
 */
public abstract class AbstractIndexedRepo implements RegistryPlugin, Plugin, RemoteRepositoryPlugin, IndexProvider {
	
	public static final String PROP_NAME = "name";
	public static final String PROP_REPO_TYPE = "type";
	public static final String PROP_RESOLUTION_PHASE = "phase";
	public static final String PROP_RESOLUTION_PHASE_ANY = "any";
	
	public static final String REPO_TYPE_R5  = R5RepoContentProvider.NAME;
	public static final String REPO_TYPE_OBR = ObrContentProvider.NAME;
	
	private   final Map<String, IRepositoryContentProvider> allContentProviders = new HashMap<String, IRepositoryContentProvider>(5);
	protected final List<IRepositoryContentProvider> generatingProviders = new LinkedList<IRepositoryContentProvider>();
	
	protected Registry registry;
	protected Reporter reporter;
	protected LogService logService = new NullLogService();
	protected String name = this.getClass().getName();
	protected Set<ResolutionPhase> supportedPhases = EnumSet.allOf(ResolutionPhase.class);
	
	private List<URL> indexLocations;

	private String requestedContentProviderList = null;
	
	private boolean initialised = false;

	private final Map<String, SortedMap<Version, BaseResource>> bsnMap = new HashMap<String, SortedMap<Version, BaseResource>>();
	
	
	protected AbstractIndexedRepo() {
		allContentProviders.put(REPO_TYPE_R5, new R5RepoContentProvider());
		allContentProviders.put(REPO_TYPE_OBR, new ObrContentProvider());
		
		generatingProviders.add(allContentProviders.get(REPO_TYPE_R5));
	}
	
	protected void addResourceToIndex(BaseResource resource) {
		addBundleSymbolicNameToIndex(resource);
	}

	public synchronized void reset() {
		initialised = false;
	}
	
	private synchronized void clear() {
		bsnMap.clear();
	}
	

	protected abstract List<URL> loadIndexes() throws Exception;

	protected synchronized void loadAllContentProviders() {
		if (registry == null)
			return;

		List<IRepositoryContentProvider> extraProviders = registry.getPlugins(IRepositoryContentProvider.class);

		for (IRepositoryContentProvider provider : extraProviders) {
			String providerName = provider.getName();
			if (allContentProviders.containsKey(providerName)) {
				if (reporter != null) reporter.warning("Repository content provider with name \"%s\" is already registered.", providerName);
			} else {
				allContentProviders.put(providerName, provider);
			}
		}
	}
	
	protected final synchronized void init() throws Exception {
		if (!initialised) {
			clear();
			
			// Load the request repository content providers, if specified
			if (requestedContentProviderList != null && requestedContentProviderList.length() > 0) {
				generatingProviders.clear();
				
				// Load the available providers from the workspace plugins.
				loadAllContentProviders();
				
				// Find the requested providers from the available ones.
				StringTokenizer tokenizer = new StringTokenizer(requestedContentProviderList, "|");
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken().trim();
					IRepositoryContentProvider provider = allContentProviders.get(token);
					if (provider == null) {
						if (reporter != null) reporter.warning("Unknown repository content provider \"%s\".", token);
					} else {
						generatingProviders.add(provider);
					}
				}
				if (generatingProviders.isEmpty()) {
					if (reporter != null)
						reporter.warning("No valid repository index generators were found, requested list was: [%s]", requestedContentProviderList);
				}
			}

			// Initialise index locations
			indexLocations = loadIndexes();

			// Create the callback for new referral and resource objects
			final URLConnector connector = getConnector();
			IRepositoryListener listener = new IRepositoryListener() {

				public void processResource(BaseResource resource) {
					addResourceToIndex(resource);
				}

				public void processReferral(String fromUrl, Referral referral, int maxDepth, int currentDepth) {
					try {
						URL indexLocation = new URL(referral.getUrl());
						try {
							CachingURLResourceHandle indexHandle = new CachingURLResourceHandle(indexLocation.toExternalForm(), null, getCacheDirectory(), connector, CachingMode.PreferRemote);
							indexHandle.setReporter(reporter);
							readIndex(indexLocation.getFile(), indexLocation.toString(), new FileInputStream(indexHandle.request()), this);
						} catch (Exception e) {
							if (reporter != null) reporter.error("Unable to read referral index at URL '%s' from parent index '%s': %s", indexLocation, fromUrl, e);
						}
						
					} catch (MalformedURLException e) {
						if (reporter != null) reporter.error("Invalid referral URL '%s' from parent index '%s': %s", referral.getUrl(), fromUrl, e);
					}
				}

			};

			// Parse the indexes
			for (URL indexLocation : indexLocations) {
				try {
					CachingURLResourceHandle indexHandle = new CachingURLResourceHandle(indexLocation.toExternalForm(), null, getCacheDirectory(), connector, CachingMode.PreferRemote);
					indexHandle.setReporter(reporter);
					File indexFile = indexHandle.request();
					InputStream indexStream = GZipUtils.detectCompression(new WrappingStream(new FileInputStream(indexFile)));
					readIndex(indexFile.getName(), indexLocation.toExternalForm(), indexStream, listener);
				} catch (Exception e) {
					e.printStackTrace();
					if (reporter != null)
						reporter.error("Unable to read index at URL '%s': %s", indexLocation, e);
				}
			}
			
			initialised = true;
		}
	}
	
	public final List<URL> getIndexLocations() throws Exception {
		init();
		return Collections.unmodifiableList(indexLocations);
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
		
		if (map.containsKey(PROP_RESOLUTION_PHASE)) {
			supportedPhases = EnumSet.noneOf(ResolutionPhase.class);
			StringTokenizer tokenizer = new StringTokenizer(map.get(PROP_RESOLUTION_PHASE), ",");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				if (PROP_RESOLUTION_PHASE_ANY.equalsIgnoreCase(token))
					supportedPhases = EnumSet.allOf(ResolutionPhase.class);
				else {
					try {
						supportedPhases.add(ResolutionPhase.valueOf(token));
					} catch (Exception e) {
						if (reporter != null) reporter.error("Unknown OBR resolution mode: " + token);
					}
				}
			}
		}
		
		requestedContentProviderList = map.get(PROP_REPO_TYPE);
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
		
		
		SortedMap<Version, BaseResource> versionMap = bsnMap.get(bsn);
		if (versionMap == null || versionMap.isEmpty())
			return null;
		List<BaseResource> resources = narrowVersionsByVersionRange(versionMap, rangeStr);
		List<ResourceHandle> handles = mapResourcesToHandles(resources);
		
		return (ResourceHandle[]) handles.toArray(new ResourceHandle[handles.size()]);
	}
	
	public synchronized void setReporter(Reporter reporter) {
		this.reporter = reporter;
		this.logService = new ReporterLogService(reporter);
	}
	
	public File get(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
		ResourceHandle handle = getHandle(bsn, range, strategy, properties);
		return handle != null ? handle.request() : null;
	}
	
	public ResourceHandle getHandle(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
		init();
		ResourceHandle result;
		if (bsn != null)
			result = resolveBundle(bsn, range, strategy);
		else {
			throw new IllegalArgumentException("Cannot resolve bundle: bundle symbolic name not specified.");
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
		SortedMap<Version, BaseResource> versionMap = bsnMap.get(bsn);
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

	void addBundleSymbolicNameToIndex(BaseResource resource) {
		String bsn = resource.getIdentity();
		Version version;
		String versionStr = resource.getVersion();
		try {
			version = new Version(versionStr);
		} catch (Exception e) {
			version = new Version("0.0.0");
		}
		SortedMap<Version, BaseResource> versionMap = bsnMap.get(bsn);
		if (versionMap == null) {
			versionMap = new TreeMap<Version, BaseResource>();
			bsnMap.put(bsn, versionMap);
		}
		versionMap.put(version, resource);
	}

	private void readIndex(String name, String baseUrl, InputStream stream, IRepositoryListener listener) throws Exception {
		// Make sure we have a buffering stream
		InputStream bufferedStream;
		if (stream.markSupported())
			bufferedStream = stream;
		else
			bufferedStream = new BufferedInputStream(stream);

		// Find a compatible content provider for the input
		IRepositoryContentProvider selectedProvider = null;
		for (Entry<String, IRepositoryContentProvider> entry : allContentProviders.entrySet()) {
			IRepositoryContentProvider provider = entry.getValue();
			CheckResult checkResult = provider.checkStream(name, bufferedStream);
			if (checkResult.getDecision() == Decision.accept) {
				selectedProvider = provider;
				break;
			} else if (checkResult.getDecision() == Decision.undecided) {
				if (reporter != null)
					reporter.warning("Content provider '%s' was unable to determine compatibility with index at URL '%s': %s", provider.getName(), baseUrl, checkResult.getMessage());
			}
		}
		if (selectedProvider == null)
			throw new IOException("No content provider understands the specified index.");
		
		// Finally, parse the bugger
		try {
			selectedProvider.parseIndex(bufferedStream, baseUrl, listener, logService);
		} finally {
			IO.close(bufferedStream);
		}
	}

	List<BaseResource> narrowVersionsByFilter(String pkgName, SortedMap<Version, BaseResource> versionMap, Filter filter) {
		List<BaseResource> result = new ArrayList<BaseResource>(versionMap.size());
		
		Dictionary<String, String> dict = new Hashtable<String, String>();
		dict.put("package", pkgName);
		
		for (Version version : versionMap.keySet()) {
			dict.put("version", version.toString());
			if (filter.match(dict))
				result.add(versionMap.get(version));
		}
		
		return result;
	}

	List<BaseResource> narrowVersionsByVersionRange(SortedMap<Version, BaseResource> versionMap, String rangeStr) {
		List<BaseResource> result;
		if ("latest".equals(rangeStr)) {
			Version highest = versionMap.lastKey();
			result = Create.list(new BaseResource[] { versionMap.get(highest) });
		} else {
			VersionRange range = rangeStr != null ? new VersionRange(rangeStr) : null;
			
			// optimisation: skip versions definitely less than the range
			if (range != null && range.getLow() != null)
				versionMap = versionMap.tailMap(range.getLow());
			
			result = new ArrayList<BaseResource>(versionMap.size());
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
	
	List<ResourceHandle> mapResourcesToHandles(Collection<BaseResource> resources) throws Exception {
		List<ResourceHandle> result = new ArrayList<ResourceHandle>(resources.size());
		
		for (BaseResource resource : resources) {
			ResourceHandle handle = mapResourceToHandle(resource);
			if (handle != null)
				result.add(handle);
		}
		
		return result;
	}
	
	ResourceHandle mapResourceToHandle(BaseResource resource) throws Exception {
		ResourceHandle result = null;
		
		CachingURLResourceHandle handle ;
		try {
			handle = new CachingURLResourceHandle(resource.getContentUrl(), resource.getBaseUrl(), getCacheDirectory(), getConnector(), CachingMode.PreferCache);
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
	
	String listToString(List<?> list) {
		StringBuilder builder = new StringBuilder();
		
		int count = 0;
		for (Object item : list) {
			if (count++ > 0) builder.append(',');
			builder.append(item);
		}
		
		return builder.toString();
	}

	ResourceHandle findExactMatch(String identity, String version, Map<String, SortedMap<Version, BaseResource>> resourceMap) throws Exception {
		BaseResource resource;
		VersionRange range = new VersionRange(version);
		if (range.isRange())
			return null;
		
		SortedMap<Version, BaseResource> versions = resourceMap.get(identity);
		if (versions == null)
			return null;

		resource = findVersion(range.getLow(), versions);
		if (resource == null)
			return null;

		return mapResourceToHandle(resource);
	}
	
	BaseResource findVersion(Version version, SortedMap<Version, BaseResource> versions) {
		if (version.getQualifier() != null && version.getQualifier().length() > 0) {
			return versions.get(version);
		}

		BaseResource latest = null;
		for (Map.Entry<Version, BaseResource> entry : versions.entrySet()) {
			if (version.getMicro() == entry.getKey().getMicro()
				&& version.getMinor() == entry.getKey().getMinor()
				&& version.getMajor() == entry.getKey().getMajor()) {
				latest = entry.getValue();
				continue;
			}
			if (compare(version, entry.getKey()) < 0) {
				break;
			}
		}
		return latest;
	}

	private int compare(Version v1, Version v2) {

        if (v1.getMajor() != v2.getMajor())
            return v1.getMajor() - v2.getMajor();

        if (v1.getMinor() != v2.getMinor())
            return v1.getMinor() - v2.getMinor();

        if (v1.getMicro() != v2.getMicro())
            return v1.getMicro() - v2.getMicro();

        return 0;
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
	
	public Set<ResolutionPhase> getSupportedPhases() {
		return supportedPhases;
	}

	@Override
	public String toString() {
		return getName();
	}
}
