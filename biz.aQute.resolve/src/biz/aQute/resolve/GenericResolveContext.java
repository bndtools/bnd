package biz.aQute.resolve;
import org.osgi.framework.Constants;
import static org.osgi.framework.namespace.BundleNamespace.*;
import static org.osgi.framework.namespace.PackageNamespace.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.namespace.contract.*;
import org.osgi.resource.*;
import org.osgi.resource.Resource;
import org.osgi.service.log.*;
import org.osgi.service.repository.*;
import org.osgi.service.resolver.*;

import aQute.bnd.build.model.*;
import aQute.bnd.deployer.repository.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.resolve.hook.*;
import aQute.libg.filters.*;
import aQute.libg.filters.Filter;
import biz.aQute.resolve.internal.*;

public class GenericResolveContext extends ResolveContext {

	protected static final String					CONTRACT_OSGI_FRAMEWORK		= "OSGiFramework";
	protected static final String					IDENTITY_INITIAL_RESOURCE	= "<<INITIAL>>";

	protected final LogService						log;

	private final List<Capability>					systemCapabilities;
	private final List<Capability>					capabilities				= new ArrayList<Capability>();
	private final List<Requirement>					systemRequirements;
	private final List<Requirement>					requirements				= new ArrayList<Requirement>();

	protected CapabilityIndex						systemCapabilityIndex;

	protected final List<Repository>				repositories				= new ArrayList<Repository>();

	protected final List<Requirement>				failed						= new ArrayList<Requirement>();
	protected final Map<CacheKey,List<Capability>>	providerCache				= new HashMap<CacheKey,List<Capability>>();
	protected final Set<Resource>					optionalRoots				= new HashSet<Resource>();
	protected final ConcurrentMap<Resource,Integer>	resourcePriorities			= new ConcurrentHashMap<Resource,Integer>();

	protected final Comparator<Capability>			capabilityComparator		= new CapabilityComparator();

	protected Set<String>							effectiveSet;

	protected final List<ResolverHook>				resolverHooks				= new ArrayList<ResolverHook>();
	protected final List<ResolutionCallback>		callbacks					= new LinkedList<ResolutionCallback>();

	protected boolean								initialised					= false;
	protected Resource								systemResource;
	protected Resource								inputResource;

	public GenericResolveContext(LogService log) {
		this(Collections.EMPTY_LIST, Collections.EMPTY_LIST, log);
	}

	public GenericResolveContext(List<Capability> systemCapabilities, List<Requirement> systemRequirements,
			LogService log) {
		this.systemCapabilities = systemCapabilities;
		this.systemRequirements = systemRequirements;
		this.log = log;
		systemCapabilityIndex = new CapabilityIndex();
	}

	protected synchronized void init() {
		if (initialised)
			return;

		try {
			failed.clear();

			getSystemResource();
			getInputResource();
			initialised = true;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void addInputRequirement(Requirement requirement) {
		requirements.add(requirement);
	}

	public void addInputCapability(Capability capability) {
		capabilities.add(capability);
	}

	@Override
	public List<Capability> findProviders(Requirement requirement) {
		List<Capability> result = findProviders0(requirement);
		if (result == null || result.isEmpty()) {
			failed.add(requirement);
		}
		return result;
	}

	private List<Capability> findProviders0(Requirement requirement) {

		init();

		List<Capability> result;

		CacheKey cacheKey = getCacheKey(requirement);
		List<Capability> cached = providerCache.get(cacheKey);
		if (cached != null) {
			result = new ArrayList<Capability>(cached);
		} else {
			// First stage: framework and self-capabilities. This should never
			// be reordered by preferences or resolver
			// hooks
			LinkedHashSet<Capability> firstStageResult = new LinkedHashSet<Capability>();

			// The selected OSGi framework always has the first chance to
			// provide the capabilities
			systemCapabilityIndex.appendMatchingCapabilities(requirement, firstStageResult);

			// Next find out if the requirement is satisfied by a capability on
			// the same resource
			Resource resource = requirement.getResource();
			if (resource != null) {
				List<Capability> selfCaps = resource.getCapabilities(requirement.getNamespace());
				if (selfCaps != null) {
					for (Capability selfCap : selfCaps) {
						if (matches(requirement, selfCap))
							firstStageResult.add(selfCap);
					}
				}
			}

			// If the requirement is optional and doesn't come from an optional
			// root resource,
			// then we are done already, no need to look for providers from the
			// repos.
			boolean optional = Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(
					Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
			if (optional && !optionalRoots.contains(requirement.getResource())) {

				result = new ArrayList<Capability>(firstStageResult);
				Collections.sort(result, capabilityComparator);

			} else {

				// Second stage results: repository contents; may be reordered.
				ArrayList<Capability> secondStageResult = new ArrayList<Capability>();

				// Iterate over the repos
				int order = 0;
				ArrayList<Capability> repoCapabilities = new ArrayList<Capability>();
				for (Repository repo : repositories) {
					repoCapabilities.clear();
					Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections
							.singleton(requirement));
					Collection<Capability> capabilities = providers.get(requirement);
					if (capabilities != null && !capabilities.isEmpty()) {
						repoCapabilities.ensureCapacity(capabilities.size());
						for (Capability capability : capabilities) {
							if (isPermitted(capability.getResource())) {
								repoCapabilities.add(capability);
								setResourcePriority(order, capability.getResource());
							}
						}
						secondStageResult.addAll(repoCapabilities);
					}
					order++;
				}
				Collections.sort(secondStageResult, capabilityComparator);

				// Convert second-stage results to a list and post-process
				ArrayList<Capability> secondStageList = new ArrayList<Capability>(secondStageResult);

				// Post-processing second stage results
				postProcessProviders(requirement, firstStageResult, secondStageList);

				// Concatenate both stages, eliminating duplicates between the
				// two
				firstStageResult.addAll(secondStageList);
				result = new ArrayList<Capability>(firstStageResult);
			}
			providerCache.put(cacheKey, result);
		}

		return result;

	}

	private void setResourcePriority(int priority, Resource resource) {
		resourcePriorities.putIfAbsent(resource, priority);
	}

	public static Requirement createBundleRequirement(String bsn, String versionStr) {
		return CapReqBuilder.createBundleRequirement(bsn, versionStr).buildSyntheticRequirement();
	}

	private boolean matches(Requirement requirement, Capability selfCap) {
		boolean match = false;
		try {
			String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			org.osgi.framework.Filter filter = filterStr != null ? org.osgi.framework.FrameworkUtil
					.createFilter(filterStr) : null;

			if (filter == null)
				match = true;
			else
				match = filter.match(new MapToDictionaryAdapter(selfCap.getAttributes()));
		}
		catch (InvalidSyntaxException e) {
			log.log(LogService.LOG_ERROR, "Invalid filter directive on requirement: " + requirement, e);
		}
		return match;
	}

	@Override
	public int insertHostedCapability(List<Capability> caps, HostedCapability hc) {
		Integer prioObj = resourcePriorities.get(hc.getResource());
		int priority = prioObj != null ? prioObj.intValue() : Integer.MAX_VALUE;

		for (int i = 0; i < caps.size(); i++) {
			Capability c = caps.get(i);

			Integer otherPrioObj = resourcePriorities.get(c.getResource());
			int otherPriority = otherPrioObj != null ? otherPrioObj.intValue() : 0;
			if (otherPriority > priority) {
				caps.add(i, hc);
				return i;
			}
		}

		caps.add(hc);
		return caps.size() - 1;
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		init();
		String effective = requirement.getDirectives().get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		if (effective == null || Namespace.EFFECTIVE_RESOLVE.equals(effective))
			return true;

		if (effectiveSet != null && effectiveSet.contains(effective))
			return true;

		return false;
	}

	@Override
	public Map<Resource,Wiring> getWirings() {
		return Collections.emptyMap();
	}

	public void setOptionalRoots(Collection<Resource> roots) {
		this.optionalRoots.clear();
		this.optionalRoots.addAll(roots);
	}

	public void addRepository(Repository repo) {
		repositories.add(repo);
	}

	public List<Requirement> getFailed() {
		return failed;
	}

	private boolean isPermitted(Resource resource) {
		// OSGi frameworks cannot be selected as ordinary resources
		Capability fwkCap = findFrameworkContractCapability(resource);
		if (fwkCap != null) {
			return false;
		}

		// Remove osgi.core and any ee JAR
		List<Capability> idCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (idCaps == null || idCaps.isEmpty()) {
			log.log(LogService.LOG_ERROR, "Resource is missing an identity capability (osgi.identity).");
			return false;
		}
		if (idCaps.size() > 1) {
			log.log(LogService.LOG_ERROR, "Resource has more than one identity capability (osgi.identity).");
			return false;
		}
		String identity = (String) idCaps.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identity == null) {
			log.log(LogService.LOG_ERROR, "Resource is missing an identity capability (osgi.identity).");
			return false;
		}

		if ("osgi.core".equals(identity))
			return false;

		if (identity.startsWith("ee."))
			return false;

		return true;
	}

	protected static Capability findFrameworkContractCapability(Resource resource) {
		List<Capability> contractCaps = resource.getCapabilities(ContractNamespace.CONTRACT_NAMESPACE);
		if (contractCaps != null)
			for (Capability cap : contractCaps) {
				if (CONTRACT_OSGI_FRAMEWORK.equals(cap.getAttributes().get(ContractNamespace.CONTRACT_NAMESPACE)))
					return cap;
			}
		return null;
	}

	public static boolean isInputRequirementResource(Resource resource) {
		Capability id = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
		return IDENTITY_INITIAL_RESOURCE.equals(id.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
	}

	private static CacheKey getCacheKey(Requirement requirement) {
		return new CacheKey(requirement.getNamespace(), requirement.getDirectives(), requirement.getAttributes());
	}

	private static class CacheKey {
		final String				namespace;
		final Map<String,String>	directives;
		final Map<String,Object>	attributes;
		final int					hashcode;

		CacheKey(String namespace, Map<String,String> directives, Map<String,Object> attributes) {
			this.namespace = namespace;
			this.directives = directives;
			this.attributes = attributes;
			this.hashcode = calculateHashCode(namespace, directives, attributes);
		}

		private static int calculateHashCode(String namespace, Map<String,String> directives,
				Map<String,Object> attributes) {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
			result = prime * result + ((directives == null) ? 0 : directives.hashCode());
			result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
			return result;
		}

		@Override
		public int hashCode() {
			return hashcode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (attributes == null) {
				if (other.attributes != null)
					return false;
			} else if (!attributes.equals(other.attributes))
				return false;
			if (directives == null) {
				if (other.directives != null)
					return false;
			} else if (!directives.equals(other.directives))
				return false;
			if (namespace == null) {
				if (other.namespace != null)
					return false;
			} else if (!namespace.equals(other.namespace))
				return false;
			return true;
		}

	}

	static Version getVersion(Capability cap, String attr) {
		Object versionatt = cap.getAttributes().get(attr);
		if (versionatt instanceof Version)
			return (Version) versionatt;
		else if (versionatt instanceof String)
			return Version.parseVersion((String) versionatt);
		else
			return Version.emptyVersion;
	}

	private class CapabilityComparator implements Comparator<Capability> {

		public CapabilityComparator() {}

		public int compare(Capability o1, Capability o2) {

			Resource res1 = o1.getResource();
			Resource res2 = o2.getResource();

			// 1. Framework bundle
			if (res1 == getSystemResource())
				return -1;
			if (res2 == getSystemResource())
				return +1;

			// 2. Wired
			Map<Resource,Wiring> wirings = getWirings();
			Wiring w1 = wirings.get(res1);
			Wiring w2 = wirings.get(res2);

			if (w1 != null && w2 == null)
				return -1;
			if (w1 == null && w2 != null)
				return +1;

			// 3. Input requirements
			if (isInputRequirementResource(res1)) {
				if (!isInputRequirementResource(res2))
					return -1;
			}
			if (isInputRequirementResource(res2)) {
				if (!isInputRequirementResource(res1))
					return +1;
			}

			// 4. Higher package version
			String ns1 = o1.getNamespace();
			String ns2 = o2.getNamespace();
			if (PACKAGE_NAMESPACE.equals(ns1) && PACKAGE_NAMESPACE.equals(ns2)) {
				Version v1 = getVersion(o1, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				Version v2 = getVersion(o2, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				if (!v1.equals(v2))
					return v2.compareTo(v1);
			}

			// 5. Higher resource version
			if (BUNDLE_NAMESPACE.equals(ns1) && BUNDLE_NAMESPACE.equals(ns2)) {
				Version v1 = getVersion(o1, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
				Version v2 = getVersion(o2, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
				if (!v1.equals(v2))
					return v2.compareTo(v1);
			} else if (IdentityNamespace.IDENTITY_NAMESPACE.equals(ns1)
					&& IdentityNamespace.IDENTITY_NAMESPACE.equals(ns2)) {
				Version v1 = getVersion(o1, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				Version v2 = getVersion(o2, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				if (!v1.equals(v2))
					return v2.compareTo(v1);
			}

			// 6. Same package version, higher bundle version
			if (PACKAGE_NAMESPACE.equals(ns1) && PACKAGE_NAMESPACE.equals(ns2)) {
				String bsn1 = (String) o1.getAttributes().get(aQute.bnd.osgi.Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE);
				String bsn2 = (String) o2.getAttributes().get(aQute.bnd.osgi.Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE);
				if (bsn1 != null && bsn1.equals(bsn2)) {
					Version v1 = getVersion(o1, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
					Version v2 = getVersion(o2, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
					if (!v1.equals(v2))
						return v2.compareTo(v1);
				}
			}

			// 7. The resource with the fewest requirements
			int diff = res1.getRequirements(null).size() - res2.getRequirements(null).size();
			if (diff != 0)
				return diff;

			// 8. The resource with most capabilities
			return res2.getCapabilities(null).size() - res1.getCapabilities(null).size();
		}
	}

	public Resource getInputResource() {
		if (inputResource != null) {
			return inputResource;
		}
		ResourceBuilder resBuilder = new ResourceBuilder();
		CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addAttribute(
				IdentityNamespace.IDENTITY_NAMESPACE, IDENTITY_INITIAL_RESOURCE);
		resBuilder.addCapability(identity);

		for (Requirement req : requirements) {
			resBuilder.addRequirement(req);
		}
		for (Capability cap : capabilities) {
			resBuilder.addCapability(cap);
		}
		inputResource = resBuilder.build();
		return inputResource;
	}

	public Resource getSystemResource() {
		if (systemResource != null) {
			return systemResource;
		}
		ResourceBuilder resBuilder = new ResourceBuilder();
		for (Requirement req : systemRequirements) {
			resBuilder.addRequirement(req);
		}
		for (Capability cap : systemCapabilities) {
			resBuilder.addCapability(cap);
		}
		systemResource = resBuilder.build();
		for (Capability capability : systemResource.getCapabilities(null)) {
			systemCapabilityIndex.addCapability(capability);
		}
		return systemResource;

	}

	@Override
	public Collection<Resource> getMandatoryResources() {
		init();
		List<Resource> resources = new ArrayList<Resource>();
		Resource resource = getSystemResource();
		if (resource != null) {
			resources.add(resource);
		}
		resource = getInputResource();
		if (resource != null) {
			resources.add(resource);
		}
		return resources;
	}

	public void addEffectiveDirective(String effectiveDirective) {
		this.effectiveSet.add(effectiveDirective);
	}

	protected void postProcessProviders(Requirement requirement, Set<Capability> wired, List<Capability> candidates) {
		if (candidates.size() == 0)
			return;

		// Call resolver hooks
		for (ResolverHook resolverHook : resolverHooks) {
			resolverHook.filterMatches(requirement, candidates);
		}

		// If preferences were applied, then don't need to call the callbacks
		for (ResolutionCallback callback : callbacks) {
			callback.processCandidates(requirement, wired, candidates);
		}
	}

	public void addResolverHook(ResolverHook resolverHook) {
		resolverHooks.add(resolverHook);
	}

	public void addCallbacks(Collection<ResolutionCallback> callbacks) {
		this.callbacks.addAll(callbacks);
	}

	public static Requirement createIdentityRequirement(String identity, String versionRange) {
		// Construct a filter & requirement to find matches
		Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, identity);
		if (versionRange != null)
			filter = new AndFilter().addChild(filter).addChild(
					new LiteralFilter(Filters.fromVersionRange(versionRange)));
		Requirement frameworkReq = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(
				Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
		return frameworkReq;
	}

	public boolean isInputResource(Resource resource) {
		return resource == inputResource;
	}

	public boolean isSystemResource(Resource resource) {
		return resource == systemResource;
	}

	public static Resource getFrameworkResource(List<Repository> repos, String bsn, String version) {

		Requirement frameworkRequirement = GenericResolveContext.createIdentityRequirement(bsn, version);
		if (frameworkRequirement == null) {
			return null;
		}
		Version current = null;
		Resource result = null;
		for (Repository repo : repos) {
			Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections
					.singletonList(frameworkRequirement));
			Collection<Capability> frameworkCaps = providers.get(frameworkRequirement);
			if (frameworkCaps != null) {
				for (Capability frameworkCap : frameworkCaps) {
					if (findFrameworkContractCapability(frameworkCap.getResource()) != null) {
						Version foundVersion = toVersion(frameworkCap.getAttributes().get(
								IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
						if (foundVersion != null) {
							if (current == null || (foundVersion.compareTo(current) > 0)) {
								result = frameworkCap.getResource();
								ResourceBuilder resBuilder = new ResourceBuilder();
								for (Capability cap : result.getCapabilities(null)) {
									resBuilder.addCapability(cap);
								}
								for (Requirement req : result.getRequirements(null)) {
									resBuilder.addRequirement(req);
								}
								// Add system.bundle alias
						        Version frameworkVersion = Utils.findIdentityVersion(result);
						        resBuilder.addCapability(new CapReqBuilder(BundleNamespace.BUNDLE_NAMESPACE).addAttribute(BundleNamespace.BUNDLE_NAMESPACE, Constants.SYSTEM_BUNDLE_SYMBOLICNAME)
						                .addAttribute(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, frameworkVersion).setResource(result).buildCapability());
						        resBuilder.addCapability(new CapReqBuilder(HostNamespace.HOST_NAMESPACE).addAttribute(HostNamespace.HOST_NAMESPACE, Constants.SYSTEM_BUNDLE_SYMBOLICNAME)
						                .addAttribute(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, frameworkVersion).setResource(result).buildCapability());
								result = resBuilder.build();
								current = foundVersion;
							}
						}
					}
				}
			}
		}
		return result;
	}

	protected static Version toVersion(Object object) throws IllegalArgumentException {
		if (object == null)
			return null;

		if (object instanceof Version)
			return (Version) object;

		if (object instanceof String)
			return Version.parseVersion((String) object);

		throw new IllegalArgumentException(MessageFormat.format("Cannot convert type {0} to Version.", object
				.getClass().getName()));
	}

	public static List<Capability> getEECapabilities(EE ee) {

		List<Capability> caps = new ArrayList<Capability>();
		addEECapability(ee, caps);
		for (EE compatibleEE : ee.getCompatible()) {
			addEECapability(compatibleEE, caps);
		}
		getJRECapabilities(ee);
		return caps;
	}

	public static List<Capability> getJRECapabilities(EE ee) {

		List<Capability> caps = new ArrayList<Capability>();
		loadJREPackages(ee, caps);
		return caps;
	}

	private static void addEECapability(EE ee, List<Capability> capabilities) {
		CapReqBuilder builder;

		// Correct version according to R5 specification section 3.4.1
		// BREE J2SE-1.4 ==> osgi.ee=JavaSE, version:Version=1.4
		// See bug 329, https://github.com/bndtools/bnd/issues/329
		builder = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, ee.getCapabilityName());
		builder.addAttribute(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, ee.getCapabilityVersion());
		capabilities.add(builder.buildSyntheticCapability());

		// Compatibility with old version...
		builder = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		builder.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, ee.getEEName());
		capabilities.add(builder.buildSyntheticCapability());

	}

	protected static void loadJREPackages(EE ee, List<Capability> capabilities) {
		InputStream stream = FrameworkResourceRepository.class.getResourceAsStream(ee.name() + ".properties");
		if (stream != null) {
			try {
				Properties properties = new Properties();
				properties.load(stream);

				Parameters params = new Parameters(properties.getProperty("org.osgi.framework.system.packages", ""));
				for (String packageName : params.keySet()) {
					CapReqBuilder builder = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
					builder.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, packageName);
					builder.addAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(0, 0, 0));
					Capability cap = builder.buildSyntheticCapability();
					capabilities.add(cap);
				}
			}
			catch (IOException e) {
				throw new IllegalStateException("Error loading JRE package properties", e);
			}
		}
	}

	public static Capability createPackageCapability(String packageName, String versionString) {
		CapReqBuilder builder = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
		builder.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, packageName);
		Version version = versionString != null ? new Version(versionString) : Version.emptyVersion;
		builder.addAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
		return builder.buildSyntheticCapability();
	}
}
