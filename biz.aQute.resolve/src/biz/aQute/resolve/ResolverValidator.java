package biz.aQute.resolve;

import static aQute.bnd.osgi.resource.CapReqBuilder.createRequirementFromCapability;
import static aQute.bnd.osgi.resource.ResourceUtils.createWildcardRequirement;
import static aQute.bnd.osgi.resource.ResourceUtils.getIdentityCapability;
import static java.util.Collections.singleton;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.resolver.ResolverImpl;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.deployer.repository.FixedIndexedRepo;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.ResolutionDirective;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.lib.strings.Strings;

public class ResolverValidator extends Processor {

	LogReporter	reporter		= new LogReporter(this);
	Resolver	resolver		= new ResolverImpl(reporter);
	List<URI>	repositories	= new ArrayList<>();
	Resource	system			= null;

	public static class Resolution {
		public Resource				resource;
		public boolean				succeeded;
		public String				message;
		public List<Requirement>	system		= new ArrayList<>();
		public List<Requirement>	repos		= new ArrayList<>();
		public List<Requirement>	missing		= new ArrayList<>();
		public List<Requirement>	optionals	= new ArrayList<>();
		public List<Requirement>	unresolved	= new ArrayList<>();
	}

	public ResolverValidator(Processor parent) throws Exception {
		super(parent);
	}

	public ResolverValidator() {}

	public void addRepository(URI url) throws Exception {
		repositories.add(url);
	}

	public void setSystem(Resource resource) throws Exception {
		assert resource != null;
		this.system = resource;
	}

	public List<Resolution> validate() throws Exception {

		FixedIndexedRepo repository = new FixedIndexedRepo();
		repository.setLocations(Strings.join(repositories));

		Set<Resource> resources = getAllResources(repository);
		setProperty("-runfw", "dummy");

		return validateResources(repository, resources);
	}

	public List<Resolution> validateResources(Repository repository, Set<Resource> resources) throws Exception {
		List<Resolution> result = new ArrayList<>();
		for (Resource resource : resources) {
			Resolution resolution = resolve(repository, resource);
			result.add(resolution);
		}
		return result;
	}

	public static Set<Resource> getAllResources(Repository repository) {
		Requirement r = createWildcardRequirement();

		Map<Requirement,Collection<Capability>> providers = repository.findProviders(Collections.singleton(r));
		Set<Resource> resources = ResourceUtils.getResources(providers.get(r));
		return resources;
	}

	private BndrunResolveContext getResolveContext() throws Exception {
		BndrunResolveContext context = new BndrunResolveContext(this, null, this, reporter) {
			@Override
			void loadFramework(ResourceBuilder systemBuilder) throws Exception {
				systemBuilder.addCapabilities(system.getCapabilities(null));
			}
		};
		return context;
	}

	public Requirement getIdentity(Resource resource) {
		IdentityCapability identityCapability = getIdentityCapability(resource);
		return createRequirementFromCapability(identityCapability).buildSyntheticRequirement();
	}

	public Resolution resolve(Repository repository, Resource resource) throws Exception {
		Resolution resolution = new Resolution();

		Requirement identity = getIdentity(resource);
		setProperty("-runrequires", ResourceUtils.toRequireCapability(identity));

		BndrunResolveContext context = getResolveContext();

		context.addRepository(repository);
		context.init();

		resolution.resource = resource;

		try {
			Map<Resource,List<Wire>> resolve2 = resolver.resolve(context);
			resolution.succeeded = true;

			trace("resolving %s succeeded", resource);
		} catch (ResolutionException e) {
			trace("resolving %s failed", resource);

			resolution.succeeded = false;
			resolution.message = e.getMessage();

			for (Requirement req : e.getUnresolvedRequirements()) {
				trace("    missing %s", req);
				resolution.unresolved.add(req);
			}

			ResourcesRepository systemRepository = new ResourcesRepository(system);

			for (Requirement r : resource.getRequirements(null)) {

				Collection<Capability> caps = systemRepository.findProvider(r);

				boolean missing = caps.isEmpty();

				if (missing) {

					Set<Requirement> requirements = singleton(r);
					caps = repository.findProviders(requirements).get(r);
					missing = caps.isEmpty();

					if (missing) {
						if (ResourceUtils.getResolution(r) == ResolutionDirective.optional)
							resolution.optionals.add(r);
						else
							resolution.missing.add(r);

					} else {
						trace("     found %s in repo", r);
						resolution.repos.add(r);
					}
				} else {
					trace("     found %s in system", r);
					resolution.system.add(r);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			error("resolving %s failed with %s", context.getInputResource().getRequirements(null), e);
			resolution.message = e.getMessage();
		}

		return resolution;
	}
}
