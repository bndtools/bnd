package biz.aQute.resolve.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.service.Registry;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.Filters;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.LiteralFilter;
import aQute.libg.filters.SimpleFilter;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;

public class BndrunResolveContext extends ResolveContext {

    private static final String CONTRACT_OSGI_FRAMEWORK = "OSGiFramework";
    private static final String IDENTITY_INITIAL_RESOURCE = "<<INITIAL>>";

    private final List<Repository> repos = new LinkedList<Repository>();
    private final Map<Requirement,List<Capability>> optionalRequirements = new HashMap<Requirement,List<Capability>>();

    private final BndEditModel runModel;
    private final Registry registry;

    private boolean initialised = false;

    private Resource frameworkResource = null;
    private Version frameworkResourceVersion = null;
    private Repository frameworkResourceRepo;

    private Resource inputRequirementsResource = null;
    private EE ee;

    public BndrunResolveContext(BndEditModel runModel, Registry registry) {
        this.runModel = runModel;
        this.registry = registry;
    }

    protected synchronized void init() {
        if (initialised)
            return;

        loadEE();
        loadRepositories();
        findFramework();
        constructInputRequirements();

        initialised = true;
    }

    private void loadEE() {
        EE tmp = runModel.getEE();
        ee = (tmp != null) ? tmp : EE.JavaSE_1_6;
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

    private void findFramework() {
        String header = runModel.getRunFramework();
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
                                frameworkResourceRepo = new FrameworkResourceRepository(frameworkResource, ee);
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

    public boolean isInputRequirementResource(Resource resource) {
        Capability id = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
        return IDENTITY_INITIAL_RESOURCE.equals(id.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
    }

    private Version toVersion(Object object) throws IllegalArgumentException {
        if (object == null)
            return null;

        if (object instanceof Version)
            return (Version) object;

        if (object instanceof String)
            return Version.parseVersion((String) object);

        throw new IllegalArgumentException(MessageFormat.format("Cannot convert type {0} to Version.", object.getClass().getName()));
    }

    private Capability findFrameworkContractCapability(Resource resource) {
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
            throw new IllegalStateException(MessageFormat.format("Could not find OSGi framework matching {0}.", runModel.getRunFramework()));

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(frameworkResource);

        if (inputRequirementsResource != null)
            resources.add(inputRequirementsResource);
        return resources;
    }

    @Override
    public List<Capability> findProviders(Requirement requirement) {
        init();
        ArrayList<Capability> result = new ArrayList<Capability>();

        // The selected OSGi framework always has the first chance to provide the capabilities
        if (frameworkResourceRepo != null) {
            Map<Requirement,Collection<Capability>> providers = frameworkResourceRepo.findProviders(Collections.singleton(requirement));
            Collection<Capability> capabilities = providers.get(requirement);
            if (capabilities != null && !capabilities.isEmpty()) {
                result.addAll(capabilities);
                // scoreResource
            }
        }

        // int score = 0;
        for (Repository repo : repos) {
            Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections.singleton(requirement));
            Collection<Capability> capabilities = providers.get(requirement);
            if (capabilities != null && !capabilities.isEmpty()) {
                result.ensureCapacity(result.size() + capabilities.size());
                for (Capability capability : capabilities) {
                    // filter out OSGi frameworks
                    if (findFrameworkContractCapability(capability.getResource()) == null)
                        result.add(capability);
                }
                // for (Capability capability : capabilities)
                // scoreResource(capability.getResource(), score);
            }
            // score--;
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
            if (fwkCaps.isEmpty())
                optionalRequirements.put(requirement, result);

            return fwkCaps;
        } else {
            return result;
        }
    }

    @Override
    public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean isEffective(Requirement requirement) {
        String effective = requirement.getDirectives().get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
        return effective == null || Namespace.EFFECTIVE_RESOLVE.equals(effective);
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

    public Map<Requirement,List<Capability>> getOptionalRequirements() {
        return optionalRequirements;
    }

}
