package aQute.bnd.main;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.resolver.ResolverImpl;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.deployer.repository.FixedIndexedRepo;
import aQute.bnd.main.bnd.projectOptions;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.resolve.BndrunResolveContext;
import biz.aQute.resolve.LogReporter;
import biz.aQute.resolve.ProjectResolver;

public class ResolveCommand extends Processor {

	private bnd bnd;

	public ResolveCommand(bnd bnd) {
		super(bnd);
		this.bnd = bnd;
		getSettings(bnd);
	}

	interface ResolveOptions extends projectOptions {
		String workspace();
	}

	public void _find(ResolveOptions options, bnd bnd) throws Exception {

		List<String> args = options._arguments();

		for (String bndrun : args) {
			Project p = bnd.getProject(options.project());
			Workspace workspace = p == null ? bnd.getWorkspace(options.workspace()) : p.getWorkspace();

			Run run = new Run(workspace, p != null ? p.getBase() : IO.work, IO.getFile(bndrun));

			ProjectResolver pr = new ProjectResolver(run);
			addClose(pr);

			pr.resolve();

			bnd.out.println("Resolved " + run);
			for (Container c : pr.getRunBundles()) {
				bnd.out.printf("%-30s %-20s %-6s %s\n", c.getBundleSymbolicName(), c.getVersion(), c.getType(),
						c.getFile());
			}

		}

	}

	interface QueryOptions extends projectOptions {
		String workspace();
	}

	public void _query(QueryOptions options) throws Exception {
		List<String> args = options._arguments();
		String bsn = args.remove(0);
		String version = null;
		if (!args.isEmpty())
			version = args.remove(0);

		ProjectResolver pr = new ProjectResolver(bnd.getProject(options.project()));
		addClose(pr);

		IdentityCapability resource = pr.getResource(bsn, version);

		bnd.out.printf("%-30s %-20s %s\n", resource.osgi_identity(), resource.version(), resource.description(""));
		Resource r = resource.getResource();
		FilterParser p = new FilterParser();

		if (r != null) {
			List<Requirement> requirements = resource.getResource().getRequirements(null);
			if (requirements != null && requirements.size() > 0) {
				bnd.out.println("Requirements:");
				for (Requirement req : requirements) {
					Expression parse = p.parse(req);
					bnd.out.printf("  %-20s %s\n", req.getNamespace(), parse);
				}
			}
			List<Capability> capabilities = resource.getResource().getCapabilities(null);
			if (capabilities != null && capabilities.size() > 0) {

				bnd.out.println("Capabilities:");
				for (Capability cap : capabilities) {
					Map<String,Object> attrs = new HashMap<String,Object>(cap.getAttributes());
					Object id = attrs.remove(cap.getNamespace());
					Object vv = attrs.remove("version");
					if (vv == null)
						vv = attrs.remove("bundle-version");
					bnd.out.printf("  %-20s %-40s %-20s attrs=%s dirs=%s\n", cap.getNamespace(), id, vv, attrs,
							cap.getDirectives());
				}
			}
		}
	}

	interface RepoOptions extends Options {
		String workspace();
	}

	public void _repos(RepoOptions options) throws Exception {
		Workspace ws = bnd.getWorkspace(options.workspace());
		if (ws == null) {
			error("No workspace");
			return;
		}

		List<Repository> plugins = ws.getPlugins(Repository.class);
		bnd.out.println(Strings.join("\n", plugins));
	}

	/**
	 * Validate a repository so that it is self consistent
	 */

	@Arguments(arg = {
			"index-path"
	})
	interface ValidateOptions extends Options {

	}

	public void _validate(ValidateOptions options) throws Exception {
		LogReporter reporter = new LogReporter(this);
		List<String> args = options._arguments();

		File index = getFile(args.remove(0));
		FixedIndexedRepo fir = new FixedIndexedRepo();
		fir.setLocations(index.toURI().toString());

		Resolver resolver = new ResolverImpl(reporter);
		Requirement r = CapReqBuilder.createSimpleRequirement(IdentityNamespace.IDENTITY_NAMESPACE, "*", null)
				.buildSyntheticRequirement();

		setProperty("-runfw", "org.eclipse.osgi");

		Map<Requirement,Collection<Capability>> providers = fir.findProviders(Collections.singleton(r));
		Set<Resource> resources = ResourceUtils.getResources(providers.get(r));
		for (Resource resource : resources) {
			IdentityCapability identityCapability = ResourceUtils.getIdentityCapability(resource);
			Requirement req = CapReqBuilder.createRequirementFromCapability(identityCapability)
					.buildSyntheticRequirement();
			
			setProperty("-runrequires", "osgi.identity;filter:='(osgi.identity=org.apache.felix.gogo.shell)'");
			resolve(reporter, fir, resolver, resource);

		}

	}

	public void resolve(LogReporter reporter, FixedIndexedRepo fir, Resolver resolver, Resource resource)
			throws Exception {
		trace("resolving %s", resource);

		BndrunResolveContext brc = new BndrunResolveContext(this, null, this, reporter);
		brc.addRepository(fir);
		brc.setInputRequirements(resource.getRequirements(null).toArray(new Requirement[0]));
		brc.init();

		try {
			Map<Resource,List<Wire>> resolve2 = resolver.resolve(brc);
			trace("resolving %s succeeded", resource);
		}
		catch (ResolutionException e) {
			error("!!!! %s :: %s", e.getUnresolvedRequirements());
		}
		catch (Exception e) {
			e.printStackTrace();
			error("resolving %s failed with %s", resource, e);
		}
	}
}
