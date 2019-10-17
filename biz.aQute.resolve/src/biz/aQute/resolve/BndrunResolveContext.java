package biz.aQute.resolve;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.AggregateRepository;
import aQute.bnd.osgi.repository.AugmentRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.resolve.hook.ResolverHook;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;

/**
 * This class does the resolving for bundles. It loads the details from a
 * BndEditModel & Project
 */
public class BndrunResolveContext extends AbstractResolveContext {
	private final static Logger	logger						= LoggerFactory.getLogger(BndrunResolveContext.class);

	private static final String	BND_AUGMENT					= "bnd.augment";
	public static final String	RUN_EFFECTIVE_INSTRUCTION	= "-resolve.effective";
	public static final String	PROP_RESOLVE_PREFERENCES	= "-resolve.preferences";
	private static final String	NAMESPACE_WHITELIST			= "x-whitelist";

	private Registry			registry;
	private Parameters			resolvePrefs;
	private final Processor		properties;
	private Project				project;
	private boolean				initialized;
	private volatile List<ResolverHook>	resolverHooks;

	/**
	 * Constructor for a BndEditModel. The idea to use a BndEditModel was rather
	 * bad because it couples things that should not be coupled. The other
	 * constructor should be preferred.
	 *
	 * @param runModel The edit model
	 * @param registry The bnd registry
	 * @param log
	 */
	@Deprecated
	public BndrunResolveContext(BndEditModel runModel, Registry registry, LogService log) {
		super(log);
		try {
			this.registry = registry;
			this.properties = runModel.getProperties();
			this.project = runModel.getProject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The preferred constructor
	 *
	 * @param runModel The model (its properties)
	 * @param project The project to access bundles
	 * @param registry the registry
	 * @param log
	 */

	public BndrunResolveContext(Processor runModel, Project project, Registry registry, LogService log) {
		super(log);
		try {
			this.registry = registry;
			this.properties = runModel;
			this.project = project;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initializes the resolver. Here we will load all the information from the
	 * model.
	 */
	@Override
	public synchronized void init() {

		if (initialized)
			return;

		initialized = true;

		try {

			if (getLevel() <= 0) {
				Integer level = Converter.cnv(Integer.class, properties.getProperty("-resolvedebug", "0"));
				if (level != null)
					setLevel(level);
			}

			loadPreferences();

			Processor augments = loadRepositories();

			constructBlacklist(augments);

			Map<String, Set<String>> effectiveSet = loadEffectiveSet();
			if (effectiveSet != null)
				addEffectiveSet(effectiveSet);

			//
			// Create a resource from the -runrequire that contains
			// all the requirement
			//

			setInputResource(constructInputRequirements());

			//
			// We gradually build up the system resource that contains
			// the system packages, the EE, etc.
			//

			ResourceBuilder system = new ResourceBuilder();

			//
			// Let's identify the system resource to make it look less
			// ugly
			//

			//
			// If we have a distro, we do not load the environment
			// settings
			//

			String distro = properties.mergeProperties(Constants.DISTRO);
			if (distro != null && !distro.trim()
				.isEmpty()) {
				loadPath(system, distro, Constants.DISTRO);

				loadProvidedCapabilities(system);
			} else {
				//
				// Load the EE's and packages that belong to it.
				//

				EE tmp = EE.parse(properties.getProperty(Constants.RUNEE));
				EE ee = (tmp != null) ? tmp : EE.JavaSE_1_6;

				system.addAllExecutionEnvironments(ee);

				//
				// We make the system packages as coming from the system
				// resource
				//

				Parameters systemPackages = new Parameters(properties.mergeProperties(Constants.RUNSYSTEMPACKAGES),
					project);
				system.addExportPackages(systemPackages);

				//
				// We make the system capabilities as coming from the system
				// resource
				//

				Parameters systemCapabilities = new Parameters(
					properties.mergeProperties(Constants.RUNSYSTEMCAPABILITIES), project);
				system.addProvideCapabilities(systemCapabilities);

				loadProvidedCapabilities(system);

				//
				// Load the frameworks capabilities
				//

				loadFramework(system);

				//
				// Analyze the path and add all exported packages and provided
				// capabilities
				// to the system resource
				//

				String runpath = properties.mergeProperties(Constants.RUNPATH);

				if (runpath != null && !runpath.trim()
					.isEmpty())
					loadPath(system, runpath, Constants.RUNPATH);
			}

			//
			// We've not gathered all the capabilities of the system
			// so we can create the resource and set it as the system resource
			//

			setSystemResource(system.build());
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, e.getMessage(), e);
			throw new RuntimeException(e);
		}
		super.init();
	}

	private void loadProvidedCapabilities(ResourceBuilder system) throws Exception {
		//
		// Some capabilities are provided by the runtime, like native
		// code.
		// We need to add them here so the resolver is aware of them
		//

		Parameters providedCapabilities = new Parameters(properties.mergeProperties(Constants.RUNPROVIDEDCAPABILITIES),
			project);
		system.addProvideCapabilities(providedCapabilities);
	}

	void loadFramework(ResourceBuilder system) throws Exception {
		Parameters parameters = new Parameters(properties.getProperty(Constants.RUNFW), project);
		if (parameters.isEmpty()) {
			log.log(LogService.LOG_WARNING, "No -runfw set");
			return;
		}

		if (parameters.size() > 1)
			throw new IllegalArgumentException(
				"Too many frameworks specified in " + Constants.RUNFW + " (" + parameters + ")");

		Map.Entry<String, Attrs> bsn = parameters.entrySet()
			.iterator()
			.next();

		String name = bsn.getKey();
		String version = bsn.getValue()
			.getVersion();

		log.log(LogService.LOG_INFO, "Using frameowork " + name + ";" + version);

		if ("none".equals(name))
			return;

		Resource framework = getHighestResource(name, version);
		if (framework == null) {
			log.log(LogService.LOG_ERROR, "Cannot find framework " + name + ";" + version);
		} else
			super.setFramework(system, framework);

	}

	/**
	 * Add a path to the system resource. This is done by the bnd launcher for
	 * -runpath and it is also used for -distro.
	 */

	@Override
	public void loadPath(ResourceBuilder system, String path, String what) throws Exception {

		if (project != null) {
			List<Container> containers = Container.flatten(project.getBundles(Strategy.HIGHEST, path, what));

			for (Container c : containers) {
				HashSet<String> ignoredNamespaces = new HashSet<>(IGNORED_NAMESPACES_FOR_SYSTEM_RESOURCES);
				Strings.splitAsStream(c.getAttributes()
					.get(NAMESPACE_WHITELIST))
					.forEach(ignoredNamespaces::remove);

				Manifest manifest = c.getManifest();
				if (manifest != null) {
					ResourceBuilder rb = new ResourceBuilder();
					rb.addManifest(Domain.domain(manifest));
					system.copyCapabilities(ignoredNamespaces, rb.build());
				}

			}
		} else {
			super.loadPath(system, path, what);
		}
	}

	/**
	 * Load the effective set from the properties
	 */
	Map<String, Set<String>> loadEffectiveSet() {
		String effective = properties.getProperty(RUN_EFFECTIVE_INSTRUCTION);
		if (effective == null)
			return null;

		HashMap<String, Set<String>> effectiveSet = new HashMap<>();

		for (Entry<String, Attrs> entry : new Parameters(effective, project).entrySet()) {
			String skip = entry.getValue()
				.get("skip:");
			Set<String> toSkip = skip == null ? new HashSet<>() : new HashSet<>(Arrays.asList(skip.split(",")));
			effectiveSet.put(entry.getKey(), toSkip);
		}

		return effectiveSet;
	}

	/**
	 * Load all the OSGi repositories from our registry
	 * <p>
	 * TODO Use Instruction ...
	 *
	 * @return
	 * @throws Exception
	 */

	private Processor loadRepositories() throws Exception {
		//
		// Get all of the repositories from the plugin registry
		//

		ensureWorkspaceRepository();

		List<Repository> allRepos = registry.getPlugins(Repository.class);

		Collection<Repository> orderedRepositories;

		String rn = properties.mergeProperties(Constants.RUNREPOS);
		if (rn == null) {

			//
			// No filter set, so we use all
			//
			orderedRepositories = allRepos;

		} else {

			Parameters repoNames = new Parameters(rn, project);

			// Map the repository names...

			Map<String, Repository> repoNameMap = new HashMap<>(allRepos.size());
			for (Repository repo : allRepos) {
				String name;
				if (repo instanceof RepositoryPlugin) {
					name = ((RepositoryPlugin) repo).getName();
				} else {
					name = repo.toString();
				}
				repoNameMap.put(name, repo);
			}

			// Create the result list
			orderedRepositories = new ArrayList<>();
			for (String repoName : repoNames.keySet()) {
				Repository repo = repoNameMap.get(repoName);
				if (repo != null)
					orderedRepositories.add(repo);
			}
		}

		Processor repositoryAugments = findRepositoryAugments(orderedRepositories);

		Parameters augments = new Parameters(repositoryAugments.mergeProperties(Constants.AUGMENT), project);
		augments.putAll(new Parameters(properties.mergeProperties(Constants.AUGMENT), project));

		if (!augments.isEmpty()) {
			AggregateRepository aggregate = new AggregateRepository(orderedRepositories);
			AugmentRepository augment = new AugmentRepository(augments, aggregate);
			orderedRepositories = Collections.singleton(augment);
		}

		for (Repository repository : orderedRepositories) {
			super.addRepository(repository);
		}

		return repositoryAugments;
	}

	/*
	 * Ensure that the workspace has a repository that models its projects.
	 */
	private void ensureWorkspaceRepository() throws Exception {
		if (project != null) {
			if (!project.isStandalone() && project.getWorkspace()
				.getPlugins(WorkspaceRepositoryMarker.class)
				.isEmpty()) {

				assert !project.getWorkspace()
					.isInteractive() : "A static workspace repo cannot be used in an interactive environment";

				project.getWorkspace()
					.addBasicPlugin(new WorkspaceResourcesRepository(project.getWorkspace()));
			}
		}
	}

	private Processor findRepositoryAugments(Collection<Repository> orderedRepositories) {
		Processor main = new Processor();
		RequirementBuilder rb = new RequirementBuilder(BND_AUGMENT);
		rb.filter("(path=*)");
		Requirement req = rb.buildSyntheticRequirement();

		for (Repository r : orderedRepositories) {
			Map<Requirement, Collection<Capability>> found = r.findProviders(Collections.singleton(req));
			Collection<Capability> capabilities = found.get(req);
			if (capabilities != null) {
				for (Capability capability : capabilities) {
					findAdditionalAugmentsFromResource(main, capability);
				}
			}
		}
		return main;
	}

	private void findAdditionalAugmentsFromResource(Processor augments, Capability capability) {
		Resource resource = capability.getResource();
		Map<URI, String> locations = ResourceUtils.getLocations(resource);

		if (locations == null || locations.isEmpty())
			return;

		Object pathObject = capability.getAttributes()
			.get("path");
		if (pathObject == null)
			pathObject = "augments.bnd";

		if (pathObject instanceof String) {
			String path = (String) pathObject;

			HttpClient http = registry.getPlugin(HttpClient.class);

			for (URI uri : locations.keySet())
				try {
					logger.debug("loading augments from {}", uri);
					File file = http.build()
						.age(24, TimeUnit.HOURS)
						.useCache()
						.go(uri);
					try (Jar jar = new Jar(file)) {
						aQute.bnd.osgi.Resource rs = jar.getResource(path);
						try (InputStream in = rs.openInputStream()) {
							UTF8Properties p = new UTF8Properties();
							p.load(in, file, project, Constants.OSGI_SYNTAX_HEADERS);
							augments.getProperties()
								.putAll(p);
							return;
						}
					}
				} catch (Exception e) {
					project.warning("Failed to handle augment resource from repo %s", uri);
				}
		}
	}

	@Override
	public boolean isSystemResource(Resource resource) {
		Resource systemResource = getSystemResource();
		return resource == systemResource;
	}

	Resource constructInputRequirements() throws Exception {
		ResourceBuilder resBuilder = new ResourceBuilder();

		CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, IDENTITY_INITIAL_RESOURCE);
		resBuilder.addCapability(identity);

		Parameters inputRequirements = new Parameters(properties.mergeProperties(Constants.RUNREQUIRES), project);
		if (inputRequirements != null && !inputRequirements.isEmpty()) {
			List<Requirement> requires = CapReqBuilder.getRequirementsFrom(inputRequirements);
			resBuilder.addRequirements(requires);
		}

		return resBuilder.build();
	}

	private void constructBlacklist(Processor augments) throws Exception {
		Parameters blacklist = new Parameters(augments.mergeProperties(Constants.RUNBLACKLIST), project);
		blacklist.putAll(new Parameters(properties.mergeProperties(Constants.RUNBLACKLIST), project));

		if (blacklist != null && !blacklist.isEmpty()) {
			List<Requirement> reject = CapReqBuilder.getRequirementsFrom(blacklist);
			setBlackList(reject);
		}
	}

	private void loadPreferences() {
		resolvePrefs = new Parameters(properties.getProperty(PROP_RESOLVE_PREFERENCES), project);
	}

	private List<ResolverHook> getResolverHooks() {
		if (resolverHooks != null) {
			return resolverHooks;
		}
		return resolverHooks = registry.getPlugins(ResolverHook.class);
	}

	@Override
	protected void postProcessProviders(Requirement requirement, Set<Capability> wired, List<Capability> candidates) {
		if (candidates.isEmpty())
			return;

		// Call resolver hooks
		for (ResolverHook resolverHook : getResolverHooks()) {
			resolverHook.filterMatches(requirement, candidates);
		}

		// Process the resolve preferences
		boolean prefsUsed = false;

		if (resolvePrefs != null && !resolvePrefs.isEmpty()) {
			List<Capability> insertions = new LinkedList<>();
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
			for (ResolutionCallback callback : getCallbacks()) {
				callback.processCandidates(requirement, wired, candidates);
			}
		}
	}
}
