package biz.aQute.resolve;

import java.util.*;

import org.apache.felix.resolver.*;
import org.osgi.framework.*;
import org.osgi.resource.*;
import org.osgi.service.resolver.*;

import biz.aQute.resolve.internal.*;

public class BndResolver implements Resolver {

	private final ResolverImpl	resolver;

	public BndResolver(ResolverLogger logger) {
		resolver = new ResolverImpl(new InternalResolverLogger(logger));
	}

	public Map<Resource,List<Wire>> resolve(ResolveContext resolveContext) throws ResolutionException {

		Map<Resource,List<Wire>> result = resolver.resolve(resolveContext);

		List<Resource> resources = new ArrayList<Resource>();

		for (Resource r : resolveContext.getMandatoryResources()) {
			reqs: for (Requirement req : r.getRequirements(null)) {
				for (Resource found : result.keySet()) {
					String filterStr = req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
					try {
						org.osgi.framework.Filter filter = filterStr != null ? org.osgi.framework.FrameworkUtil
								.createFilter(filterStr) : null;

						for (Capability c : found.getCapabilities(req.getNamespace())) {
							if (filter != null && filter.matches(c.getAttributes())) {
								resources.add(found);
								continue reqs;
							}
						}
					}
					catch (InvalidSyntaxException e) {}
				}
			}
		}
		WrappedResolveContext wrappedResolveContext = new WrappedResolveContext(resources, resolveContext);
		return resolver.resolve(wrappedResolveContext);
	}

	private class WrappedResolveContext extends ResolveContext {

		final ResolveContext		resolveContext;
		final Collection<Resource>	resources;

		public WrappedResolveContext(Collection<Resource> resources, ResolveContext resolveContext) {
			this.resources = resources;
			this.resolveContext = resolveContext;
		}

		@Override
		public List<Capability> findProviders(Requirement requirement) {
			Resource resource = requirement.getResource();
			List<Capability> result = new ArrayList<Capability>();
			for (Resource found : resources) {
				String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
				try {
					org.osgi.framework.Filter filter = filterStr != null ? org.osgi.framework.FrameworkUtil
							.createFilter(filterStr) : null;

					List<Capability> caps = found.getCapabilities(requirement.getNamespace());
					for (Capability c : caps) {
						if (filter != null && filter.matches(c.getAttributes())) {
							result.add(c);
						}
					}
				}
				catch (InvalidSyntaxException e) {}
			}

			result.addAll(resolveContext.findProviders(requirement));
			return result;
		}

		@Override
		public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
			return resolveContext.insertHostedCapability(capabilities, hostedCapability);
		}

		@Override
		public boolean isEffective(Requirement requirement) {
			return true;
		}

		@Override
		public Map<Resource,Wiring> getWirings() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Resource> getMandatoryResources() {
			return resources;
		}
	}
}
