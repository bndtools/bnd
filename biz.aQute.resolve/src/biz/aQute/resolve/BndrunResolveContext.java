package biz.aQute.resolve;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.service.Registry;
import aQute.lib.osgi.resource.CapReqBuilder;
import aQute.libg.filters.SimpleFilter;

public class BndrunResolveContext extends ResolveContext {

    private final BndEditModel runModel;
    private final Registry registry;

    private final List<Repository> repos = new LinkedList<Repository>();
    private Resource frameworkResource = null;

    private boolean initialised = false;

    public BndrunResolveContext(BndEditModel runModel, Registry registry) {
        this.runModel = runModel;
        this.registry = registry;
    }

    protected synchronized void init() {
        if (initialised)
            return;

        loadRepositories();
        findFramework();

        initialised = true;
    }

    private void loadRepositories() {
        List<Repository> allRepos = registry.getPlugins(Repository.class);

        // Reorder or filter repos list if specified by the run model
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
        String frameworkName = runModel.getRunFramework();
        SimpleFilter frameworkFilter = new SimpleFilter("osgi.framework", frameworkName);
        Requirement frameworkReq = new CapReqBuilder("osgi.framework").addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, frameworkFilter.toString()).buildSyntheticRequirement();

        RepoLoop: for (Repository repo : repos) {
            Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections.singletonList(frameworkReq));
            Collection<Capability> frameworkCaps = providers.get(frameworkReq);
            if (frameworkCaps != null) {
                for (Capability frameworkCap : frameworkCaps) {
                    frameworkResource = frameworkCap.getResource();
                    break RepoLoop;
                }
            }
        }
    }

    public void addRepository(Repository repo) {
        repos.add(repo);
    }

    @Override
    public Collection<Resource> getMandatoryResources() {
        init();
        if (frameworkResource == null)
            throw new IllegalStateException(MessageFormat.format("Could not find OSGi framework matching {0}.", runModel.getRunFramework()));
        return Collections.singletonList(frameworkResource);
    }

    @Override
    public Collection<Resource> getOptionalResources() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<Capability> findProviders(Requirement requirement) {
        init();
        ArrayList<Capability> result = new ArrayList<Capability>();

        // int score = 0;
        for (Repository repo : repos) {
            Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections.singleton(requirement));
            Collection<Capability> capabilities = providers.get(requirement);
            if (capabilities != null) {
                result.addAll(capabilities);
                // for (Capability capability : capabilities)
                // scoreResource(capability.getResource(), score);
            }
            // score--;
        }

        return result;
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

}
