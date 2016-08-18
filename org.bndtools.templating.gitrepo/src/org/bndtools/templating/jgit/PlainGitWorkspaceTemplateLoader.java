package org.bndtools.templating.jgit;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.service.reporter.Reporter;

@Component(name = "org.bndtools.core.templating.workspace.git", property = {
        Constants.SERVICE_DESCRIPTION + "=Load workspace templates from Git clone URLs"
})
public class PlainGitWorkspaceTemplateLoader implements TemplateLoader {

    private static final String TEMPLATE_TYPE = "workspace";

    private URL iconUrl;

    @Activate
    void activate(BundleContext context) {
        iconUrl = context.getBundle().getEntry("icons/git-16px.png");
    }

    @Override
    public Promise<List<Template>> findTemplates(String type, Reporter reporter) {
        if (!TEMPLATE_TYPE.equals(type))
            return Promises.resolved(Collections.<Template> emptyList());

        List<Template> templates = new LinkedList<>();

        Parameters gitRepos = new GitRepoPreferences().getGitRepos();
        for (Entry<String,Attrs> entry : gitRepos.entrySet()) {
            String cloneUrl = GitRepoPreferences.removeDuplicateMarker(entry.getKey());
            Attrs attribs = entry.getValue();

            try {
                GitCloneTemplateParams params = new GitCloneTemplateParams();
                params.cloneUrl = cloneUrl;
                params.category = "Git Repositories";
                params.name = attribs.get("name");
                params.iconUri = iconUrl.toURI();
                params.branch = attribs.get("branch");

                GitCloneTemplate template = new GitCloneTemplate(params);
                templates.add(template);
            } catch (Exception e) {
                reporter.exception(e, "Error loading template from Git clone URL %s", cloneUrl);
            }
        }
        return Promises.resolved(templates);
    }

}
