package org.bndtools.core.templating.repobased;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Workspace;
import aQute.bnd.deployer.repository.FixedIndexedRepo;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.service.reporter.Reporter;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;

@Component(name = "org.bndtools.templating.repos", property = {
        "source=workspace", Constants.SERVICE_DESCRIPTION + "=Load templates from the Workspace and Repositories", Constants.SERVICE_RANKING + "=" + ReposTemplateLoader.RANKING
})
public class ReposTemplateLoader implements TemplateLoader {

    static final int RANKING = Integer.MAX_VALUE;

    private static final String NS_TEMPLATE = "org.bndtools.template";

    private List<Repository> repos;

    private BundleLocator locator;

    /**
     * For testing
     */
    void activate(Workspace workspace) {
        if (workspace != null) {
            this.repos = workspace.getPlugins(Repository.class);
            this.locator = new RepoPluginsBundleLocator(workspace.getRepositories());
        } else {
            this.repos = Collections.emptyList();
            this.locator = new BundleLocator() {
                @Override
                public File locate(String bsn, String hash, String algo) throws Exception {
                    return null;
                }
            };
        }
    }

    @Activate
    void activate() {
        Workspace workspace = null;
        try {
            workspace = Central.getWorkspaceIfPresent();
        } catch (IllegalStateException e) {}
        activate(workspace);
    }

    @Override
    public List<Template> findTemplates(String templateType, Reporter reporter) {
        String filterStr = String.format("(%s=%s)", NS_TEMPLATE, templateType);

        Requirement requirement = new CapReqBuilder(NS_TEMPLATE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filterStr).buildSyntheticRequirement();
        List<Template> templates = new ArrayList<>();

        List<Repository> repos = new ArrayList<>(this.repos.size() + 1);
        repos.addAll(this.repos);
        addPreferenceConfiguredRepos(repos, reporter);

        // Search for templates
        for (Repository repo : repos) {
            Map<Requirement,Collection<Capability>> providerMap = repo.findProviders(Collections.singleton(requirement));
            if (providerMap != null) {
                Collection<Capability> candidates = providerMap.get(requirement);
                if (candidates != null) {
                    for (Capability cap : candidates) {
                        try {
                            templates.add(new CapabilityBasedTemplate(cap, locator));
                        } catch (Exception e) {
                            IdentityCapability idcap = ResourceUtils.getIdentityCapability(cap.getResource());
                            Object id = idcap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
                            Object ver = idcap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                            reporter.error("Error loading template from resource '%s' version %s: %s", id, ver, e.getMessage());
                        }
                    }
                }
            }
        }
        return templates;
    }

    private static void addPreferenceConfiguredRepos(List<Repository> repos, Reporter reporter) {
        BndPreferences bndPrefs = null;
        try {
            bndPrefs = new BndPreferences();
        } catch (Exception e) {
            // e.printStackTrace();
        }

        if (bndPrefs != null && bndPrefs.getEnableTemplateRepo()) {
            List<String> repoUris = bndPrefs.getTemplateRepoUriList();
            try {
                FixedIndexedRepo prefsRepo = loadRepo(repoUris);
                repos.add(prefsRepo);
            } catch (IOException | URISyntaxException ex) {
                reporter.exception(ex, "Error loading preference repository: %s", repoUris);
            }
        }
    }

    private static FixedIndexedRepo loadRepo(List<String> uris) throws IOException, URISyntaxException {
        FixedIndexedRepo repo = new FixedIndexedRepo();
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> iter = uris.iterator(); iter.hasNext();) {
            sb.append(iter.next());
            if (iter.hasNext())
                sb.append(',');
        }
        repo.setLocations(sb.toString());
        return repo;
    }

}
