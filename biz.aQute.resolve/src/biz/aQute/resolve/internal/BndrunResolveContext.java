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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.ExportedPackage;
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

    private final List<Repository> repos = new LinkedList<Repository>();
    private final ConcurrentMap<Resource,Integer> resourcePriorities = new ConcurrentHashMap<Resource,Integer>();
    private final Map<Requirement,List<Capability>> mandatoryRequirements = new HashMap<Requirement,List<Capability>>();
    private final Map<Requirement,List<Capability>> optionalRequirements = new HashMap<Requirement,List<Capability>>();

    private final Map<CacheKey,List<Capability>> providerCache = new HashMap<CacheKey,List<Capability>>();

    private final Comparator<Capability> capabilityComparator = new CapabilityComparator();

    private final BndEditModel runModel;
    private final Registry registry;
    private final LogService log;

    private boolean initialised = false;

    private Resource frameworkResource = null;
    private Version frameworkResourceVersion = null;
    private FrameworkResourceRepository frameworkResourceRepo;

    private Resource inputRequirementsResource = null;
    private EE ee;
    private Set<String> effectiveSet;
    private List<ExportedPackage> sysPkgsExtra;
    private Parameters sysCapsExtraParams;

    public BndrunResolveContext(BndEditModel runModel, Registry registry, LogService log) {
        this.runModel = runModel;
        this.registry = registry;
        this.log = log;
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

        ArrayList<Capability> result;
        CacheKey cacheKey = getCacheKey(requirement);
        List<Capability> cached = providerCache.get(cacheKey);
        if (cached != null) {
            result = new ArrayList<Capability>(cached);
        } else {
            result = new ArrayList<Capability>();
            // The selected OSGi framework always has the first chance to provide the capabilities
            if (frameworkResourceRepo != null) {
                Map<Requirement,Collection<Capability>> providers = frameworkResourceRepo.findProviders(Collections.singleton(requirement));
                Collection<Capability> capabilities = providers.get(requirement);
                if (capabilities != null && !capabilities.isEmpty()) {
                    result.addAll(capabilities);
                    Collections.sort(result, capabilityComparator);
                    // scoreResource
                }
            }

            int order = 0;
            ArrayList<Capability> repoCapabilities = new ArrayList<Capability>();
            for (Repository repo : repos) {
                repoCapabilities.clear();
                Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections.singleton(requirement));
                Collection<Capability> capabilities = providers.get(requirement);
                if (capabilities != null && !capabilities.isEmpty()) {
                    repoCapabilities.ensureCapacity(capabilities.size());
                    for (Capability capability : capabilities) {
                        // filter out OSGi frameworks & other forbidden resource
                        if (isPermitted(capability.getResource())) {
                            repoCapabilities.add(capability);
                            setResourcePriority(order, capability.getResource());
                        }
                    }
                    if (!repoCapabilities.isEmpty()) {
                        Collections.sort(repoCapabilities, capabilityComparator);
                        result.ensureCapacity(result.size() + repoCapabilities.size());
                        result.addAll(repoCapabilities);
                    }
                }
                order++;
            }
        }

        if (Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
            // Only return the framework's capabilities when asked for optional resources.
            List<Capability> fwkCaps = new ArrayList<Capability>(result.size());
            for (Capability capability : result) {
                if (capability.getResource() == frameworkResource)
                    fwkCaps.add(capability);
            }

            // If the framework couldn't provide the requirement then save the list of potential providers
            // to the side, in order to work out the optional resources later.
            if (fwkCaps.isEmpty()) {
                if (cached == null) {
                    callResolverHooks(requirement, result);
                    providerCache.put(cacheKey, new ArrayList<Capability>(result));
                }
                optionalRequirements.put(requirement, result);
            }
            Collections.sort(fwkCaps, capabilityComparator);
            return fwkCaps;
        } else {
            if (cached == null) {
                callResolverHooks(requirement, result);
                providerCache.put(cacheKey, new ArrayList<Capability>(result));
            }
            // Record as a mandatory requirement
            mandatoryRequirements.put(requirement, result);

            return result;
        }
    }

    private static CacheKey getCacheKey(Requirement requirement) {
        return new CacheKey(requirement.getNamespace(), requirement.getDirectives().get("filter"), requirement.getAttributes());
    }

    private void callResolverHooks(Requirement requirement, Collection<Capability> candidates) {
        if (candidates.size() == 0) {
            return;
        }
        for (ResolverHook resolverHook : registry.getPlugins(ResolverHook.class)) {
            resolverHook.filterMatches(requirement, candidates);
        }
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

    public Map<Requirement,List<Capability>> getMandatoryRequirements() {
        return mandatoryRequirements;
    }

    public Map<Requirement,List<Capability>> getOptionalRequirements() {
        return optionalRequirements;
    }

    private static class CacheKey {
        final String namespace;
        final String filter;
        final Map<String,Object> attributes;
        final int hashcode;

        CacheKey(String namespace, String filter, Map<String,Object> attributes) {
            this.namespace = namespace;
            this.filter = filter;
            this.attributes = attributes;
            this.hashcode = calculateHashCode(namespace, filter, attributes);
        }

        private static int calculateHashCode(String namespace, String filter, Map<String,Object> attributes) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
            result = prime * result + ((filter == null) ? 0 : filter.hashCode());
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
            if (filter == null) {
                if (other.filter != null)
                    return false;
            } else if (!filter.equals(other.filter))
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

            // 7. The resource with most capabilities
            return res2.getCapabilities(null).size() - res1.getCapabilities(null).size();
        }
    }

}
