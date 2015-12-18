package biz.aQute.resolve;

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
import java.util.jar.Manifest;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.AggregateRepository;
import aQute.bnd.osgi.repository.AugmentRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.Registry;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.resolve.hook.ResolverHook;
import aQute.lib.converter.Converter;

/**
 * This class does the resolving for bundles. It loads the details from a
 * BndEditModel & Project
 */
public class BndrunResolveContext extends AbstractResolveContext {

	public static final String	RUN_EFFECTIVE_INSTRUCTION	= "-resolve.effective";
	public static final String	PROP_RESOLVE_PREFERENCES	= "-resolve.preferences";

	private Registry			registry;
	private Parameters			resolvePrefs;
	private final Processor		properties;
	private Project				project;
	private boolean				initialized;

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

			loadRepositories();

			constructBlacklist();

			Map<String,Set<String>> effectiveSet = loadEffectiveSet();
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
			if (distro != null && !distro.trim().isEmpty()) {
				loadPath(system, distro, Constants.DISTRO);

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

				Parameters systemPackages = new Parameters(properties.mergeProperties(Constants.RUNSYSTEMPACKAGES));
				system.addExportPackages(systemPackages);

				//
				// We make the system capabilities as coming from the system
				// resource
				//

				Parameters systemCapabilities = new Parameters(
						properties.mergeProperties(Constants.RUNSYSTEMCAPABILITIES));
				system.addProvideCapabilities(systemCapabilities);

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

				if (runpath != null && !runpath.trim().isEmpty())
					loadPath(system, runpath, Constants.RUNPATH);
			}

			//
			// We've not gathered all the capabilities of the system
			// so we can create the resource and set it as the system resource
			//

			//
			// TODO Add osgi.wiring.bundle + osgi.wiring.host
			// filed a bug about using the impl version for the system
			// capabilities
			//
			List<Capability> frameworkPackages = system.findCapabilities(PackageNamespace.PACKAGE_NAMESPACE,
					"(" + PackageNamespace.PACKAGE_NAMESPACE + "=org.osgi.framework)");
			if (!frameworkPackages.isEmpty()) {
				Capability c = frameworkPackages.get(0);
				Version version = (Version) c.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);

				CapReqBuilder crb = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
				crb.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, "system.bundle");

				crb.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
				system.addCapability(crb);
			}

			setSystemResource(system.build());
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, e.getMessage(), e);
			throw new RuntimeException(e);
		}
		super.init();
	}

	void loadFramework(ResourceBuilder system) throws Exception {
		Parameters parameters = new Parameters(properties.getProperty(Constants.RUNFW));
		if (parameters.isEmpty()) {
			log.log(LogService.LOG_WARNING, "No -runfw set");
			return;
		}

		if (parameters.size() > 1)
			throw new IllegalArgumentException(
					"Too many frameworks specified in " + Constants.RUNFW + " (" + parameters + ")");

		Map.Entry<String,Attrs> bsn = parameters.entrySet().iterator().next();

		String name = bsn.getKey();
		String version = bsn.getValue().getVersion();

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

	public void loadPath(ResourceBuilder system, String path, String what) throws Exception {

		if (project != null) {
			List<Container> containers = Container.flatten(project.getBundles(Strategy.HIGHEST, path, what));

			for (Container c : containers) {

				Manifest manifest = c.getManifest();
				if (manifest != null) {
					ResourceBuilder rb = new ResourceBuilder();
					rb.addManifest(Domain.domain(manifest));
					addSystemResource(system, rb.build());
				}

			}
		} else {
			super.loadPath(system, path, what);
		}
	}

	/**
	 * Load the effective set from the properties
	 * 
	 * @return
	 */
	Map<String,Set<String>> loadEffectiveSet() {
		String effective = properties.getProperty(RUN_EFFECTIVE_INSTRUCTION);
		if (effective == null)
			return null;

		HashMap<String,Set<String>> effectiveSet = new HashMap<String,Set<String>>();

		for (Entry<String,Attrs> entry : new Parameters(effective).entrySet()) {
			String skip = entry.getValue().get("skip:");
			Set<String> toSkip = skip == null ? new HashSet<String>()
					: new HashSet<String>(Arrays.asList(skip.split(",")));
			effectiveSet.put(entry.getKey(), toSkip);
		}

		return effectiveSet;
	}

	/**
	 * Load all the OSGi repositories from our registry
	 * <p>
	 * TODO Use Instruction ...
	 * 
	 * @throws Exception
	 */

	private void loadRepositories() throws Exception {
		//
		// Get all of the repositories from the plugin registry
		//

		List<Repository> allRepos = registry.getPlugins(Repository.class);
		Collection<Repository> orderedRepositories;

		String rn = properties.mergeProperties(Constants.RUNREPOS);
		if (rn == null) {

			//
			// No filter set, so we use all
			//
			orderedRepositories = allRepos;

		} else {

			Parameters repoNames = new Parameters(rn);

			// Map the repository names...

			Map<String,Repository> repoNameMap = new HashMap<String,Repository>(allRepos.size());
			for (Repository repo : allRepos)
				repoNameMap.put(repo.toString(), repo);

			// Create the result list
			orderedRepositories = new ArrayList<>();
			for (String repoName : repoNames.keySet()) {
				Repository repo = repoNameMap.get(repoName);
				if (repo != null)
					orderedRepositories.add(repo);
			}
		}

		Parameters augments = new Parameters(properties.mergeProperties(Constants.AUGMENT));
		if (!augments.isEmpty()) {
			AggregateRepository aggregate = new AggregateRepository(orderedRepositories);
			AugmentRepository augment = new AugmentRepository(augments, aggregate);
			orderedRepositories = Collections.singleton((Repository) augment);
		}

		for (Repository repository : orderedRepositories) {
			super.addRepository(repository);
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

		Parameters inputRequirements = new Parameters(properties.mergeProperties(Constants.RUNREQUIRES));
		if (inputRequirements != null && !inputRequirements.isEmpty()) {
			List<Requirement> requires = CapReqBuilder.getRequirementsFrom(inputRequirements);
			resBuilder.addRequirements(requires);
		}

		return resBuilder.build();
	}

	private void constructBlacklist() throws Exception {
		Parameters blacklist = new Parameters(properties.mergeProperties(Constants.RUNBLACKLIST));
		if (blacklist != null && !blacklist.isEmpty()) {
			List<Requirement> reject = CapReqBuilder.getRequirementsFrom(blacklist);
			setBlackList(reject);
		}
	}

	private void loadPreferences() {
		resolvePrefs = new Parameters(properties.getProperty(PROP_RESOLVE_PREFERENCES));
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
			for (ResolutionCallback callback : getCallbacks()) {
				callback.processCandidates(requirement, wired, candidates);
			}
		}
	}
}
