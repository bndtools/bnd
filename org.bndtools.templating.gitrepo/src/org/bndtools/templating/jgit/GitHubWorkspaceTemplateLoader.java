package org.bndtools.templating.jgit;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.util.function.Function;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.lib.base64.Base64;
import aQute.service.reporter.Reporter;

@Component(name = "org.bndtools.core.templating.workspace.github", property = {
        Constants.SERVICE_DESCRIPTION + "=Load workspace templates from GitHub repositories"
})
public class GitHubWorkspaceTemplateLoader implements TemplateLoader {

    private static final String TEMPLATE_TYPE = "workspace";

    private final Cache cache = new Cache();

    private ExecutorService executor;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    @Activate
    void activate() {
        if (executor == null)
            executor = Executors.newCachedThreadPool();
    }

    @Override
    public Promise<List<Template>> findTemplates(String type, Reporter reporter) {
        if (!TEMPLATE_TYPE.equals(type))
            return Promises.resolved(Collections.<Template> emptyList());

        List<Promise<Template>> promises = new LinkedList<>();

        Parameters githubRepos = new GitRepoPreferences().getGithubRepos();
        for (Entry<String,Attrs> entry : githubRepos.entrySet()) {
            final String repo = GitRepoPreferences.removeDuplicateMarker(entry.getKey());
            final Attrs attribs = entry.getValue();

            try {
                final GitHub gitHub = new GitHub(cache, executor);
                promises.add(gitHub.loadRepoDetails(repo).map(new Function<GithubRepoDetailsDTO,Template>() {
                    @Override
                    public Template apply(GithubRepoDetailsDTO detailsDTO) {
                        if (detailsDTO.clone_url == null)
                            throw new IllegalArgumentException("Missing clone URL");

                        // Generate icon URI from the owner avatar. The s=16 parameter
                        // is added to select a 16x16 icon.
                        URI avatarUri = null;
                        if (detailsDTO.owner.avatar_url != null)
                            avatarUri = URI.create(detailsDTO.owner.avatar_url + "&s=16");

                        String name = attribs.get("name");
                        if (name == null)
                            name = repo;
                        String branch = attribs.get("branch");
                        final GitCloneTemplateParams params = new GitCloneTemplateParams();
                        params.cloneUrl = detailsDTO.clone_url;
                        if (branch != null)
                            params.branch = branch;
                        else
                            params.branch = "origin/" + detailsDTO.default_branch;
                        params.name = name;
                        params.category = "GitHub";
                        params.iconUri = avatarUri;

                        if (detailsDTO.html_url != null) {
                            params.helpUri = createHelpUri(repo, detailsDTO.html_url);
                        }

                        return new GitCloneTemplate(params);
                    }

                }));
            } catch (Exception e) {
                reporter.exception(e, "Error loading template from Github repository %s", repo);
            }
        }

        return Promises.all(promises);
    }

    private static URI createHelpUri(String repoName, String linkUri) {
        try {
            String formText = String.format("<form><p>This is a GitHub template using the repository %s. See the <a href='%s'>project homepage</a> for more information.</p></form>", repoName, new URI(linkUri));
            String encodedFormText = Base64.encodeBase64(formText.getBytes("UTF-8"));
            return new URI("data:text/xml;base64," + encodedFormText);
        } catch (Exception e) {
            return null;
        }
    }

}
