package biz.aQute.resolve.internal;

import static aQute.bnd.osgi.resource.CapReqBuilder.copy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Manifest;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.deployer.repository.CapabilityIndex;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.Filters;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.Registry;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.resolve.hook.ResolverHook;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.LiteralFilter;
import aQute.libg.filters.SimpleFilter;
import biz.aQute.resolve.GenericResolveContext;
import biz.aQute.resolve.ResolutionCallback;

/**
 * This class does the resolving for bundles.
 */
public class BndrunResolveContext extends GenericResolveContext {
	static Set<String>					ignoreNamespaces			= new HashSet<String>();
	static {
		ignoreNamespaces.add(BundleNamespace.BUNDLE_NAMESPACE);
		ignoreNamespaces.add(IdentityNamespace.IDENTITY_NAMESPACE);
		ignoreNamespaces.add(ContentNamespace.CONTENT_NAMESPACE);
		ignoreNamespaces.add(HostNamespace.HOST_NAMESPACE);
	}

	public static final String			RUN_EFFECTIVE_INSTRUCTION	= "-resolve.effective";
	public static final String			PROP_RESOLVE_PREFERENCES	= "-resolve.preferences";

	private Registry					registry;

	private EE							ee;

	private List<ExportedPackage>		sysPkgsExtra				= new ArrayList<ExportedPackage>();
	private Parameters					sysCapsExtraParams			= new Parameters();
	private Parameters					resolvePrefs;

	private Version						frameworkResourceVersion	= null;
	private FrameworkResourceRepository	frameworkResourceRepo;
	private final Processor				properties;
	private Project						project;

	public BndrunResolveContext(BndEditModel runModel, Registry registry, LogService log) {
		super(log);
		try {
			this.registry = registry;
			this.properties = runModel.getProperties();
			this.project = runModel.getProject();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public BndrunResolveContext(Processor runModel, Project project, Registry registry, LogService log) {
		super(log);
		try {
			this.registry = registry;
			this.properties = runModel;
			this.project = project;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected synchronized void init() {
		if (initialised)
			return;

		try {
			loadEE();
			loadSystemPackagesExtra();
			loadSystemCapabilitiesExtra();
			loadRepositories();
			constructBlacklist();
			loadEffectiveSet();
			findFramework();
			loadpaths(systemCapabilityIndex);
			constructInputRequirements();
			loadPreferences();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		super.init();
	}

	/**
	 * Load the capabilities and packages from the -testpath and -runpath since
	 * we make those available. Testpath is only added whent he Test-Cases
	 * header is set in the lowest properties file.
	 */
	private void loadpaths(CapabilityIndex capabilityIndex) throws Exception {
		loadpath(capabilityIndex, Constants.DISTRO, "-provided");
		loadpath(capabilityIndex, Constants.RUNPATH, "-runpath");
		if (properties.get(Constants.TESTCASES) != null && !properties.is(Constants.NOJUNITOSGI))
			loadpath(capabilityIndex, Constants.TESTPATH, "-testpath");
	}

	/**
	 * Read the bundles from a path (test/run) and copy their exported packages
	 * to the packages extra list and their capabilities to the capabilities
	 * extra list just like the launcher does.
	 */
	private void loadpath(CapabilityIndex capabilityIndex, String path, String source) throws Exception {

		//
		// First gather all the resources on the buildpath
		//
		Set<Resource> resources = new LinkedHashSet<Resource>();

		String totalpath = properties.mergeProperties(path);

		if (project != null) {
			List<Container> containers = Container.flatten(project.getBundles(Strategy.HIGHEST, totalpath, path));
			for (Container container : containers) {
				Manifest m = container.getManifest();
				if (m != null) {
					Domain domain = Domain.domain(m);
					Entry<String,Attrs> bsne = domain.getBundleSymbolicName();
					if (bsne != null && Verifier.isBsn(bsne.getKey())) {
						String version = domain.getBundleVersion();
						if (version != null && Verifier.isVersion(version)) {
							if (!getResources(resources, bsne.getKey(), version)) {
								// fallback for non-indexed things
								// need a more permanent solutions

							}
						}
					}
				}
			}
		} else {

			//
			// Fallback so we can test this without a full project setup
			//

			Parameters p = new Parameters(totalpath);
			for (Entry<String,Attrs> e : p.entrySet()) {
				String bsn = e.getKey();
				String version = e.getValue().getVersion();
				if (version == null)
					version = "0";

				if (Verifier.isBsn(bsn) && Verifier.isVersion(version)) {
					if (!getResources(resources, bsn, version)) {
						// fallback for non-indexed things
						// need a more permanent solutions

					}
				}
			}
		}
		//
		// Now for each resource, get all its capabilities and
		// add them to the capability index with the system as
		// owner
		//

		for (Resource r : resources) {
			for (Capability c : r.getCapabilities(null)) {
				if (ignoreNamespaces.contains(c.getNamespace()))
					continue;

				Capability copy = copy(c, systemResource);
				capabilityIndex.addCapability(copy);
			}
		}
	}

	/*
	 * Try to find a resource from the existing repositories by its bsn and
	 * version. This returns true if a single bundle was find, otherwise false
	 */
	private boolean getResources(Set<Resource> resources, String bsn, String version) {
		CapReqBuilder capReq = CapReqBuilder.createBundleRequirement(bsn, version);
		Requirement requirement = capReq.buildSyntheticRequirement();

		for (Repository repository : getRepositories()) {

			Map<Requirement,Collection<Capability>> bundles = findProviders(repository, requirement);
			if (bundles.isEmpty())
				continue;

			for (Collection<Capability> caps : bundles.values()) {
				for (Capability cap : caps)
					resources.add(cap.getResource());
			}
			return true;
		}
		return false;
	}

	private void loadEE() {
		EE tmp = EE.parse(properties.getProperty(Constants.RUNEE));
		ee = (tmp != null) ? tmp : EE.JavaSE_1_6;
	}

	private void loadEffectiveSet() {
		String effective = properties.getProperty(RUN_EFFECTIVE_INSTRUCTION);
		if (effective == null)
			effectiveSet = null;
		else {
			effectiveSet = new HashMap<String,Set<String>>();
			for (Entry<String,Attrs> entry : new Parameters(effective).entrySet()) {
				String skip = entry.getValue().get("skip:");
				Set<String> toSkip = skip == null ? new HashSet<String>() : new HashSet<String>(Arrays.asList(skip
						.split(",")));
				effectiveSet.put(entry.getKey(), toSkip);
			}
		}
	}

	private void loadSystemPackagesExtra() {
		Parameters p = new Parameters(properties.mergeProperties(Constants.RUNSYSTEMPACKAGES));

		sysPkgsExtra = toExportedPackages(p); // runModel.getSystemPackages();
	}

	private List<ExportedPackage> toExportedPackages(Parameters p) {
		List<ExportedPackage> list = new ArrayList<ExportedPackage>();
		for (Entry<String,Attrs> e : p.entrySet()) {
			list.add(new ExportedPackage(Processor.removeDuplicateMarker(e.getKey()), e.getValue()));
		}

		return list;
	}

	private void loadSystemCapabilitiesExtra() {
		String header = properties.mergeProperties(Constants.RUNSYSTEMCAPABILITIES);
		sysCapsExtraParams.putAll(new Parameters(header));
	}

	private void loadRepositories() throws IOException {
		// Get all of the repositories from the plugin registry
		List<Repository> allRepos = registry.getPlugins(Repository.class);

		// Workspace ws = registry.getPlugin(Workspace.class);
		// if (ws != null) {
		// for (InfoRepository ir : registry.getPlugins(InfoRepository.class)) {
		// allRepos.add(new InfoRepositoryWrapper(ir, ws.getCache("ir-" +
		// ir.getName())));
		// }
		// }

		// Reorder/filter if specified by the run model

		String rn = properties.mergeProperties(Constants.RUNREPOS);
		if (rn == null) {
			// No filter, use all
			for (Repository repo : allRepos) {
				super.addRepository(repo);
			}
		} else {
			Parameters repoNames = new Parameters(rn);

			// Map the repository names...
			Map<String,Repository> repoNameMap = new HashMap<String,Repository>(allRepos.size());
			for (Repository repo : allRepos)
				repoNameMap.put(repo.toString(), repo);

			// Create the result list
			for (String repoName : repoNames.keySet()) {
				Repository repo = repoNameMap.get(repoName);
				if (repo != null)
					super.addRepository(repo);
			}
		}
	}

	private void findFramework() {
		Requirement frameworkReq = getFrameworkRequirement();
		if (frameworkReq == null)
			return;

		// Iterate over repos looking for matches
		for (Repository repo : repositories) {
			Map<Requirement,Collection<Capability>> providers = findProviders(repo, frameworkReq);
			Collection<Capability> frameworkCaps = providers.get(frameworkReq);
			if (frameworkCaps != null) {
				for (Capability frameworkCap : frameworkCaps) {
					if (findFrameworkContractCapability(frameworkCap.getResource()) != null) {
						Version foundVersion = toVersion(frameworkCap.getAttributes().get(
								IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
						if (foundVersion != null) {
							if (frameworkResourceVersion == null
									|| (foundVersion.compareTo(frameworkResourceVersion) > 0)) {
								systemResource = frameworkCap.getResource();
								frameworkResourceVersion = foundVersion;
								frameworkResourceRepo = new FrameworkResourceRepository(systemResource, ee,
										sysPkgsExtra, sysCapsExtraParams);
								systemCapabilityIndex = frameworkResourceRepo.getCapabilityIndex();
							}
						}
					}
				}
			}
		}
	}

	private Requirement getFrameworkRequirement() {
		String header = properties.getProperty(Constants.RUNFW);
		if (header == null)
			return null;

		// Get the identity and version of the requested JAR
		Parameters params = new Parameters(header);
		if (params.size() > 1)
			throw new IllegalArgumentException("Cannot specify more than one OSGi Framework.");
		Entry<String,Attrs> entry = params.entrySet().iterator().next();
		String identity = entry.getKey();

		String versionStr = entry.getValue().getVersion();

		// Construct a filter & requirement to find matches
		Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, identity);
		if (versionStr != null)
			filter = new AndFilter().addChild(filter).addChild(new LiteralFilter(Filters.fromVersionRange(versionStr)));
		Requirement frameworkReq = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(
				Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
		return frameworkReq;
	}

	@Override
	public Resource getInputResource() {
		if (inputResource != null) {
			return inputResource;
		}
		constructInputRequirements();
		return inputResource;
	}

	@Override
	public Resource getSystemResource() {
		if (systemResource != null) {
			return systemResource;
		}
		findFramework();
		return systemResource;
	}

	@Override
	public boolean isSystemResource(Resource resource) {
		Resource systemResource = getSystemResource();
		return resource == systemResource;
	}

	private void constructInputRequirements() {
		Parameters inputRequirements = new Parameters(properties.mergeProperties(Constants.RUNREQUIRES));
		if (inputRequirements == null || inputRequirements.isEmpty()) {
			inputResource = null;
		} else {
			List<Requirement> requires = CapReqBuilder.getRequirementsFrom(inputRequirements);

			ResourceBuilder resBuilder = new ResourceBuilder();
			CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addAttribute(
					IdentityNamespace.IDENTITY_NAMESPACE, IDENTITY_INITIAL_RESOURCE);
			resBuilder.addCapability(identity);

			for (Requirement req : requires) {
				resBuilder.addRequirement(req);
			}

			inputResource = resBuilder.build();
		}
	}

	private void constructBlacklist() {
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
			for (ResolutionCallback callback : callbacks) {
				callback.processCandidates(requirement, wired, candidates);
			}
		}
	}
}
