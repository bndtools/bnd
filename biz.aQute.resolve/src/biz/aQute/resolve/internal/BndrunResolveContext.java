package biz.aQute.resolve.internal;

import static org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import biz.aQute.resolve.ResolutionCallback;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.deployer.repository.MapToDictionaryAdapter;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.Filters;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.Registry;
import aQute.bnd.service.resolve.hook.ResolverHook;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.LiteralFilter;
import aQute.libg.filters.SimpleFilter;

public class BndrunResolveContext extends ResolveContext {

    private static final String CONTRACT_OSGI_FRAMEWORK = "OSGiFramework";
    private static final String IDENTITY_INITIAL_RESOURCE = "<<INITIAL>>";

    public static final String RUN_EFFECTIVE_INSTRUCTION = "-resolve.effective";
    public static final String PROP_RESOLVE_PREFERENCES = "-resolve.preferences";

    private final List<Repository> repos = new LinkedList<Repository>();
    private final ConcurrentMap<Resource,Integer> resourcePriorities = new ConcurrentHashMap<Resource,Integer>();

    private final Map<CacheKey,List<Capability>> providerCache = new HashMap<CacheKey,List<Capability>>();

    private final Comparator<Capability> capabilityComparator = new CapabilityComparator();

    private final BndEditModel runModel;
    private final Registry registry;
    private final List<ResolutionCallback> callbacks = new LinkedList<ResolutionCallback>();
    private final LogService log;
    private final Set<Resource> optionalRoots = new HashSet<Resource>();

    private boolean initialised = false;

    private Resource frameworkResource = null;
    private Version frameworkResourceVersion = null;
    private FrameworkResourceRepository frameworkResourceRepo;

    private Resource inputRequirementsResource = null;
    private EE ee;
    private Set<String> effectiveSet;
    private List<ExportedPackage> sysPkgsExtra;
    private Parameters sysCapsExtraParams;
    private Parameters resolvePrefs;

    public BndrunResolveContext(BndEditModel runModel, Registry registry, LogService log) {
        this.runModel = runModel;
        this.registry = registry;
        this.log = log;
    }

    public void setOptionalRoots(Collection<Resource> roots) {
        this.optionalRoots.clear();
        this.optionalRoots.addAll(roots);
    }

    public void addCallbacks(Collection<ResolutionCallback> callbacks) {
        this.callbacks.addAll(callbacks);
    }

    protected synchronized void init() {
        if (initialised)
            return;

        loadEE();
        loadSystemPackagesExtra();
        loadSystemCapabilitiesExtra();
        loadRepositories();
        loadEffectiveSet();
        findFramework();
        constructInputRequirements();
        loadPreferences();

        initialised = true;
    }

    private void loadEE() {
        EE tmp = runModel.getEE();
        ee = (tmp != null) ? tmp : EE.JavaSE_1_6;
    }

    private void loadSystemPackagesExtra() {
        sysPkgsExtra = runModel.getSystemPackages();
    }

    private void loadSystemCapabilitiesExtra() {
        String header = (String) runModel.genericGet(Constants.RUNSYSTEMCAPABILITIES);
        if (header != null) {
            Processor processor = new Processor();
            try {
                processor.setProperty(Constants.RUNSYSTEMCAPABILITIES, header);
                String processedHeader = processor.getProperty(Constants.RUNSYSTEMCAPABILITIES);
                sysCapsExtraParams = new Parameters(processedHeader);
            }
            finally {
                processor.close();
            }
        } else {
            sysCapsExtraParams = null;
        }
    }

    private void loadRepositories() {
        // Get all of the repositories from the plugin registry
        List<Repository> allRepos = registry.getPlugins(Repository.class);

        // Reorder/filter if specified by the run model
        List<String> repoNames = runModel.getRunRepos();
        if (repoNames == null) {
            // No filter, use all
            repos.addAll(allRepos);
        } else {
            // Map the repository names...
            Map<String,Repository> repoNameMap = new HashMap<String,Repository>(allRepos.size());
            for (Repository repo : allRepos)
                repoNameMap.put(repo.toString(), repo);

            // Create the result list
            for (String repoName : repoNames) {
                Repository repo = repoNameMap.get(repoName);
                if (repo != null)
                    repos.add(repo);
            }
        }
    }

    private void loadEffectiveSet() {
        String effective = (String) runModel.genericGet(RUN_EFFECTIVE_INSTRUCTION);
        if (effective == null)
            effectiveSet = null;
        else {
            effectiveSet = new HashSet<String>();
            for (Entry<String,Attrs> entry : new Parameters(effective).entrySet())
                effectiveSet.add(entry.getKey());
        }
    }

    private void findFramework() {
        String header = runModel.getRunFw();
        if (header == null)
            return;

        // Get the identity and version of the requested JAR
        Parameters params = new Parameters(header);
        if (params.size() > 1)
            throw new IllegalArgumentException("Cannot specify more than one OSGi Framework.");
        Entry<String,Attrs> entry = params.entrySet().iterator().next();
        String identity = entry.getKey();

        String versionStr = entry.getValue().get("version");

        // Construct a filter & requirement to find matches
        Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, identity);
        if (versionStr != null)
            filter = new AndFilter().addChild(filter).addChild(new LiteralFilter(Filters.fromVersionRange(versionStr)));
        Requirement frameworkReq = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();

        // Iterate over repos looking for matches
        for (Repository repo : repos) {
            Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections.singletonList(frameworkReq));
            Collection<Capability> frameworkCaps = providers.get(frameworkReq);
            if (frameworkCaps != null) {
                for (Capability frameworkCap : frameworkCaps) {
                    if (findFrameworkContractCapability(frameworkCap.getResource()) != null) {
                        Version foundVersion = toVersion(frameworkCap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
                        if (foundVersion != null) {
                            if (frameworkResourceVersion == null || (foundVersion.compareTo(frameworkResourceVersion) > 0)) {
                                frameworkResource = frameworkCap.getResource();
                                frameworkResourceVersion = foundVersion;
                                frameworkResourceRepo = new FrameworkResourceRepository(frameworkResource, ee, sysPkgsExtra, sysCapsExtraParams);
                            }
                        }
                    }
                }
            }
        }
    }

    private void constructInputRequirements() {
        List<Requirement> requires = runModel.getRunRequires();
        if (requires == null || requires.isEmpty()) {
            inputRequirementsResource = null;
        } else {
            ResourceBuilder resBuilder = new ResourceBuilder();
            CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, IDENTITY_INITIAL_RESOURCE);
            resBuilder.addCapability(identity);

            for (Requirement req : requires) {
                resBuilder.addRequirement(req);
            }

            inputRequirementsResource = resBuilder.build();
        }
    }

    private void loadPreferences() {
        String prefsStr = (String) runModel.genericGet(PROP_RESOLVE_PREFERENCES);
        if (prefsStr == null)
            prefsStr = "";
        resolvePrefs = new Parameters(prefsStr);
    }

    public static boolean isInputRequirementResource(Resource resource) {
        Capability id = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
        return IDENTITY_INITIAL_RESOURCE.equals(id.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
    }

    private static Version toVersion(Object object) throws IllegalArgumentException {
        if (object == null)
            return null;

        if (object instanceof Version)
            return (Version) object;

        if (object instanceof String)
            return Version.parseVersion((String) object);

        throw new IllegalArgumentException(MessageFormat.format("Cannot convert type {0} to Version.", object.getClass().getName()));
    }

    private static Capability findFrameworkContractCapability(Resource resource) {
        List<Capability> contractCaps = resource.getCapabilities(ContractNamespace.CONTRACT_NAMESPACE);
        if (contractCaps != null)
            for (Capability cap : contractCaps) {
                if (CONTRACT_OSGI_FRAMEWORK.equals(cap.getAttributes().get(ContractNamespace.CONTRACT_NAMESPACE)))
                    return cap;
            }
        return null;
    }

    public void addRepository(Repository repo) {
        repos.add(repo);
    }

    @Override
    public Collection<Resource> getMandatoryResources() {
        init();
        if (frameworkResource == null)
            throw new IllegalStateException(MessageFormat.format("Could not find OSGi framework matching {0}.", runModel.getRunFw()));

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(frameworkResource);

        if (inputRequirementsResource != null)
            resources.add(inputRequirementsResource);
        return resources;
    }

    @Override
    public List<Capability> findProviders(Requirement requirement) {
        init();

        List<Capability> result;

        CacheKey cacheKey = getCacheKey(requirement);
        List<Capability> cached = providerCache.get(cacheKey);
        if (cached != null) {
            result = new ArrayList<Capability>(cached);
        } else {
            // First stage: framework and self-capabilities. This should never be reordered by preferences or resolver
            // hooks
            LinkedHashSet<Capability> firstStageResult = new LinkedHashSet<Capability>();

            // The selected OSGi framework always has the first chance to provide the capabilities
            if (frameworkResourceRepo != null) {
                Map<Requirement,Collection<Capability>> providers = frameworkResourceRepo.findProviders(Collections.singleton(requirement));
                Collection<Capability> capabilities = providers.get(requirement);
                if (capabilities != null && !capabilities.isEmpty()) {
                    firstStageResult.addAll(capabilities);
                }
            }

            // Next find out if the requirement is satisfied by a capability on the same resource
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

            // If the requirement is optional and doesn't come from an optional root resource,
            // then we are done already, no need to look for providers from the repos.
            boolean optional = Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
            if (optional && !optionalRoots.contains(requirement.getResource())) {

                result = new ArrayList<Capability>(firstStageResult);
                Collections.sort(result, capabilityComparator);

            } else {

                // Second stage results: repository contents; may be reordered.
            	ArrayList<Capability> secondStageResult = new ArrayList<Capability>();

                // Iterate over the repos
                int order = 0;
                ArrayList<Capability> repoCapabilities = new ArrayList<Capability>();
                for (Repository repo : repos) {
                    repoCapabilities.clear();
                    Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections.singleton(requirement));
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

                // Concatenate both stages, eliminating duplicates between the two
                firstStageResult.addAll(secondStageList);
                result = new ArrayList<Capability>(firstStageResult);
            }

            providerCache.put(cacheKey, result);
        }

        return result;
    }

    private boolean matches(Requirement requirement, Capability selfCap) {
        boolean match = false;
        try {
            String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
            org.osgi.framework.Filter filter = filterStr != null ? org.osgi.framework.FrameworkUtil.createFilter(filterStr) : null;

            if (filter == null)
                match = true;
            else
                match = filter.match(new MapToDictionaryAdapter(selfCap.getAttributes()));
        } catch (InvalidSyntaxException e) {
            log.log(LogService.LOG_ERROR, "Invalid filter directive on requirement: " + requirement, e);
        }
        return match;
    }

    private static CacheKey getCacheKey(Requirement requirement) {
        return new CacheKey(requirement.getNamespace(), requirement.getDirectives(), requirement.getAttributes());
    }

    protected void postProcessProviders(Requirement requirement, Set<Capability> wired, List<Capability> candidates) {
        if (candidates.size() == 0)
            return;

        // Call resolver hooks
        for (ResolverHook resolverHook : registry.getPlugins(ResolverHook.class)) {
            resolverHook.filterMatches(requirement, candidates);
        }

        // Process the resolve preferences
        boolean prefsUsed = false;
        if (resolvePrefs != null && !resolvePrefs.isEmpty()) {
            List<Capability> insertions = new LinkedList<Capability>();
            for (Iterator<Capability> iterator = candidates.iterator(); iterator.hasNext();) {
                Capability cap = iterator.next();
                if (resolvePrefs.containsKey(getResourceIdentity(cap.getResource()))) {
                    iterator.remove();
                    insertions.add(cap);
                }
            }

            if (!insertions.isEmpty()) {
                candidates.addAll(0, insertions);
                prefsUsed = true;
            }
        }

        // If preferences were applied, then don't need to call the callbacks
        if (!prefsUsed) {
            for (ResolutionCallback callback : callbacks) {
                callback.processCandidates(requirement, wired, candidates);
            }
        }
    }

    private static String getResourceIdentity(Resource resource) throws IllegalArgumentException {
        List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (identities == null || identities.size() != 1)
            throw new IllegalArgumentException("Resource element does not contain exactly one identity capability");

        Object idObj = identities.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
        if (idObj == null || !(idObj instanceof String))
            throw new IllegalArgumentException("Resource identity capability does not have a string identity attribute");

        return (String) idObj;
    }

    Resource getFrameworkResource() {
        return frameworkResource;
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

    private void setResourcePriority(int priority, Resource resource) {
        resourcePriorities.putIfAbsent(resource, priority);
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

    public boolean isInputRequirementsResource(Resource resource) {
        return resource == inputRequirementsResource;
    }

    public boolean isFrameworkResource(Resource resource) {
        return resource == frameworkResource;
    }

    private static class CacheKey {
        final String namespace;
        final Map<String,String> directives;
        final Map<String,Object> attributes;
        final int hashcode;

        CacheKey(String namespace, Map<String,String> directives, Map<String,Object> attributes) {
            this.namespace = namespace;
            this.directives = directives;
            this.attributes = attributes;
            this.hashcode = calculateHashCode(namespace, directives, attributes);
        }

        private static int calculateHashCode(String namespace, Map<String,String> directives, Map<String,Object> attributes) {
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

    private class CapabilityComparator implements Comparator<Capability> {

        public CapabilityComparator() {}

        public int compare(Capability o1, Capability o2) {

            Resource res1 = o1.getResource();
            Resource res2 = o2.getResource();

            // 1. Framework bundle
            if (res1 == getFrameworkResource())
                return -1;
            if (res2 == getFrameworkResource())
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
            } else if (IdentityNamespace.IDENTITY_NAMESPACE.equals(ns1) && IdentityNamespace.IDENTITY_NAMESPACE.equals(ns2)) {
                Version v1 = getVersion(o1, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                Version v2 = getVersion(o2, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                if (!v1.equals(v2))
                    return v2.compareTo(v1);
            }

            // 6. Same package version, higher bundle version
            if (PACKAGE_NAMESPACE.equals(ns1) && PACKAGE_NAMESPACE.equals(ns2)) {
                String bsn1 = (String) o1.getAttributes().get(Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE);
                String bsn2 = (String) o2.getAttributes().get(Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE);
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

}
