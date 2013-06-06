package aQute.bnd.deployer.repository;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.framework.namespace.*;
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
import aQute.lib.collections.*;
import aQute.lib.filter.*;
import aQute.lib.io.*;
import aQute.libg.generics.*;
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
public abstract class AbstractIndexedRepo implements RegistryPlugin, Plugin, RemoteRepositoryPlugin, IndexProvider, Repository {

	public static final String									PROP_NAME						= "name";
	public static final String									PROP_REPO_TYPE					= "type";
	public static final String									PROP_RESOLUTION_PHASE			= "phase";
	public static final String									PROP_RESOLUTION_PHASE_ANY		= "any";

	public static final String									REPO_TYPE_R5					= R5RepoContentProvider.NAME;
	public static final String									REPO_TYPE_OBR					= ObrContentProvider.NAME;
	public static final String									REPO_INDEX_SHA_EXTENSION		= ".sha";

	private static final int									READ_AHEAD_MAX					= 5 * 1024 * 1024;

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
	private final Map<String,SortedMap<Version,Resource>>	bsnMap							= new HashMap<String,SortedMap<Version,Resource>>();

	protected AbstractIndexedRepo() {
		allContentProviders.put(REPO_TYPE_R5, new R5RepoContentProvider());
		allContentProviders.put(REPO_TYPE_OBR, new ObrContentProvider(obrIndexer));

		generatingProviders.add(allContentProviders.get(REPO_TYPE_R5));
	}

	public synchronized void reset() {
		initialised = false;
	}

	private synchronized void clear() {
		bsnMap.clear();
		capabilityIndex.clear();
	}

	protected abstract List<URI> loadIndexes() throws Exception;

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
					addResourceToIndex(resource);
				}

				public void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth) {
					try {
						URI indexLocation = new URI(referral.getUrl());
						try {
							CachingUriResourceHandle indexHandle = new CachingUriResourceHandle(indexLocation, getCacheDirectory(), connector, (String) null);
							indexHandle.setReporter(reporter);
							readIndex(indexLocation.getPath(), indexLocation, new FileInputStream(indexHandle.request()), this);
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
					indexHandle.setReporter(reporter);
					File indexFile = indexHandle.request();
					InputStream indexStream = GZipUtils.detectCompression(new FileInputStream(indexFile));
					readIndex(indexFile.getName(), indexLocation, indexStream, processor);
				}
				catch (Exception e) {
					warning("Unable to read index at URL '%s': %s", indexLocation, e);
				}
			}

			initialised = true;
		}
	}

	public final List<URI> getIndexLocations() throws Exception {
		init();
		return Collections.unmodifiableList(indexLocations);
	}

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
		if ("project".equals(rangeStr))
			return null;

		SortedMap<Version,Resource> versionMap = bsnMap.get(bsn);
		if (versionMap == null || versionMap.isEmpty())
			return null;
		List<Resource> resources = narrowVersionsByVersionRange(versionMap, rangeStr);
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

		for (String bsn : bsnMap.keySet()) {
			if (glob == null || glob.matcher(bsn).matches())
				result.add(bsn);
		}

		return result;
	}

	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		SortedMap<Version,Resource> versionMap = bsnMap.get(bsn);
		if (versionMap == null || versionMap.isEmpty())
			return SortedList.empty();
		
		return new SortedList<Version>(versionMap.keySet());
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

	void addResourceToIndex(Resource resource) {
		// Add to the bundle identity map
		String id = getResourceIdentity(resource);
		if (id == null)
			return;
		
		Version version = getResourceVersion(resource);
		SortedMap<Version,Resource> versionMap = bsnMap.get(id);
		if (versionMap == null) {
			versionMap = new TreeMap<Version,Resource>();
			bsnMap.put(id, versionMap);
		}
		versionMap.put(version, resource);
		
		// Add capabilities to the capability index
		capabilityIndex.addResource(resource);
	}
	
	static Capability getIdentityCapability(Resource resource) {
		List<Capability> identityCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identityCaps == null || identityCaps.isEmpty())
			throw new IllegalArgumentException("Resource has no identity capability.");
		return identityCaps.iterator().next();
	}
	
	static String getResourceIdentity(Resource resource) {
		return (String) getIdentityCapability(resource).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
	}

	static Version getResourceVersion(Resource resource) {
		Version result;
		
		Object versionObj = getIdentityCapability(resource).getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		if (versionObj == null) {
			result = Version.emptyVersion;
		} else if (versionObj instanceof org.osgi.framework.Version) {
			org.osgi.framework.Version v = (org.osgi.framework.Version) versionObj;
			result = new Version(v.toString());
		} else {
			throw new IllegalArgumentException("Cannot convert to Version from type: " + versionObj.getClass());
		}
		
		return result;
	}
	
	static URI getContentUrl(Resource resource) {
		List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
		if (caps == null || caps.isEmpty())
			throw new IllegalArgumentException("Resource has no content capability");

		Object uri = caps.iterator().next().getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
		if (uri == null)
			throw new IllegalArgumentException("Resource content has no 'uri' attribute.");
		if (uri instanceof URI)
			return (URI) uri;
		
		try {
			if (uri instanceof URL)
				return ((URL) uri).toURI();
			if (uri instanceof String)
				return new URI((String) uri);
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Failed to convert resource content location to a valid URI.", e);
		}
		
		throw new IllegalArgumentException("Failed to convert resource content location to a valid URI.");
	}
	
	static String getContentSha(Resource resource) {
		List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
		if (caps == null || caps.isEmpty())
			return null;
		
		Object contentObj = caps.iterator().next().getAttributes().get(ContentNamespace.CONTENT_NAMESPACE);
		if (contentObj == null)
			return null;
		if (contentObj instanceof String)
			return (String) contentObj;
		
		throw new IllegalArgumentException("Content attribute is wrong type: " + contentObj.getClass().toString() + " (expected String).");
	}

	private void readIndex(String name, URI baseUri, InputStream stream, IRepositoryIndexProcessor listener)
			throws Exception {
		// Make sure we have a buffering stream
		InputStream bufferedStream;
		if (stream.markSupported())
			bufferedStream = stream;
		else
			bufferedStream = new BufferedInputStream(stream);

		// Find a compatible content provider for the input
		IRepositoryContentProvider selectedProvider = null;
		IRepositoryContentProvider maybeSelectedProvider = null;
		for (Entry<String,IRepositoryContentProvider> entry : allContentProviders.entrySet()) {
			IRepositoryContentProvider provider = entry.getValue();

			CheckResult checkResult;
			try {
				bufferedStream.mark(READ_AHEAD_MAX);
				checkResult = provider.checkStream(name, new ProtectedStream(bufferedStream));
			}
			finally {
				bufferedStream.reset();
			}

			if (checkResult.getDecision() == Decision.accept) {
				selectedProvider = provider;
				break;
			} else if (checkResult.getDecision() == Decision.undecided) {
				warning("Content provider '%s' was unable to determine compatibility with index at URL '%s': %s",
						provider.getName(), baseUri, checkResult.getMessage());
				if (maybeSelectedProvider == null)
					maybeSelectedProvider = provider;
			}
		}

		// If no provider answered definitively, fall back to the first
		// undecided provider, with an appropriate warning.
		if (selectedProvider == null) {
			if (maybeSelectedProvider != null) {
				selectedProvider = maybeSelectedProvider;
				warning("No content provider matches the specified index unambiguously. Selected '%s' arbitrarily.",
						selectedProvider.getName());
			} else {
				throw new IOException("No content provider understands the specified index.");
			}
		}

		// Finally, parse the damn file.
		try {
			selectedProvider.parseIndex(bufferedStream, baseUri, listener, logService);
		}
		finally {
			IO.close(bufferedStream);
		}
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

	static List<Resource> narrowVersionsByVersionRange(SortedMap<Version,Resource> versionMap, String rangeStr) {
		List<Resource> result;
		if ("latest".equals(rangeStr)) {
			Version highest = versionMap.lastKey();
			result = Create.list(new Resource[] {
				versionMap.get(highest)
			});
		} else {
			VersionRange range = rangeStr != null ? new VersionRange(rangeStr) : null;

			// optimisation: skip versions definitely less than the range
			if (range != null && range.getLow() != null)
				versionMap = versionMap.tailMap(range.getLow());

			result = new ArrayList<Resource>(versionMap.size());
			for (Entry<Version,Resource> entry : versionMap.entrySet()) {
				Version version = entry.getKey();
				if (range == null || range.includes(version))
					result.add(entry.getValue());

				// optimisation: skip versions definitely higher than the range
				if (range != null && range.isRange() && version.compareTo(range.getHigh()) >= 0)
					break;
			}
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
			handle = new CachingUriResourceHandle(getContentUrl(resource), getCacheDirectory(), getConnector(), getContentSha(resource));
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
			return findExactMatch(bsn, rangeStr, bsnMap);
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

	ResourceHandle findExactMatch(String identity, String version, Map<String,SortedMap<Version,Resource>> resourceMap) throws Exception {
		Resource resource;
		VersionRange range = new VersionRange(version);
		if (range.isRange())
			return null;

		SortedMap<Version,Resource> versions = resourceMap.get(identity);
		if (versions == null)
			return null;

		resource = findVersion(range.getLow(), versions);
		if (resource == null)
			return null;

		return mapResourceToHandle(resource);
	}

	static Resource findVersion(Version version, SortedMap<Version,Resource> versions) {
		if (version.getQualifier() != null && version.getQualifier().length() > 0) {
			return versions.get(version);
		}

		Resource latest = null;
		for (Map.Entry<Version,Resource> entry : versions.entrySet()) {
			if (version.getMicro() == entry.getKey().getMicro() && version.getMinor() == entry.getKey().getMinor()
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

	private static int compare(Version v1, Version v2) {

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

}
