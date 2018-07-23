package biz.aQute.resolve;

import java.util.List;
import java.util.Map;

import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.osgi.Processor;

public class BndResolver implements Resolver {

	private final Resolver resolver;

	public BndResolver(ResolverLogger logger) {
		this(new InternalResolverLogger(logger));
	}

	public BndResolver(Logger logger) {
		resolver = new ResolverImpl(logger, Processor.getExecutor());
	}

	@Override
	public Map<Resource, List<Wire>> resolve(ResolveContext resolveContext) throws ResolutionException {
		return resolver.resolve(resolveContext);
	}

	@Override
	public Map<Resource, List<Wire>> resolveDynamic(ResolveContext context, Wiring hostWiring,
		Requirement dynamicRequirement) throws ResolutionException {
		return resolver.resolveDynamic(context, hostWiring, dynamicRequirement);
	}
}
