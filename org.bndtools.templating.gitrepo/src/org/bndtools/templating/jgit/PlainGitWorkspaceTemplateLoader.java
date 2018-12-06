package org.bndtools.templating.jgit;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.service.reporter.Reporter;

@Component(name = "org.bndtools.core.templating.workspace.git", property = {
    Constants.SERVICE_DESCRIPTION + "=Load workspace templates from Git clone URLs"
})
public class PlainGitWorkspaceTemplateLoader implements TemplateLoader {

    private static final String TEMPLATE_TYPE = "workspace";

    private URL iconUrl;

    private PromiseFactory promiseFactory;
    private ExecutorService localExecutor = null;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    void setExecutorService(ExecutorService executor) {
        this.promiseFactory = new PromiseFactory(Objects.requireNonNull(executor));
    }

    @Activate
    void activate(BundleContext context) {
        iconUrl = context.getBundle()
            .getEntry("icons/git-16px.png");
        if (promiseFactory == null) {
            localExecutor = Executors.newCachedThreadPool();
            promiseFactory = new PromiseFactory(localExecutor);
        }
    }

    @Deactivate
    void dectivate() {
        if (localExecutor != null) {
            localExecutor.shutdown();
        }
    }

    @Override
    public Promise<List<Template>> findTemplates(String type, Reporter reporter) {
        if (!TEMPLATE_TYPE.equals(type)) {
            return promiseFactory.resolved(Collections.emptyList());
        }

        Parameters gitRepos = new GitRepoPreferences().getGitRepos();
        List<Template> templates = gitRepos.entrySet()
            .stream()
            .map(entry -> {
                String cloneUrl = GitRepoPreferences.removeDuplicateMarker(entry.getKey());
                Attrs attribs = entry.getValue();
                try {
                    GitCloneTemplateParams params = new GitCloneTemplateParams();
                    params.cloneUrl = cloneUrl;
                    params.category = "Git Repositories";
                    params.name = attribs.get("name");
                    params.iconUri = iconUrl.toURI();
                    params.branch = attribs.get("branch");

                    return (Template) new GitCloneTemplate(params);
                } catch (Exception e) {
                    reporter.exception(e, "Error loading template from Git clone URL %s", cloneUrl);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return promiseFactory.resolved(templates);
    }
}
