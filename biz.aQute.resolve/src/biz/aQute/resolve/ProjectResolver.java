package biz.aQute.resolve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.resolver.Logger;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.service.Strategy;

/**
 * This class provides resolving capabilities to a Project (and this a bndrun
 * which is a Run which extends Project). This class is supposed to simplify the
 * sometimes bewildering number of moving cogs in resolving. It is a processor
 * and uses the facilities to provide the different logging schemes used.
 */

@Deprecated
public class ProjectResolver extends Processor implements ResolutionCallback {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(ProjectResolver.class);

	private final class ReporterLogger extends Logger implements LogService {
		ReporterLogger(int i) {
			super(i);
		}

		@Override
		protected void doLog(int level, String msg, Throwable throwable) {
			switch (level) {
				case Logger.LOG_ERROR : // error
					if (throwable == null) {
						error("%s", msg);
					} else {
						exception(throwable, "%s", msg);
					}
					break;

				case Logger.LOG_WARNING :
					if (throwable == null) {
						warning("%s", msg);
					} else {
						warning("%s: %s", msg, throwable);
					}
					break;

				case Logger.LOG_INFO :
					logger.info("{}", msg, throwable);
					break;

				default :
					logger.debug("{}", msg, throwable);
					break;
			}
		}

		@Override
		public void log(ServiceReference sr, int level, String message) {
			doLog(level, toString(sr) + message, null);

		}

		@Override
		public void log(ServiceReference sr, int level, String message, Throwable exception) {
			doLog(level, toString(sr) + message, exception);
		}

		private String toString(ServiceReference<?> sr) {
			return "[" + sr.getProperty(org.osgi.framework.Constants.SERVICE_ID) + "] ";
		}
	}

	private final Project							project;
	private Map<Resource, List<Wire>>				resolution;
	private final ReporterLogger					log	= new ReporterLogger(0);
	private final Collection<ResolutionCallback>	cbs	= new ArrayList<>();

	public ProjectResolver(Project project) {
		super(project);
		getSettings(project);
		this.project = project;
	}

	public Map<Resource, List<Wire>> resolve() throws ResolutionException {
		try (ResolverLogger logger = new ResolverLogger()) {
			ResolveProcess resolve = new ResolveProcess();
			Resolver resolver = new BndResolver(logger);
			resolution = resolve.resolveRequired(this, project, this, resolver, cbs, log);
			return resolution;
		}
	}

	@Override
	public void processCandidates(Requirement requirement, Set<Capability> wired, List<Capability> candidates) {
		// System.out.println("Process candidates " + requirement + " " + wired
		// + " " + candidates);
	}

	/**
	 * Get the run bundles from the resolution. Resolve if this has not happened
	 * yet.
	 */

	public List<Container> getRunBundles() throws Exception {
		Map<Resource, List<Wire>> resolution = this.resolution;
		if (resolution == null) {
			resolution = resolve();
		}

		List<Container> containers = new ArrayList<>();
		for (Resource r : resolution.keySet()) {
			IdentityCapability identity = ResourceUtils.getIdentityCapability(r);
			if (identity == null) {
				error("Identity for %s not found", r);
				continue;
			}

			Container bundle = project.getBundle(identity.osgi_identity(), identity.version()
				.toString(), Strategy.EXACT, null);
			if (bundle == null) {
				error("Bundle for %s-%s not found", identity.osgi_identity(), identity.version());
				continue;
			}

			containers.add(bundle);
		}
		return containers;
	}

	/**
	 * Validate the current project for resolving.
	 *
	 * @throws Exception
	 */

	public void validate() throws Exception {
		BndrunResolveContext context = getContext();
		String runrequires = getProperty(RUNREQUIRES);
		if (runrequires == null || runrequires.isEmpty()) {
			error("Requires the %s instruction to be set", RUNREQUIRES);
		} else {
			if (EMPTY_HEADER.equals(runrequires))
				return;

			exists(context, runrequires, "Initial requirement %s cannot be resolved to an entry in the repositories");
		}
		String framework = getProperty(RUNFW);
		if (framework == null) {
			error("No framework is set");
		} else {
			exists(context, framework, "Framework not found");
		}
	}

	private void exists(BndrunResolveContext context, String framework, String msg) throws Exception {
		Parameters p = new Parameters(framework, this);
		for (Map.Entry<String, Attrs> e : p.entrySet()) {
			exists(context, e.getKey(), e.getValue(), msg);
		}
	}

	private void exists(BndrunResolveContext context, String namespace, Attrs attrs, String msg) throws Exception {
		Requirement req = CapReqBuilder.getRequirementFrom(namespace, attrs);
		List<Capability> caps = context.findProviders(req);
		if (caps == null || caps.isEmpty())
			error(msg, req);
	}

	public BndrunResolveContext getContext() {
		return new BndrunResolveContext(this, project, this, log);
	}

	public IdentityCapability getResource(String bsn, String version) {
		Requirement requirement = CapReqBuilder.createBundleRequirement(bsn, version)
			.buildSyntheticRequirement();
		List<Capability> result = getContext().findProviders(requirement);
		if (result == null || result.isEmpty())
			return null;

		return ResourceUtils.getIdentityCapability(result.get(0)
			.getResource());
	}

}
