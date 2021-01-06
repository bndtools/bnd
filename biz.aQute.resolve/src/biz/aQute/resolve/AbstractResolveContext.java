package biz.aQute.resolve;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;
import static org.osgi.namespace.contract.ContractNamespace.CONTRACT_NAMESPACE;
import static org.osgi.service.repository.ContentNamespace.CONTENT_NAMESPACE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import aQute.bnd.deployer.repository.CapabilityIndex;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.service.resolve.hook.ResolverHook;
import aQute.bnd.version.VersionRange;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.IO;
import aQute.lib.unmodifiable.Sets;

/**
 * This is the Resolve Context as outlined in the Resolver specification. It
 * manages the access to the repository and orders the capabilities. It also
 * provides the capabilities of the environment.
 */
public abstract class AbstractResolveContext extends ResolveContext {

	/**
	 * These are the namespaces that we ignore when we copy capabilities from
	 * -runpath resources.
	 */
	final static Set<String>						IGNORED_NAMESPACES_FOR_SYSTEM_RESOURCES	= Sets
		.of(IDENTITY_NAMESPACE, CONTENT_NAMESPACE, BUNDLE_NAMESPACE, HOST_NAMESPACE);

	/**
	 * The 'OSGiFramework' contract was something invented by the old indexer
	 * which is no longer in use.
	 */
	@Deprecated
	protected static final String					CONTRACT_OSGI_FRAMEWORK					= "OSGiFramework";
	protected static final String					IDENTITY_INITIAL_RESOURCE				= Constants.IDENTITY_INITIAL_RESOURCE;
	protected static final String					IDENTITY_SYSTEM_RESOURCE				= Constants.IDENTITY_SYSTEM_RESOURCE;

	protected final LogService						log;
	private final CapabilityIndex					systemCapabilityIndex					= new CapabilityIndex();
	private final List<Repository>					repositories							= new ArrayList<>();
	private final List<Requirement>					failed									= new ArrayList<>();
	private final Map<CacheKey, List<Capability>>	providerCache							= new HashMap<>();
	private final Set<Resource>						optionalRoots							= new HashSet<>();
	private final ConcurrentMap<Resource, Integer>	resourcePriorities						= new ConcurrentHashMap<>();
	private final Comparator<Capability>			capabilityComparator;
	private Map<String, Set<String>>				effectiveSet							= new HashMap<>();
	private final List<ResolverHook>				resolverHooks							= new ArrayList<>();
	private final List<ResolutionCallback>			callbacks								= new LinkedList<>();
	private boolean									initialized								= false;
	private Resource								systemResource;
	private Resource								inputResource;
	private Set<Resource>							blacklistedResources					= new HashSet<>();
	private int										level									= 0;
	private Resource								framework;
	private final AtomicBoolean						reported								= new AtomicBoolean();

	public AbstractResolveContext(LogService log) {
		this.log = log;
		this.capabilityComparator = new CapabilityComparator(log);
	}

	protected synchronized void init() {
		if (initialized)
			return;

		try {
			initialized = true;

			failed.clear();
			systemCapabilityIndex.addResource(getSystemResource());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void initAndReport() {
		init();
		if ((getLevel() > 0) && reported.compareAndSet(false, true)) {
			DebugReporter dr = new DebugReporter(System.out, this, level);
			dr.report();
		}
	}

	@Override
	public List<Capability> findProviders(Requirement requirement) {
		initAndReport();
		List<Capability> result = findProviders0(requirement);
		if (result.isEmpty()) {
			failed.add(requirement);
		}
		return result;
	}

	@Override
	public Collection<Resource> getMandatoryResources() {
		initAndReport();
		return Collections.singleton(getInputResource());
	}

	@Override
	public int insertHostedCapability(List<Capability> caps, HostedCapability hc) {
		initAndReport();
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

		int newIndex = caps.size();
		// the List passed by Felix does not support the
		// single-arg version of add()... it throws
		// UnsupportedOperationException
		caps.add(newIndex, hc);
		return newIndex;
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		initAndReport();
		String effective = requirement.getDirectives()
			.get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		if (effective == null || Namespace.EFFECTIVE_RESOLVE.equals(effective))
			return true;

		if (effectiveSet != null && effectiveSet.containsKey(effective) && !effectiveSet.get(effective)
			.contains(requirement.getNamespace()))
			return true;

		return false;
	}

	@Override
	public Map<Resource, Wiring> getWirings() {
		initAndReport();
		return Collections.emptyMap();
	}

	private List<Capability> findProviders0(Requirement requirement) {
		List<Capability> cached = providerCache.computeIfAbsent(getCacheKey(requirement), k -> {
			// First stage: framework and self-capabilities. This should never
			// be reordered by preferences or resolver
			// hooks
			LinkedHashSet<Capability> firstStageResult = new LinkedHashSet<>();

			// The selected OSGi framework always has the first chance to
			// provide the capabilities
			systemCapabilityIndex.appendMatchingCapabilities(requirement, firstStageResult);

			// Next find out if the requirement is satisfied by a capability on
			// the same resource
			processMandatoryResource(requirement, firstStageResult, requirement.getResource());
			// Next find out if the requirement is satisfied by a capability on
			// a Mandatory resource
			for (Resource res : getMandatoryResources()) {
				processMandatoryResource(requirement, firstStageResult, res);
			}

			// If the requirement is optional and doesn't come from an optional
			// root resource,
			// then we are done already, no need to look for providers from the
			// repos.
			boolean optional = Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives()
				.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
			List<Capability> result = new ArrayList<>(firstStageResult);
			Collections.sort(result, capabilityComparator);
			if (!optional || optionalRoots.contains(requirement.getResource())) {
				// We sort capabilities from the same resource and mandatory
				// resources (first stage) BEFORE capabilities from repo
				// resources (second stage) removing any duplicate capabilities.
				findProvidersFromRepositories(requirement, firstStageResult).stream()
					.filter(provider -> !result.contains(provider))
					.sorted(capabilityComparator)
					.forEach(result::add);
			}
			return result;
		});
		List<Capability> capabilities = new ArrayList<>(cached);
		log.log(LogService.LOG_DEBUG, "for " + requirement + " found " + capabilities);
		return capabilities;
	}

	protected void processMandatoryResource(Requirement requirement, LinkedHashSet<Capability> firstStageResult,
		Resource resource) {
		if (resource != null) {
			ResourceUtils.capabilityStream(resource, requirement.getNamespace())
				.filter(ResourceUtils.matcher(requirement))
				.forEachOrdered(firstStageResult::add);
		}
	}

	protected ArrayList<Capability> findProvidersFromRepositories(Requirement requirement,
		LinkedHashSet<Capability> existingWiredCapabilities) {
		// Second stage results: repository contents.
		Set<Capability> set = new LinkedHashSet<>();

		// Iterate over the repos
		int order = 0;
		for (Repository repo : repositories) {
			for (Capability capability : findProviders(repo, requirement)) {
				if (isPermitted(capability.getResource()) && ResourceUtils.isEffective(requirement, capability)) {
					if (set.add(capability)) {
						setResourcePriority(order, capability.getResource());
					}
				}
			}
			order++;
		}

		// Convert second-stage results to a list and post-process
		ArrayList<Capability> capabilities = set.stream()
			.sorted(capabilityComparator)
			.collect(toCollection(ArrayList::new));

		// Post-processing second stage results
		postProcessProviders(requirement, existingWiredCapabilities, capabilities);
		return capabilities;
	}

	/**
	 * Return any capabilities from the given repo. This method will filter the
	 * blacklist.
	 *
	 * @param repo The repo to fetch requirements from
	 * @param requirement the requirement
	 * @return a list of caps for the asked requirement minus and capabilities
	 *         that are skipped.
	 */
	protected Collection<Capability> findProviders(Repository repo, Requirement requirement) {
		Map<Requirement, Collection<Capability>> map = repo.findProviders(Collections.singleton(requirement));
		Collection<Capability> caps = map.get(requirement);
		caps.removeIf(capability -> blacklistedResources.contains(capability.getResource()));
		return caps;
	}

	private void setResourcePriority(int priority, Resource resource) {
		resourcePriorities.putIfAbsent(resource, priority);
	}

	public static Requirement createBundleRequirement(String bsn, String versionStr) {
		return CapReqBuilder.createBundleRequirement(bsn, versionStr)
			.buildSyntheticRequirement();
	}

	public void setOptionalRoots(Collection<Resource> roots) {
		this.optionalRoots.clear();
		this.optionalRoots.addAll(roots);
	}

	public void addRepository(Repository repo) {
		repositories.add(repo);
	}

	public List<Repository> getRepositories() {
		return repositories;
	}

	public List<Requirement> getFailed() {
		return failed;
	}

	private boolean isPermitted(Resource resource) {
		// OSGi frameworks cannot be selected as ordinary resources.
		// We assume any exporter of the org.osgi.framework package is
		// either a framework impl or osgi.core jar and is not meant
		// to be used as a bundle.
		if (resource.getCapabilities(PACKAGE_NAMESPACE)
			.stream()
			.anyMatch(c -> Objects.equals(c.getAttributes()
				.get(PACKAGE_NAMESPACE), "org.osgi.framework"))) {
			return false;
		}

		// Remove any jars without an identity capability
		List<Capability> idCaps = resource.getCapabilities(IDENTITY_NAMESPACE);
		if (idCaps.isEmpty()) {
			log.log(LogService.LOG_ERROR, "Resource is missing an identity capability (osgi.identity).");
			return false;
		}
		if (idCaps.size() > 1) {
			log.log(LogService.LOG_ERROR, "Resource has more than one identity capability (osgi.identity).");
			return false;
		}
		String identity = (String) idCaps.get(0)
			.getAttributes()
			.get(IDENTITY_NAMESPACE);
		if (identity == null) {
			log.log(LogService.LOG_ERROR, "Resource is missing an identity capability (osgi.identity).");
			return false;
		}

		// Remove any ee JAR
		if (identity.startsWith("ee."))
			return false;

		return true;
	}

	/**
	 * This method is BROKEN. The 'OSGiFramework' contract was something
	 * invented by the old indexer which is no longer in use.
	 */
	@Deprecated
	protected static Capability findFrameworkContractCapability(Resource resource) {
		List<Capability> contractCaps = resource.getCapabilities(CONTRACT_NAMESPACE);
		if (contractCaps != null)
			for (Capability cap : contractCaps) {
				if (CONTRACT_OSGI_FRAMEWORK.equals(cap.getAttributes()
					.get(CONTRACT_NAMESPACE)))
					return cap;
			}
		return null;
	}

	private static CacheKey getCacheKey(Requirement requirement) {
		return new CacheKey(requirement.getNamespace(), requirement.getDirectives(), requirement.getAttributes(),
			requirement.getResource());
	}

	private static class CacheKey {
		final String				namespace;
		final Map<String, String>	directives;
		final Map<String, Object>	attributes;
		final Resource				resource;
		final int					hashcode;

		CacheKey(String namespace, Map<String, String> directives, Map<String, Object> attributes, Resource resource) {
			this.namespace = namespace;
			this.directives = directives;
			this.attributes = attributes;
			this.resource = resource;
			this.hashcode = calculateHashCode(namespace, directives, attributes, resource);
		}

		private static int calculateHashCode(String namespace, Map<String, String> directives,
			Map<String, Object> attributes, Resource resource) {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
			result = prime * result + ((directives == null) ? 0 : directives.hashCode());
			result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
			result = prime * result + ((resource == null) ? 0 : resource.hashCode());
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
			if (!Objects.equals(attributes, other.attributes)) {
				return false;
			}
			if (!Objects.equals(directives, other.directives)) {
				return false;
			}
			if (!Objects.equals(namespace, other.namespace)) {
				return false;
			}
			if (resource == null) {
				if (other.resource != null)
					return false;
			} else if (!resourceIdentityEquals(resource, other.resource))
				return false;
			return true;
		}

	}

	static Version getVersion(Capability cap, String attr) {
		Object versionatt = cap.getAttributes()
			.get(attr);
		if (versionatt instanceof Version)
			return (Version) versionatt;
		else if (versionatt instanceof String)
			return Version.parseVersion((String) versionatt);
		else
			return Version.emptyVersion;
	}

	private class CapabilityComparator implements Comparator<Capability> {

		private final LogService log;

		public CapabilityComparator(LogService log) {
			this.log = log;
		}

		@Override
		public int compare(Capability o1, Capability o2) {

			Resource res1 = o1.getResource();
			Resource res2 = o2.getResource();

			// 1. Framework bundle
			if (isSystemResource(res1))
				return -1;
			if (isSystemResource(res2))
				return +1;

			// 2. Wired
			Map<Resource, Wiring> wirings = getWirings();
			Wiring w1 = wirings.get(res1);
			Wiring w2 = wirings.get(res2);

			if (w1 != null && w2 == null)
				return -1;
			if (w1 == null && w2 != null)
				return +1;

			// 3. Input requirements
			if (isInputResource(res1)) {
				if (!isInputResource(res2))
					return -1;
			}
			if (isInputResource(res2)) {
				if (!isInputResource(res1))
					return +1;
			}

			// 4. Higher capability version
			String ns1 = o1.getNamespace();
			String ns2 = o2.getNamespace();
			if (ns1.equals(ns2)) {
				try {
					// We use package namespace, as that defines the general
					// contract for versions
					Version v1 = getVersion(o1, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
					Version v2 = getVersion(o2, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
					if (!v1.equals(v2))
						return v2.compareTo(v1);
				} catch (Exception e) {
					log.log(LogService.LOG_INFO,
						"Unable to determine the versions of the capabilities " + o1 + " and " + o2, e);
				}
			}

			// 5. Higher resource version
			if (BUNDLE_NAMESPACE.equals(ns1) && BUNDLE_NAMESPACE.equals(ns2)) {
				Version v1 = getVersion(o1, AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
				Version v2 = getVersion(o2, AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
				if (!v1.equals(v2))
					return v2.compareTo(v1);
			} else if (IDENTITY_NAMESPACE.equals(ns1) && IDENTITY_NAMESPACE.equals(ns2)) {
				Version v1 = getVersion(o1, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				Version v2 = getVersion(o2, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				if (!v1.equals(v2))
					return v2.compareTo(v1);
			}

			// 6. Same package version, higher bundle version
			if (PACKAGE_NAMESPACE.equals(ns1) && PACKAGE_NAMESPACE.equals(ns2)) {
				String bsn1 = (String) o1.getAttributes()
					.get(aQute.bnd.osgi.Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE);
				String bsn2 = (String) o2.getAttributes()
					.get(aQute.bnd.osgi.Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE);
				if (bsn1 != null && bsn1.equals(bsn2)) {
					Version v1 = getVersion(o1, AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
					Version v2 = getVersion(o2, AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
					if (!v1.equals(v2))
						return v2.compareTo(v1);
				}
				// 6.5 Higher bundle version for other namespace's
			} else if (ns1.equals(ns2)) {
				IdentityCapability idCap1 = ResourceUtils.getIdentityCapability(res1);
				IdentityCapability idCap2 = ResourceUtils.getIdentityCapability(res2);

				if (idCap1 != null && idCap2 != null && idCap1.osgi_identity()
					.equals(idCap2.osgi_identity())) {
					if (!idCap1.version()
						.equals(idCap2.version()))
						return idCap2.version()
							.compareTo(idCap1.version());
				}
			}

			// 7. The resource with the fewest requirements
			int diff = res1.getRequirements(null)
				.size()
				- res2.getRequirements(null)
					.size();
			if (diff != 0)
				return diff;

			// 8. The resource with most capabilities
			return res2.getCapabilities(null)
				.size()
				- res1.getCapabilities(null)
					.size();
		}
	}

	public Resource getInputResource() {
		return inputResource;
	}

	public void setInputResource(Resource inputResource) {
		this.inputResource = inputResource;
	}

	public Resource getSystemResource() {
		return systemResource;
	}

	public void setSystemResource(Resource system) {
		systemResource = system;
	}

	public void addEffectiveDirective(String effectiveDirective) {
		this.effectiveSet.put(effectiveDirective, new HashSet<>());
	}

	public void addEffectiveDirective(String effectiveDirective, Set<String> excludedNamespaces) {
		this.effectiveSet.put(effectiveDirective, excludedNamespaces != null ? excludedNamespaces : new HashSet<>());
	}

	public void addEffectiveSet(Map<String, Set<String>> effectiveSet) {
		this.effectiveSet.putAll(effectiveSet);
	}

	protected void postProcessProviders(Requirement requirement, Set<Capability> wired, List<Capability> candidates) {
		if (candidates.isEmpty())
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
		Requirement frameworkReq = CapReqBuilder.createBundleRequirement(identity, versionRange)
			.buildSyntheticRequirement();
		return frameworkReq;
	}

	public boolean isInputResource(Resource resource) {
		return AbstractResolveContext.resourceIdentityEquals(resource, getInputResource());
	}

	public boolean isSystemResource(Resource resource) {
		return AbstractResolveContext.resourceIdentityEquals(resource, getSystemResource());
	}

	public Resource getHighestResource(String bsn, String range) {
		List<Resource> resources = getResources(getRepositories(), bsn, range);
		if (resources.isEmpty())
			return null;

		Collections.sort(resources, Collections.reverseOrder(ResourceUtils.IDENTITY_VERSION_COMPARATOR));
		return resources.get(0);
	}

	/**
	 * Get the framework repository from the
	 *
	 * @param repos
	 * @param bsn
	 */
	public List<Resource> getResources(List<Repository> repos, String bsn, String range) {
		Requirement bundle = CapReqBuilder.createBundleRequirement(bsn, range)
			.buildSyntheticRequirement();
		return getResources(repos, bundle);
	}

	public List<Resource> getResources(List<Repository> repos, Requirement req) {

		Set<Resource> resources = new HashSet<>();

		for (Repository repo : repos) {
			Collection<Capability> providers = findProviders(repo, req);
			resources.addAll(ResourceUtils.getResources(providers));
		}
		return new ArrayList<>(resources);
	}

	private static final TypeReference<List<String>> LIST_STRING = new TypeReference<List<String>>() {};

	/**
	 * Add a framework resource to the system resource builder
	 *
	 * @param system the system resource being build up
	 * @param framework the framework resource
	 * @throws Exception
	 */

	protected void setFramework(ResourceBuilder system, Resource framework) throws Exception {
		this.framework = requireNonNull(framework);

		//
		// We copy the framework capabilities and add system.bundle alias
		//
		for (Capability cap : framework.getCapabilities(null)) {
			CapReqBuilder builder = CapReqBuilder.clone(cap);
			String namespace = cap.getNamespace();
			switch (namespace) {
				case BUNDLE_NAMESPACE :
				case HOST_NAMESPACE : {
					List<String> names = Converter.cnv(LIST_STRING, cap.getAttributes()
						.get(namespace));
					if (!names.contains(SYSTEM_BUNDLE_SYMBOLICNAME)) {
						names.add(SYSTEM_BUNDLE_SYMBOLICNAME);
						builder.addAttribute(namespace, names);
					}
					break;
				}
			}
			system.addCapability(builder);
		}
	}

	/*
	 * Add all the capabilities from a system resource, i.e. something on
	 * -runpath
	 */
	protected void addSystemResource(ResourceBuilder system, Resource resource) throws Exception {
		system.copyCapabilities(IGNORED_NAMESPACES_FOR_SYSTEM_RESOURCES, resource);

	}

	protected static Version toVersion(Object object) throws IllegalArgumentException {
		if (object == null)
			return null;

		if (object instanceof Version)
			return (Version) object;

		if (object instanceof String)
			return Version.parseVersion((String) object);

		throw new IllegalArgumentException(MessageFormat.format("Cannot convert type {0} to Version.", object.getClass()
			.getName()));
	}

	public static Repository createRepository(final List<Resource> resources) {
		return new ResourcesRepository(resources);
	}

	public static Capability createPackageCapability(String packageName, String versionString) throws Exception {
		Attrs attrs = (versionString != null)
			? Attrs.create(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE + ":Version", versionString)
			: null;
		CapReqBuilder builder = CapReqBuilder.createPackageCapability(packageName, attrs, null, null);
		return builder.buildSyntheticCapability();
	}

	public static boolean resourceIdentityEquals(Resource r1, Resource r2) {
		String id1 = getResourceIdentity(r1);
		String id2 = getResourceIdentity(r2);
		if (id1 != null && id1.equals(id2)) {
			Version v1 = getResourceVersion(r1);
			Version v2 = getResourceVersion(r2);
			if ((v1 == null && v2 == null) || (v1 != null && v1.equals(v2))) {
				return true;
			}
		}
		return false;
	}

	public static Capability getIdentityCapability(Resource resource) {
		if (resource == null) {
			return null;
		}
		List<Capability> identityCaps = resource.getCapabilities(IDENTITY_NAMESPACE);
		if (identityCaps.isEmpty()) {
			return null;
		}
		return identityCaps.iterator()
			.next();
	}

	public static String getResourceIdentity(Resource resource) {
		Capability cap = getIdentityCapability(resource);
		if (cap == null) {
			return null;
		}
		return (String) cap.getAttributes()
			.get(IDENTITY_NAMESPACE);
	}

	public static Version getResourceVersion(Resource resource) {
		Capability cap = getIdentityCapability(resource);
		if (cap == null) {
			return null;
		}
		return getVersion(cap, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
	}

	/**
	 * If the blacklist is set, we have a list of requirements of resources that
	 * should not be included (blacklist). We try to find those resources and
	 * add them to the blacklistedResources
	 */
	protected void setBlackList(Collection<Requirement> reject) {
		for (Repository repo : repositories) {
			Map<Requirement, Collection<Capability>> caps = repo.findProviders(reject);
			for (Entry<Requirement, Collection<Capability>> entry : caps.entrySet()) {
				for (Capability cap : entry.getValue()) {
					blacklistedResources.add(cap.getResource());
				}
			}
		}
	}

	public List<ResolutionCallback> getCallbacks() {
		return callbacks;
	}

	public Set<Resource> getBlackList() {
		return blacklistedResources;
	}

	public void setLevel(int n) {
		this.level = n;
	}

	public int getLevel() {
		return level;
	}

	public Resource getFramework() {
		return framework;
	}

	/**
	 * Load a bnd path from the OSGi repositories. We assume the highest version
	 * allowed. This mimics Project.getBundles()
	 *
	 * @param system
	 * @param path
	 * @param what
	 * @throws IOException
	 * @throws Exception
	 */
	public void loadPath(ResourceBuilder system, String path, String what) throws IOException, Exception {
		Parameters p = new Parameters(path);
		if (p.isEmpty())
			return;

		for (Entry<String, Attrs> e : p.entrySet()) {
			String bsn = Processor.removeDuplicateMarker(e.getKey());
			String version = e.getValue()
				.getVersion();

			Resource resource;

			if ("latest".equals(version) || "snapshot".equals(version))
				version = null;

			if ("file".equals(version)) {
				File f = IO.getFile(bsn);
				if (f.isFile()) {
					try (InputStream fin = IO.stream(f)) {
						Manifest m;

						if (f.getName()
							.endsWith(".mf"))
							m = new Manifest(fin);
						else {
							try (JarInputStream jin = new JarInputStream(fin)) {
								m = jin.getManifest();
							}
						}
						if (m != null) {
							ResourceBuilder rb = new ResourceBuilder();
							rb.addManifest(Domain.domain(m));
							resource = rb.build();
						} else {
							continue; // ok to have no manifest, might be a jar
						}
					}
				} else {
					log.log(LogService.LOG_ERROR,
						"Found fileresource " + bsn + ";" + version + " but file does not exist");
					continue;
				}
			} else if (version == null || VersionRange.isVersionRange(version)) {
				resource = getHighestResource(bsn, version);
				if (resource == null) {
					log.log(LogService.LOG_ERROR, "Could not find resource " + bsn + ";" + version);
				}
			} else {
				log.log(LogService.LOG_ERROR, "Cannot find resource " + bsn + ";" + version);
				continue;
			}

			addSystemResource(system, resource);
		}

	}

	public void setInputRequirements(Requirement... reqs) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		for (Requirement r : reqs) {
			rb.addRequirement(r);
		}
		setInputResource(rb.build());
	}

	public Map<String, Set<String>> getEffectiveSet() {
		return effectiveSet;
	}
}
