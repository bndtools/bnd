package org.bndtools.core.templating.repobased;

import static aQute.lib.promise.PromiseCollectors.toPromise;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateEngine;
import org.bndtools.templating.TemplateLoader;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.repository.Repository;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter;
import bndtools.central.Central;
import bndtools.preferences.BndPreferences;

@Component(name = "org.bndtools.templating.repos", property = {
	"source=workspace", Constants.SERVICE_DESCRIPTION + "=Load templates from the Workspace and Repositories",
	Constants.SERVICE_RANKING + "=" + ReposTemplateLoader.RANKING
})
public class ReposTemplateLoader implements TemplateLoader {

	static final int									RANKING			= Integer.MAX_VALUE;

	private static final String							NS_TEMPLATE		= "org.bndtools.template";

	private final ConcurrentMap<String, TemplateEngine>	engines			= new ConcurrentHashMap<>();

	// for testing
	Workspace											workspace		= null;

	private PromiseFactory								promiseFactory;
	private ExecutorService								localExecutor	= null;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	void setExecutorService(ExecutorService executor) {
		this.promiseFactory = new PromiseFactory(Objects.requireNonNull(executor));
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addTemplateEngine(TemplateEngine engine, Map<String, Object> svcProps) {
		String name = (String) svcProps.get("name");
		engines.put(name, engine);
	}

	void removeTemplateEngine(@SuppressWarnings("unused") TemplateEngine engine, Map<String, Object> svcProps) {
		String name = (String) svcProps.get("name");
		engines.remove(name);
	}

	@Activate
	void activate() {
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
	public Promise<List<Template>> findTemplates(String templateType, final Reporter reporter) {
		String filterStr = String.format("(%s=%s)", NS_TEMPLATE, templateType);
		final Requirement requirement = new CapReqBuilder(NS_TEMPLATE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filterStr)
			.buildSyntheticRequirement();

		// Try to get the repositories and BundleLocator from the workspace
		List<Repository> workspaceRepos;
		BundleLocator tmpLocator;
		try {
			if (workspace == null)
				workspace = Central.getWorkspace();
			workspaceRepos = workspace.getPlugins(Repository.class);
			tmpLocator = new RepoPluginsBundleLocator(workspace.getRepositories());
		} catch (Exception e) {
			workspaceRepos = Collections.emptyList();
			tmpLocator = new DirectDownloadBundleLocator();
		}
		final BundleLocator locator = tmpLocator;

		// Setup the repos
		List<Repository> repos = new ArrayList<>(workspaceRepos.size() + 1);
		repos.addAll(workspaceRepos);
		addPreferenceConfiguredRepos(repos, reporter);

		// Map List<Repository> to Promise<List<Template>>
		Promise<List<Template>> promise = repos.stream()
			.map(repo -> promiseFactory.submit(() -> {
				Map<Requirement, Collection<Capability>> providerMap = repo
					.findProviders(Collections.singleton(requirement));
				return providerMap.get(requirement)
					.stream()
					.map(cap -> {
						IdentityCapability idcap = ResourceUtils.getIdentityCapability(cap.getResource());
						Object id = idcap.getAttributes()
							.get(IdentityNamespace.IDENTITY_NAMESPACE);
						Object ver = idcap.getAttributes()
							.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
						try {
							String engineName = (String) cap.getAttributes()
								.get("engine");
							if (engineName == null)
								engineName = "stringtemplate";
							TemplateEngine engine = engines.get(engineName);
							if (engine != null)
								return new CapabilityBasedTemplate(cap, locator, engine);
							reporter.error(
								"Error loading template from resource '%s' version %s: no Template Engine available matching '%s'",
								id, ver, engineName);
						} catch (Exception e) {
							reporter.error("Error loading template from resource '%s' version %s: %s", id, ver,
								e.getMessage());
						}
						return null;
					})
					.filter(Objects::nonNull)
					.collect(toList());
			}))
			.collect(toPromise(promiseFactory))
			.map(ll -> ll.stream()
				.flatMap(List::stream)
				.collect(toList()));

		return promise;
	}

	private void addPreferenceConfiguredRepos(List<Repository> repos, Reporter reporter) {
		BndPreferences bndPrefs = null;
		try {
			bndPrefs = new BndPreferences();
		} catch (Exception e) {
			// e.printStackTrace();
		}

		if (bndPrefs != null && bndPrefs.getEnableTemplateRepo()) {
			List<String> repoUris = bndPrefs.getTemplateRepoUriList();
			try {
				OSGiRepository prefsRepo = loadRepo(repoUris, reporter);
				repos.add(prefsRepo);
			} catch (Exception ex) {
				reporter.exception(ex, "Error loading preference repository: %s", repoUris);
			}
		}
	}

	private OSGiRepository loadRepo(List<String> uris, Reporter reporter) throws Exception {
		OSGiRepository repo = new OSGiRepository();
		repo.setReporter(reporter);
		if (workspace != null) {
			repo.setRegistry(workspace);
		} else {
			Processor p = new Processor();
			p.addBasicPlugin(new HttpClient());
			repo.setRegistry(p);
		}
		Map<String, String> map = new HashMap<>();
		map.put("locations", Strings.join(uris));
		repo.setProperties(map);
		return repo;
	}
}
