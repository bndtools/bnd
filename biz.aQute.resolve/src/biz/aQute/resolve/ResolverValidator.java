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
import aQute.bnd.osgi.resource.ResolutionDirective;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.osgi.resource.ResourcesRepository;
import aQute.lib.strings.Strings;

public class ResolverValidator extends Processor {

	LogReporter			reporter		= new LogReporter(this);
	Resolver			resolver		= new ResolverImpl(reporter);
	List<URI>			repositories	= new ArrayList<>();
	Resource	system			= null;

	public ResolverValidator(Processor parent) throws Exception {
		super(parent);
	}

	public ResolverValidator() {
	}

	public void addRepository(URI url) throws Exception {
		repositories.add(url);
	}

	public void setSystem(Resource resource) throws Exception {
		assert resource != null;
		this.system = resource;
	}

	public boolean validate() throws Exception {

		FixedIndexedRepo repository = new FixedIndexedRepo();
		repository.setLocations(Strings.join(repositories));

		Set<Resource> resources = getAllResources(repository);
		setProperty("-runfw", "dummy");

		validateResources(repository, resources);
		return false;
	}

	public void validateResources(Repository repository, Set<Resource> resources) throws Exception {
		for (Resource resource : resources) {
			resolve(repository, resource);
		}
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

	public void resolve(Repository repository, Resource resource) throws Exception {
		Requirement identity = getIdentity(resource);
		setProperty("-runrequires", ResourceUtils.toRequireCapability(identity));

		BndrunResolveContext context = getResolveContext();

		context.addRepository(repository);
		context.init();

		try {
			Map<Resource,List<Wire>> resolve2 = resolver.resolve(context);
			trace("resolving %s succeeded", context.getInputResource().getRequirements(null));
		}
		catch (ResolutionException e) {
			error("!!!! %s :: %s", resource, e.getMessage());

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
							error("     optional but missing %s", r);
						else
							error("     missing %s", r);

					} else
						trace("     found %s in repo", r);
				} else
					trace("     found %s in system", r);
			}

		}
		catch (Exception e) {
			e.printStackTrace();
			error("resolving %s failed with %s", context.getInputResource().getRequirements(null), e);
		}
	}
}
