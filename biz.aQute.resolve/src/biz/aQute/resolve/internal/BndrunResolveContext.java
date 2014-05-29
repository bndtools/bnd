package biz.aQute.resolve.internal;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.resource.*;
import org.osgi.resource.Resource;
import org.osgi.service.log.*;
import org.osgi.service.repository.*;

import aQute.bnd.build.model.*;
import aQute.bnd.build.model.clauses.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.*;
import aQute.bnd.service.resolve.hook.*;
import aQute.libg.filters.*;
import aQute.libg.filters.Filter;
import biz.aQute.resolve.*;

public class BndrunResolveContext extends GenericResolveContext {

	public static final String			RUN_EFFECTIVE_INSTRUCTION	= "-resolve.effective";
	public static final String			PROP_RESOLVE_PREFERENCES	= "-resolve.preferences";

	private Registry					registry;

	private EE							ee;

	private List<ExportedPackage>		sysPkgsExtra;
	private Parameters					sysCapsExtraParams;
	private Parameters					resolvePrefs;

	private Version						frameworkResourceVersion	= null;
	private FrameworkResourceRepository	frameworkResourceRepo;
	private final Processor				properties;

	public BndrunResolveContext(BndEditModel runModel, Registry registry, LogService log) {
		super(log);
		try {
			this.registry = registry;
			this.properties = runModel.getProperties();
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
			loadEffectiveSet();
			findFramework();
			constructInputRequirements();
			loadPreferences();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		super.init();
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
			effectiveSet = new HashSet<String>();
			for (Entry<String,Attrs> entry : new Parameters(effective).entrySet())
				effectiveSet.add(entry.getKey());
		}
	}

	private void loadSystemPackagesExtra() {
		Parameters p = new Parameters( properties.mergeProperties(Constants.RUNSYSTEMPACKAGES));
		
		sysPkgsExtra = toExportedPackages(p); // runModel.getSystemPackages();
	}

	private List<ExportedPackage> toExportedPackages(Parameters p) {
		List<ExportedPackage> list = new ArrayList<ExportedPackage>();
		for ( Entry<String,Attrs> e : p.entrySet()) {
			list.add( new ExportedPackage(Processor.removeDuplicateMarker(e.getKey()), e.getValue()));
		}
		
		return list;
	}

	private void loadSystemCapabilitiesExtra() {
		String header = properties.mergeProperties(Constants.RUNSYSTEMCAPABILITIES);
		sysCapsExtraParams = header == null ? null : new Parameters(header);
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
			Map<Requirement,Collection<Capability>> providers = repo.findProviders(Collections
					.singletonList(frameworkReq));
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

	private static String getResourceIdentity(Resource resource) throws IllegalArgumentException {
		List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identities == null || identities.size() != 1)
			throw new IllegalArgumentException("Resource element does not contain exactly one identity capability");

		Object idObj = identities.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
		if (idObj == null || !(idObj instanceof String))
			throw new IllegalArgumentException("Resource identity capability does not have a string identity attribute");

		return (String) idObj;
	}
}
