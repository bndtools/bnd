package aQute.bnd.deployer.repository;

import static aQute.bnd.deployer.repository.RepoResourceUtils.getContentSha;
import static aQute.bnd.deployer.repository.RepoResourceUtils.getContentUrl;
import static aQute.bnd.deployer.repository.RepoResourceUtils.getIdentityCapability;
import static aQute.bnd.deployer.repository.RepoResourceUtils.readIndex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;

import aQute.bnd.deployer.repository.api.IRepositoryContentProvider;
import aQute.bnd.deployer.repository.api.IRepositoryIndexProcessor;
import aQute.bnd.deployer.repository.api.Referral;
import aQute.bnd.deployer.repository.providers.R5RepoContentProvider;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RemoteRepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.service.ResourceHandle;
import aQute.bnd.service.ResourceHandle.Location;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.url.URLConnector;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.filter.Filter;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import aQute.libg.gzip.GZipUtils;
import aQute.service.reporter.Reporter;

/**
 * Abstract base class for indexed repositories.
 * <p>
 * The repository implementation is read-only by default. To implement a
 * writable repository, subclasses should override {@link #canWrite()} and
 * {@link #put(InputStream, aQute.bnd.service.RepositoryPlugin.PutOptions)}.
 *
 * @author Neil Bartlett
 */
@SuppressWarnings("synthetic-access")
public abstract class AbstractIndexedRepo extends BaseRepository
	implements RegistryPlugin, Plugin, RemoteRepositoryPlugin, IndexProvider, Repository, Refreshable {

	private static final String								SHA_256							= "SHA-256";

	public static final String								PROP_NAME						= "name";
	public static final String								PROP_REPO_TYPE					= "type";
	public static final String								PROP_RESOLUTION_PHASE			= "phase";
	public static final String								PROP_RESOLUTION_PHASE_ANY		= "any";

	public static final String								REPO_TYPE_R5					= R5RepoContentProvider.NAME;
	public static final String								REPO_INDEX_SHA_EXTENSION		= ".sha";
	public static final String								PROP_CACHE_TIMEOUT				= "timeout";
	public static final String								PROP_ONLINE						= "online";
	public static final String								PROP_VERSION_KEY				= "version";
	public static final String								PROP_VERSION_HASH				= "hash";
	public static final String								PROP_CHECK_BSN					= "bsn";

	private final static int								DEFAULT_CACHE_TIMEOUT			= 5;

	/**
	 * Make sure the content providers are always processed in the same order.
	 */
	protected final Map<String, IRepositoryContentProvider>	allContentProviders				= new TreeMap<>();

	protected final List<IRepositoryContentProvider>		generatingProviders				= new LinkedList<>();

	protected Registry										registry;
	protected Reporter										reporter;
	protected LogService									logService						= new NullLogService();
	protected String										name							= this.getClass()
		.getName();
	protected Set<ResolutionPhase>							supportedPhases					= EnumSet
		.allOf(ResolutionPhase.class);

	private List<URI>										indexLocations;

	private String											requestedContentProviderList	= null;

	private boolean											initialised						= false;

	private final CapabilityIndex							capabilityIndex					= new CapabilityIndex();
	private final VersionedResourceIndex					identityMap						= new VersionedResourceIndex();
	private int												cacheTimeoutSeconds				= DEFAULT_CACHE_TIMEOUT;
	private boolean											online							= true;

	protected AbstractIndexedRepo() {
		allContentProviders.put(REPO_TYPE_R5, new R5RepoContentProvider());

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

	protected final void init() throws Exception {
		init(false);
	}

	protected final synchronized void init(boolean ignoreCachedFile) throws Exception {
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
					String token = tokenizer.nextToken()
						.trim();
					IRepositoryContentProvider provider = allContentProviders.get(token);
					if (provider == null) {
						warning("Unknown repository content provider \"%s\".", token);
					} else {
						generatingProviders.add(provider);
					}
				}
				if (generatingProviders.isEmpty()) {
					warning("No valid repository index generators were found, requested list was: [%s]",
						requestedContentProviderList);
				}
			}

			// Initialise index locations
			indexLocations = loadIndexes();

			// Create the callback for new referral and resource objects
			final URLConnector connector = getConnector();
			IRepositoryIndexProcessor processor = new IRepositoryIndexProcessor() {

				@Override
				public void processResource(Resource resource) {
					identityMap.put(resource);
					capabilityIndex.addResource(resource);
				}

				@Override
				public void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth) {
					try {
						URI indexLocation = new URI(referral.getUrl());
						try {
							CachingUriResourceHandle indexHandle = new CachingUriResourceHandle(indexLocation,
								getCacheDirectory(), connector, null);
							indexHandle.setReporter(reporter);
							InputStream indexStream = GZipUtils.detectCompression(IO.stream(indexHandle.request()));
							readIndex(indexLocation.getPath(), indexLocation, indexStream, allContentProviders.values(),
								this, logService);
						} catch (Exception e) {
							warning("Unable to read referral index at URL '%s' from parent index '%s': %s",
								indexLocation, parentUri, e);
						}

					} catch (URISyntaxException e) {
						warning("Invalid referral URL '%s' from parent index '%s': %s", referral.getUrl(), parentUri,
							e);
					}
				}

			};

			// Parse the indexes
			for (URI indexLocation : indexLocations) {
				try {
					CachingUriResourceHandle indexHandle = new CachingUriResourceHandle(indexLocation,
						getCacheDirectory(), connector, null);
					// If there is a cachedFile, then just use it IF
					// 1) the cachedFile is within the timeout period
					// OR 2) online is false
					if (indexHandle.cachedFile != null && !ignoreCachedFile
						&& ((System.currentTimeMillis()
							- indexHandle.cachedFile.lastModified() < this.cacheTimeoutSeconds * 1000)
							|| !this.online)) {
						indexHandle.sha = indexHandle.getCachedSHA();
						if (indexHandle.sha != null && !this.online) {
							System.out.println(String.format("Offline. Using cached %s.", indexLocation));
						}
					}
					indexHandle.setReporter(reporter);
					File indexFile = indexHandle.request();
					InputStream indexStream = GZipUtils.detectCompression(IO.stream(indexFile));
					readIndex(indexFile.getName(), indexLocation, indexStream, allContentProviders.values(), processor,
						logService);
				} catch (Exception e) {
					error("Unable to read index at URL '%s': %s", indexLocation, e);
				}
			}

			initialised = true;
		}
	}

	@Override
	public final List<URI> getIndexLocations() throws Exception {
		init();
		return Collections.unmodifiableList(indexLocations);
	}

	/**
	 * @return the current Http Client
	 */
	private URLConnector getConnector() {
		if (registry != null) {
			HttpClient plugin = registry.getPlugin(HttpClient.class);
			if (plugin != null)
				return plugin;
		}
		return new HttpClient();
	}

	@Override
	public synchronized final void setRegistry(Registry registry) {
		this.registry = registry;
	}

	@Override
	public synchronized void setProperties(Map<String, String> map) {
		if (map.containsKey(PROP_NAME))
			name = map.get(PROP_NAME);

		if (map.containsKey(PROP_RESOLUTION_PHASE)) {
			supportedPhases = EnumSet.noneOf(ResolutionPhase.class);
			StringTokenizer tokenizer = new StringTokenizer(map.get(PROP_RESOLUTION_PHASE), ",");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken()
					.trim();
				if (PROP_RESOLUTION_PHASE_ANY.equalsIgnoreCase(token))
					supportedPhases = EnumSet.allOf(ResolutionPhase.class);
				else {
					try {
						supportedPhases.add(ResolutionPhase.valueOf(token));
					} catch (Exception e) {
						error("Unknown OBR resolution mode: " + token);
					}
				}
			}
		}

		if (map.containsKey(PROP_CACHE_TIMEOUT)) {
			try {
				this.cacheTimeoutSeconds = Integer.parseInt(map.get(PROP_CACHE_TIMEOUT));
			} catch (NumberFormatException e) {
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

	protected static File[] requestAll(ResourceHandle[] handles) throws Exception {
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

		return handles.toArray(new ResourceHandle[0]);
	}

	@Override
	public synchronized void setReporter(Reporter reporter) {
		this.reporter = reporter;
		this.logService = new ReporterLogService(reporter);
	}

	public File get(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
		ResourceHandle handle = getHandle(bsn, range, strategy, properties);
		return handle != null ? handle.request() : null;
	}

	@Override
	public ResourceHandle getHandle(String bsn, String range, Strategy strategy, Map<String, String> properties)
		throws Exception {
		init();
		ResourceHandle result;
		if (bsn != null)
			result = resolveBundle(bsn, range, strategy, properties);
		else {
			throw new IllegalArgumentException("Cannot resolve bundle: bundle symbolic name not specified.");
		}
		return result;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Read-only repository.");
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		init();
		Glob glob = pattern != null ? new Glob(pattern) : null;
		List<String> result = new LinkedList<>();

		for (String bsn : identityMap.getIdentities()) {
			if (glob == null || glob.matcher(bsn)
				.matches())
				result.add(bsn);
		}

		return result;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		return identityMap.getVersions(bsn);
	}

	@Override
	public synchronized String getName() {
		return name;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		try {
			init();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Map<Requirement, Collection<Capability>> result = new HashMap<>();
		for (Requirement requirement : requirements) {
			List<Capability> matches = new LinkedList<>();
			result.put(requirement, matches);

			capabilityIndex.appendMatchingCapabilities(requirement, matches);
		}
		return result;
	}

	static List<Resource> narrowVersionsByFilter(String pkgName, SortedMap<Version, Resource> versionMap, Filter filter)
		throws Exception {
		List<Resource> result = new ArrayList<>(versionMap.size());

		Dictionary<String, String> dict = new Hashtable<>();
		dict.put("package", pkgName);

		for (Entry<Version, Resource> entry : versionMap.entrySet()) {
			dict.put("version", entry.getKey()
				.toString());
			if (filter.match(dict))
				result.add(entry.getValue());
		}

		return result;
	}

	List<ResourceHandle> mapResourcesToHandles(Collection<Resource> resources) throws Exception {
		List<ResourceHandle> result = new ArrayList<>(resources.size());

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
			handle = new CachingUriResourceHandle(getContentUrl(resource), getCacheDirectory(), getConnector(),
				contentSha);
			if (contentSha == null) {
				handle.sha = handle.getCachedSHA();
			}
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Broken link in repository index: " + e);
		}
		if (handle.getLocation() == Location.local || getCacheDirectory() != null)
			result = handle;

		return result;
	}

	ResourceHandle resolveBundle(String bsn, String rangeStr, Strategy strategy, Map<String, String> properties)
		throws Exception {
		if (rangeStr == null)
			rangeStr = "0.0.0";

		if (PROP_VERSION_HASH.equals(rangeStr)) {
			return findByHash(bsn, properties);
		}

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

	static String listToString(List<?> list) {
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

	ResourceHandle findByHash(String bsn, Map<String, String> properties) throws Exception {
		if (bsn == null)
			throw new IllegalArgumentException("Bundle symbolic name must be specified");

		// Get the hash string
		String hashStr = properties.get("hash");
		if (hashStr == null)
			throw new IllegalArgumentException(
				"Content hash must be provided (using hash=<algo>:<hash>) when version=hash is specified");

		// Parse into algo and hash
		String algo = SHA_256;
		int colonIndex = hashStr.indexOf(':');
		if (colonIndex > -1) {
			algo = hashStr.substring(0, colonIndex);
			int afterColon = colonIndex + 1;
			hashStr = (colonIndex < hashStr.length()) ? hashStr.substring(afterColon) : "";
		}

		// R5 indexes are always SHA-256
		if (!SHA_256.equalsIgnoreCase(algo))
			return null;

		String contentFilter = String.format("(%s=%s)", ContentNamespace.CONTENT_NAMESPACE, hashStr);
		Requirement contentReq = new CapReqBuilder(ContentNamespace.CONTENT_NAMESPACE).filter(contentFilter)
			.buildSyntheticRequirement();

		List<Capability> caps = new LinkedList<>();
		capabilityIndex.appendMatchingCapabilities(contentReq, caps);

		if (caps.isEmpty())
			return null;

		Resource resource = caps.get(0)
			.getResource();

		Capability identityCap = getIdentityCapability(resource);
		Object id = identityCap.getAttributes()
			.get(IdentityNamespace.IDENTITY_NAMESPACE);
		if (!bsn.equals(id))
			throw new IllegalArgumentException(
				String.format("Resource with requested hash does not match ID '%s' [hash: %s]", bsn, hashStr));

		return mapResourceToHandle(resource);
	}

	/**
	 * Utility function for parsing lists of URLs.
	 *
	 * @param locationsStr Comma-separated list of URLs
	 * @return a list of URIs
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	protected static List<URI> parseLocations(String locationsStr) throws MalformedURLException, URISyntaxException {
		StringTokenizer tok = new StringTokenizer(locationsStr, ",");
		List<URI> urls = new ArrayList<>(tok.countTokens());
		while (tok.hasMoreTokens()) {
			String urlStr = tok.nextToken()
				.trim();
			urls.add(new URL(urlStr).toURI());
		}
		return urls;
	}

	@Override
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
	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		init();

		String versionStr;
		if (version != null)
			versionStr = version.toString();
		else
			versionStr = properties.get(PROP_VERSION_KEY);
		ResourceHandle handle = resolveBundle(bsn, versionStr, Strategy.EXACT, properties);
		if (handle == null)
			return null;

		File f = handle.request();
		if (f == null)
			return null;

		for (DownloadListener l : listeners) {
			try {
				l.success(f);
			} catch (Exception e) {
				error("Download listener for %s: %s", f, e);
			}
		}
		return f;
	}

	private void error(String format, Object... args) {
		if (reporter != null)
			reporter.error(format, args);
		else
			System.err.println(Strings.format(format, args));
	}

	private void warning(String format, Object... args) {
		if (reporter != null)
			reporter.warning(format, args);
		else
			System.err.println(Strings.format(format, args));
	}

	@Override
	public boolean refresh() throws Exception {
		initialised = false;
		init(true);
		return true;
	}
}
