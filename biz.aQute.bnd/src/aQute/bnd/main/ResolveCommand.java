package aQute.bnd.main;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.OSGI_CORE;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.HeaderClauseFormatter;
import aQute.bnd.header.Parameters;
import aQute.bnd.main.bnd.projectOptions;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.version.Version;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.ProjectResolver;
import biz.aQute.resolve.ResolverValidator;
import biz.aQute.resolve.ResolverValidator.Resolution;

public class ResolveCommand extends Processor {
	private final static Logger	logger	= LoggerFactory.getLogger(ResolveCommand.class);

	private bnd					bnd;

	public ResolveCommand(bnd bnd) {
		super(bnd);
		this.bnd = bnd;
		getSettings(bnd);
	}

	interface FindOptions extends projectOptions {
		String workspace();
	}

	public void _find(FindOptions options, bnd bnd) throws Exception {

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
			List<Requirement> requirements = resource.getResource()
				.getRequirements(null);
			if (!requirements.isEmpty()) {
				bnd.out.println("Requirements:");
				for (Requirement req : requirements) {
					Expression parse = p.parse(req);
					bnd.out.printf("  %-20s %s\n", req.getNamespace(), parse);
				}
			}
			List<Capability> capabilities = resource.getResource()
				.getCapabilities(null);
			if (!capabilities.isEmpty()) {
				bnd.out.println("Capabilities:");
				for (Capability cap : capabilities) {
					Map<String, Object> attrs = new HashMap<>(cap.getAttributes());
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
		@Description("Specify the execution environment used as part of the base, default is JavaSE_1_8")
		EE ee(EE ee);

		@Description("Specify the framework version used as part of the base, [R4_0_1 R4_2_1 R4_3_0 R4_3_1 R5_0_0 R6_0_0]")
		OSGI_CORE core();

		@Description("Specify a system file used as the base (more commonly referred to as a 'distro')")
		String system();

		@Description("Specify a set of packages provided by the base")
		Parameters packages();

		@Description("Specify a set of capabilities provided by the base")
		Parameters capabilities();

		@Description("Include all output details")
		boolean all();
	}

	@Description("Resolve a repository index against a base to determine if the index is 'complete'")
	public void _validate(ValidateOptions options) throws Exception {

		ResourceBuilder system = new ResourceBuilder();

		system.addEE(options.ee(EE.JavaSE_1_8), "system.bundle", Version.emptyVersion.toString());
		if (options.core() != null)
			system.addManifest(options.core()
				.getManifest());

		if (options.packages() != null)
			system.addExportPackages(options.packages(), "system.bundle", Version.emptyVersion.toString());

		if (options.capabilities() != null)
			system.addProvideCapabilities(options.capabilities());

		if (options.system() != null) {
			File f = IO.getFile(options.system());
			if (!f.isFile()) {
				error("Specified system file but not found: %s", f);
				return;
			}
			Domain domain = Domain.domain(f);
			system.addManifest(domain);
		}

		List<String> args = options._arguments();
		File index = getFile(args.remove(0));
		logger.debug("validating {}", index);

		ResolverValidator validator = new ResolverValidator(bnd);
		validator.use(bnd);
		validator.addRepository(index.toURI());
		validator.setSystem(system.build());

		List<Resolution> result = validator.validate();
		Set<Requirement> done = new HashSet<>();

		for (Resolution res : result) {
			if (options.all()) {
				bnd.out.format("%s %-60s%n", res.succeeded ? "OK" : "**", res.resource,
					res.message == null ? "" : res.message);
			}
			if (!res.succeeded) {
				for (Requirement req : res.missing) {
					if (done.contains(req))
						continue;

					bnd.out.format("    missing   %s%n", req);
					done.add(req);
				}
				if (options.all()) {
					for (Requirement req : res.repos) {
						bnd.out.format("    repos     %s%n", req);
					}
					for (Requirement req : res.system) {
						bnd.out.format("    system    %s%n", req);
					}
					for (Requirement req : res.optionals) {
						bnd.out.format("    optional  %s%n", req);
					}
				}
			}
		}

		bnd.getInfo(validator);

	}

	private static final Converter<String, Collection<? extends HeaderClause>> runbundlesFormatter = new CollectionFormatter<>(
		"\n  ", new HeaderClauseFormatter(), "", "  ", "");

	@Arguments(arg = "<path>...")
	interface ResolveOptions extends Options {
		@Description("Use the following workspace")
		String workspace();

		@Description("Specify the project directory if not in a project directory")
		String project();

		@Description("Print out the bundles")
		boolean bundles();

		@Description("Write -runbundles instruction to the file")
		boolean write();
	}

	@Description("Resolve a bndrun file")
	public void _resolve(ResolveOptions options) throws Exception {
		Project project = bnd.getProject(options.project());
		Workspace ws = null;

		if (options.workspace() != null) {
			File file = bnd.getFile(options.workspace());
			if (file.isDirectory()) {
				ws = Workspace.getWorkspace(file);
				if (!ws.isValid()) {
					error("Invalid workspace %s", file);
					return;
				}
			} else {
				error("Workspace directory %s is not a directory", file);
				return;
			}
		} else {
			if (project != null)
				ws = project.getWorkspace();
		}

		List<String> paths = options._arguments();
		for (String path : paths) {
			File f = getFile(path);
			if (!f.isFile()) {
				error("Missing bndrun file: %s", f);
			} else {
				try (Bndrun bndrun = Bndrun.createBndrun(ws, f)) {
					try {
						String runbundles = bndrun.resolve(false, options.write(), runbundlesFormatter);
						if (bndrun.isOk()) {
							System.out.printf("# %-50s ok%n", f.getName());
							if (options.bundles()) {
								System.out.println(runbundles);
							}
						}
					} catch (Exception e) {
						System.out.printf("%-50s %s\n", f.getName(), e);
						exception(e, "Failed to resolve %s: %s", f, e);
					}
					getInfo(bndrun);
				}
			}
		}
	}
}
