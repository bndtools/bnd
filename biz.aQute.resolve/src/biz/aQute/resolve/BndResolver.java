package biz.aQute.resolve;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

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

/**
 * A {@link Resolver} that delegates to a discovered implementation.
 * <p>
 * Implementations are looked up with {@link ServiceLoader} (see
 * osgi/org.osgi.service.resolver#43). If the system property
 * {@value #RESOLVER_PROPERTY} is set, the provider with that class name is
 * used, otherwise the first provider found. If there is none, the Felix
 * resolver bundled with bnd is used, so the behavior without any additional
 * resolver on the class path is unchanged.
 * <p>
 * Known limitations when a discovered (non Felix) resolver is used:
 * <ul>
 * <li>A discovered resolver is created through its no-arg constructor, so the
 * {@link ResolverLogger} passed to bnd is not available to it until
 * {@code ResolveContext.getLogger()} is available
 * (osgi/org.osgi.service.resolver#44).</li>
 * <li>The bnd specific executor is not handed to the resolver until
 * {@code ResolveContext.getExecutor()} is available
 * (osgi/org.osgi.service.resolver#51).</li>
 * <li>Detailed failure reports ({@link ResolveProcess}) rely on Felix
 * {@code ReasonException}s and are reduced to the plain
 * {@link ResolutionException} message until semantic exceptions are specified
 * (osgi/org.osgi.service.resolver#49).</li>
 * </ul>
 */
public class BndResolver implements Resolver {

	/**
	 * Class name of the {@link Resolver} provider to use.
	 */
	public static final String	RESOLVER_PROPERTY	= "bnd.resolver";

	private final Resolver		resolver;

	public BndResolver(ResolverLogger logger) {
		this(new InternalResolverLogger(logger));
	}

	public BndResolver(Logger logger) {
		resolver = discover().orElseGet(() -> new ResolverImpl(logger, Processor.getExecutor()));
	}

	private static Optional<Resolver> discover() {
		String wanted = System.getProperty(RESOLVER_PROPERTY);
		return ServiceLoader.load(Resolver.class, BndResolver.class.getClassLoader())
			.stream()
			.filter(p -> p.type() != BndResolver.class)
			.filter(p -> wanted == null || p.type()
				.getName()
				.equals(wanted))
			.findFirst()
			.map(Provider::get);
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
