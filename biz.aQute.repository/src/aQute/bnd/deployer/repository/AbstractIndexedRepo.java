package aQute.bnd.deployer.repository;

import static aQute.bnd.deployer.repository.RepoResourceUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.impl.bundle.bindex.*;
import org.osgi.resource.*;
import org.osgi.resource.Resource;
import org.osgi.service.bindex.*;
import org.osgi.service.log.*;
import org.osgi.service.repository.*;

import aQute.bnd.deployer.http.*;
import aQute.bnd.deployer.repository.api.*;
import aQute.bnd.deployer.repository.providers.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.ResourceHandle.Location;
import aQute.bnd.service.url.*;
import aQute.bnd.version.*;
import aQute.lib.filter.*;
import aQute.libg.glob.*;
import aQute.libg.gzip.*;
import aQute.service.reporter.*;

/**
 * Abstract base class for indexed repositories.
 * <p>
 * The repository implementation is read-only by default. To implement a
 * writable repository, subclasses should override {@link #canWrite()} and
 * {@link #put(Jar)}.
 * 
 * @author Neil Bartlett
 */
@SuppressWarnings("synthetic-access")
public abstract class AbstractIndexedRepo implements RegistryPlugin, Plugin, RemoteRepositoryPlugin, IndexProvider, Repository, Refreshable {

	public static final String									PROP_NAME						= "name";
	public static final String									PROP_REPO_TYPE					= "type";
	public static final String									PROP_RESOLUTION_PHASE			= "phase";
	public static final String									PROP_RESOLUTION_PHASE_ANY		= "any";

	public static final String									REPO_TYPE_R5					= R5RepoContentProvider.NAME;
	public static final String									REPO_TYPE_OBR					= ObrContentProvider.NAME;
	public static final String									REPO_INDEX_SHA_EXTENSION		= ".sha";
	public static final String									PROP_CACHE_TIMEOUT				= "timeout";
	public static final String									PROP_ONLINE						= "online";
	
	private final static int DEFAULT_CACHE_TIMEOUT = 5;

	
	private final BundleIndexer								obrIndexer						= new BundleIndexerImpl();
	protected final Map<String,IRepositoryContentProvider>	allContentProviders				= new HashMap<String,IRepositoryContentProvider>(5);
	protected final List<IRepositoryContentProvider>		generatingProviders				= new LinkedList<IRepositoryContentProvider>();

	protected Registry											registry;
	protected Reporter											reporter;
	protected LogService										logService						= new NullLogService();
	protected String											name							= this.getClass().getName();
	protected Set<ResolutionPhase>								supportedPhases					= EnumSet.allOf(ResolutionPhase.class);

	private List<URI>											indexLocations;

	private String												requestedContentProviderList	= null;

	private boolean												initialised						= false;

	private final CapabilityIndex							capabilityIndex					= new CapabilityIndex();
	private final VersionedResourceIndex					identityMap						= new VersionedResourceIndex();
	private int cacheTimeoutSeconds = DEFAULT_CACHE_TIMEOUT;
	private boolean online = true;

	protected AbstractIndexedRepo() {
		allContentProviders.put(REPO_TYPE_R5, new R5RepoContentProvider());
		allContentProviders.put(REPO_TYPE_OBR, new ObrContentProvider(obrIndexer));

		generatingProviders.add(allContentProviders.get(REPO_TYPE_R5));
	}

	public synchronized void reset() {
		initialised = false;
	}

	private synchronized void clear() {
		identityMap.clear();
		capabilityIndex.clear();
	}

	/**
	 * @return a list of URIs, parsed from the 'locations' property
	 */
	protected abstract List<URI> loadIndexes() throws Exception;

	/**
	 * Add all repository content providers that are in the registry (through
	 * workspace plugins) to the known content providers
	 */
	protected synchronized void loadAllContentProviders() {
		if (registry == null)
			return;

		List<IRepositoryContentProvider> extraProviders = registry.getPlugins(IRepositoryContentProvider.class);

		for (IRepositoryContentProvider provider : extraProviders) {
			String providerName = provider.getName();
			if (allContentProviders.containsKey(providerName)) {
				warning("Repository content provider with name \"%s\" is already registered.", providerName);
			} else {
				allContentProviders.put(providerName, provider);
			}
		}
	}

	protected final synchronized void init() throws Exception {
		if (!initialised) {
			clear();

			// Load the available providers from the workspace plugins.
			loadAllContentProviders();

			// Load the request repository content providers, if specified
			if (requestedContentProviderList != null && requestedContentProviderList.length() > 0) {
				generatingProviders.clear();

				// Find the requested providers from the available ones.
				StringTokenizer tokenizer = new StringTokenizer(requestedContentProviderList, "|");
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken().trim();
					IRepositoryContentProvider provider = allContentProviders.get(token);
					if (provider == null) {
						warning("Unknown repository content provider \"%s\".", token);
					} else {
						generatingProviders.add(provider);
					}
				}
				if (generatingProviders.isEmpty()) {
					warning("No valid repository index generators were found, requested list was: [%s]", requestedContentProviderList);
				}
			}

			// Initialise index locations
			indexLocations = loadIndexes();

			// Create the callback for new referral and resource objects
			final URLConnector connector = getConnector();
			IRepositoryIndexProcessor processor = new IRepositoryIndexProcessor() {

				public void processResource(Resource resource) {
					identityMap.put(resource);
					capabilityIndex.addResource(resource);
				}

				public void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth) {
					try {
						URI indexLocation = new URI(referral.getUrl());
						try {
							CachingUriResourceHandle indexHandle = new CachingUriResourceHandle(indexLocation, getCacheDirectory(), connector, (String) null);
							indexHandle.setReporter(reporter);
							readIndex(indexLocation.getPath(), indexLocation, new FileInputStream(indexHandle.request()), allContentProviders.values(), this, logService);
						}
						catch (Exception e) {
							warning("Unable to read referral index at URL '%s' from parent index '%s': %s", indexLocation, parentUri, e);
						}

					}
					catch (URISyntaxException e) {
						warning("Invalid referral URL '%s' from parent index '%s': %s", referral.getUrl(), parentUri, e);
					}
				}

			};

			// Parse the indexes
			for (URI indexLocation : indexLocations) {
				try {
					CachingUriResourceHandle indexHandle = new CachingUriResourceHandle(indexLocation, getCacheDirectory(), connector, (String) null);
					// If there is a cachedFile, then just use it IF
					// 1) the cachedFile is within the timeout period
					// OR 2) online is false
					if (indexHandle.cachedFile != null &&
							((System.currentTimeMillis() - indexHandle.cachedFile.lastModified() < this.cacheTimeoutSeconds * 1000)
							|| !this.online)) {
						indexHandle.sha = indexHandle.getCachedSHA();
						if (indexHandle.sha != null && !this.online) {
							System.out.println(String.format("Offline. Using cached %s.", indexLocation));
						}
					}
					indexHandle.setReporter(reporter);
					File indexFile = indexHandle.request();
					InputStream indexStream = GZipUtils.detectCompression(new FileInputStream(indexFile));
					readIndex(indexFile.getName(), indexLocation, indexStream, allContentProviders.values(), processor, logService);
				}
				catch (Exception e) {
					error("Unable to read index at URL '%s': %s", indexLocation, e);
				}
			}

			initialised = true;
		}
	}

	public final List<URI> getIndexLocations() throws Exception {
		init();
		return Collections.unmodifiableList(indexLocations);
	}

	/**
	 * @return the class to use for URL connections. It's retrieved from the
	 *         registry under the URLConnector class, or it will be the
	 *         DefaultURLConnector if the former was not found.
	 */
	private URLConnector getConnector() {
		URLConnector connector;
		synchronized (this) {
			connector = registry != null ? registry.getPlugin(URLConnector.class) : null;
		}
		if (connector == null) {
			DefaultURLConnector defaultConnector = new DefaultURLConnector();
			defaultConnector.setRegistry(registry);
			connector = defaultConnector;
		}
		return connector;
	}

	public synchronized final void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public synchronized void setProperties(Map<String,String> map) {
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
					}
					catch (Exception e) {
						error("Unknown OBR resolution mode: " + token);
					}
				}
			}
		}
		
		if (map.containsKey(PROP_CACHE_TIMEOUT)) {
			try {
				this.cacheTimeoutSeconds = Integer.parseInt(map.get(PROP_CACHE_TIMEOUT));
			}
			catch (NumberFormatException e) {
				error("Bad timeout setting. Must be integer number of milliseconds.");
			}
		}

		if (map.containsKey(PROP_ONLINE)) {
			this.online = Boolean.parseBoolean(map.get(PROP_ONLINE));
		}
		
		requestedContentProviderList = map.get(PROP_REPO_TYPE);
	}

	public File[] get(String bsn, String range) throws Exception {
		ResourceHandle[] handles = getHandles(bsn, range);

		return requestAll(handles);
	}

	protected static File[] requestAll(ResourceHandle[] handles) throws IOException {
		File[] result;
		if (handles == null)
			result = new File[0];
		else {
			result = new File[handles.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = handles[i].request();
			}
		}
		return result;
	}

	protected ResourceHandle[] getHandles(String bsn, String rangeStr) throws Exception {
		init();

		// If the range is set to "project", we cannot resolve it.
		if (Constants.VERSION_ATTR_PROJECT.equals(rangeStr))
			return null;

		List<Resource> resources = identityMap.getRange(bsn, rangeStr);
		List<ResourceHandle> handles = mapResourcesToHandles(resources);

		return (ResourceHandle[]) handles.toArray(new ResourceHandle[handles.size()]);
	}

	public synchronized void setReporter(Reporter reporter) {
		this.reporter = reporter;
		this.logService = new ReporterLogService(reporter);
	}

	public File get(String bsn, String range, Strategy strategy, Map<String,String> properties) throws Exception {
		ResourceHandle handle = getHandle(bsn, range, strategy, properties);
		return handle != null ? handle.request() : null;
	}

	public ResourceHandle getHandle(String bsn, String range, Strategy strategy, Map<String,String> properties)
			throws Exception {
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

	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Read-only repository.");
	}

	public List<String> list(String pattern) throws Exception {
		init();
		Glob glob = pattern != null ? new Glob(pattern) : null;
		List<String> result = new LinkedList<String>();

		for (String bsn : identityMap.getIdentities()) {
			if (glob == null || glob.matcher(bsn).matches())
				result.add(bsn);
		}

		return result;
	}

	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		return identityMap.getVersions(bsn);
	}

	public synchronized String getName() {
		return name;
	}
	
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		try {
			init();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		Map<Requirement,Collection<Capability>> result = new HashMap<Requirement,Collection<Capability>>();
		for (Requirement requirement : requirements) {
			List<Capability> matches = new LinkedList<Capability>();
			result.put(requirement, matches);

			capabilityIndex.appendMatchingCapabilities(requirement, matches);
		}
		return result;
	}

	static List<Resource> narrowVersionsByFilter(String pkgName, SortedMap<Version,Resource> versionMap, Filter filter) {
		List<Resource> result = new ArrayList<Resource>(versionMap.size());

		Dictionary<String,String> dict = new Hashtable<String,String>();
		dict.put("package", pkgName);

		for (Entry<Version,Resource> entry : versionMap.entrySet()) {
			dict.put("version", entry.getKey().toString());
			if (filter.match(dict))
				result.add(entry.getValue());
		}

		return result;
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

		CachingUriResourceHandle handle;
		try {
			String contentSha = getContentSha(resource);
			handle = new CachingUriResourceHandle(getContentUrl(resource), getCacheDirectory(), getConnector(), contentSha);
			if (contentSha == null) {
				handle.sha = handle.getCachedSHA();
			}
		}
		catch (FileNotFoundException e) {
			throw new FileNotFoundException("Broken link in repository index: " + e.getMessage());
		}
		if (handle.getLocation() == Location.local || getCacheDirectory() != null)
			result = handle;

		return result;
	}

	ResourceHandle resolveBundle(String bsn, String rangeStr, Strategy strategy) throws Exception {
		if (rangeStr == null)
			rangeStr = "0.0.0";

		if (strategy == Strategy.EXACT) {
			return findExactMatch(bsn, rangeStr);
		}

		ResourceHandle[] handles = getHandles(bsn, rangeStr);
		ResourceHandle selected;
		if (handles == null || handles.length == 0)
			selected = null;
		else {
			switch (strategy) {
				case LOWEST :
					selected = handles[0];
					break;
				default :
					selected = handles[handles.length - 1];
			}
		}
		return selected;
	}

	static String listToString(List< ? > list) {
		StringBuilder builder = new StringBuilder();

		int count = 0;
		for (Object item : list) {
			if (count++ > 0)
				builder.append(',');
			builder.append(item);
		}

		return builder.toString();
	}

	ResourceHandle findExactMatch(String identity, String version) throws Exception {
		VersionRange range = new VersionRange(version);
		if (range.isRange())
			return null;
		Resource resource = identityMap.getExact(identity, range.getLow());
		if (resource == null)
			return null;
		return mapResourceToHandle(resource);
	}

	/**
	 * Utility function for parsing lists of URLs.
	 * 
	 * @param locationsStr
	 *            Comma-separated list of URLs
	 * @return a list of URIs
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	protected static List<URI> parseLocations(String locationsStr) throws MalformedURLException, URISyntaxException {
		StringTokenizer tok = new StringTokenizer(locationsStr, ",");
		List<URI> urls = new ArrayList<URI>(tok.countTokens());
		while (tok.hasMoreTokens()) {
			String urlStr = tok.nextToken().trim();
			urls.add(new URL(urlStr).toURI());
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

	/**
	 * This can be optimized to use the download technique with the listeners.
	 * Now just a quick hack to make it work. I actually think these classes
	 * should extend FileRepo. TODO 
	 */
	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		init();
		ResourceHandle handle = resolveBundle(bsn, version.toString(), Strategy.EXACT);
		if (handle == null)
			return null;

		File f = handle.request();
		if (f == null)
			return null;

		for (DownloadListener l : listeners) {
			try {
				l.success(f);
			}
			catch (Exception e) {
				error("Download listener for %s: %s", f, e);
			}
		}
		return f;
	}
	
	private void error(String format, Object... args) {
		if (reporter != null)
			reporter.error(format, args);
		else
			System.err.println(String.format(format, args));
	}

	private void warning(String format, Object... args) {
		if (reporter != null)
			reporter.warning(format, args);
		else
			System.err.println(String.format(format, args));
	}

	public boolean refresh() throws Exception {
		boolean ret = true;
		if (indexLocations != null) {
			final URLConnector connector = getConnector();
			for (URI indexLocation : indexLocations) {
				CachingUriResourceHandle indexHandle;
				try {
					File f = new CachingUriResourceHandle(indexLocation, getCacheDirectory(), connector, (String) null).cachedFile;
					if (f != null && f.exists() && !f.delete()) {
						error("Unable to delete cached repository index file %s", f.getAbsolutePath());
					}
				}
				catch (IOException e) {
					error("Exception during refresh of %s", indexLocation, e);
					ret = false;
				}
			}
		}
		initialised = false;
		init();
		return ret;
	}
}
