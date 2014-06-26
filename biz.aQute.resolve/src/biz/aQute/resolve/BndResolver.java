package biz.aQute.resolve;

import java.util.*;

import org.apache.felix.resolver.*;
import org.osgi.resource.*;
import org.osgi.service.resolver.*;

import biz.aQute.resolve.internal.*;

public class BndResolver implements Resolver {

	private final Resolver	resolver;

	public BndResolver(ResolverLogger logger) {
		resolver = new ResolverImpl(new InternalResolverLogger(logger));
	}

	public Map<Resource,List<Wire>> resolve(ResolveContext resolveContext) throws ResolutionException {
		return resolver.resolve(resolveContext);
	}
}
