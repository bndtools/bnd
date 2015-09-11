package biz.aQute.resolve;

import java.util.List;
import java.util.Map;

import org.apache.felix.resolver.ResolverImpl;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

public class BndResolver implements Resolver {

	private final Resolver resolver;

	public BndResolver(ResolverLogger logger) {
		resolver = new ResolverImpl(new InternalResolverLogger(logger));
	}

	public Map<Resource,List<Wire>> resolve(ResolveContext resolveContext) throws ResolutionException {
		return resolver.resolve(resolveContext);
	}
}
