package aQute.bnd.main;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.OSGI_CORE;
import aQute.bnd.header.Parameters;
import aQute.bnd.main.bnd.projectOptions;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.resolve.ProjectResolver;
import biz.aQute.resolve.ResolverValidator;

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
		EE ee(EE ee);

		OSGI_CORE core();

		String system();

		Parameters packages();

		Parameters capabilities();
	}

	public void _validate(ValidateOptions options) throws Exception {

		ResourceBuilder system = new ResourceBuilder();

		system.addEE(options.ee(EE.JavaSE_1_8));
		if (options.core() != null)
			system.addManifest(options.core().getManifest());

		if (options.packages() != null)
			system.addExportPackages(options.packages());

		if (options.capabilities() != null)
			system.addProvideCapabilities(options.capabilities());

		if ( options.system() != null) {
			File f = IO.getFile(options.system());
			if ( !f.isFile()) {
				error("Specified system file but not found: " + f);
				return;
			}
			Domain domain = Domain.domain(f);
			system.addManifest(domain);
		}

		List<String> args = options._arguments();
		File index = getFile(args.remove(0));
		trace("validating %s", index);

		ResolverValidator validator = new ResolverValidator(bnd);
		validator.use(bnd);
		validator.addRepository(index.toURI());
		validator.setSystem(system.build());

		validator.validate();

		bnd.getInfo(validator);

	}

}
