package aQute.bnd.main;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.OSGI_CORE;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.instructions.ResolutionInstructions.Runorder;
import aQute.bnd.main.bnd.HandledProjectWorkspaceOptions;
import aQute.bnd.main.bnd.ProjectWorkspaceOptions;
import aQute.bnd.main.bnd.excludeOptions;
import aQute.bnd.main.bnd.projectOptions;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.resolve.Bndrun;
import biz.aQute.resolve.ResolutionCallback;
import biz.aQute.resolve.ResolverValidator;
import biz.aQute.resolve.ResolverValidator.Resolution;
import biz.aQute.resolve.RunResolution;

public class ResolveCommand extends Processor {
	private final static Logger	logger	= LoggerFactory.getLogger(ResolveCommand.class);

	private bnd					bnd;

	public ResolveCommand(bnd bnd) {
		super(bnd);
		this.bnd = bnd;
		getSettings(bnd);
	}

	@Description("Resolve a number of bndrun files (either standalone or based on the workspace) and print the bundles ")
	@Arguments(arg = "bndrun...")
	interface FindOptions extends projectOptions {
		@Description("Override the workspace, if inside a workspace directory then the current workspace is used")
		String workspace();
	}

	@Description("Resolve a number of bndrun files (either standalone or based on the workspace) and print the bundles ")
	public void _find(FindOptions options, bnd bnd) throws Exception {

		List<String> args = options._arguments();

		Project p = bnd.getProject(options.project());
		for (String bndrun : args) {
			Workspace workspace = p == null ? bnd.getWorkspace(options.workspace()) : p.getWorkspace();

			Run run = new Run(workspace, p != null ? p.getBase() : IO.work, IO.getFile(bndrun));
			biz.aQute.resolve.RunResolution resolution = biz.aQute.resolve.RunResolution.resolve(run, null)
				.reportException();
			bnd.out.println("Resolved " + run);
			for (Container c : resolution.getContainers()) {
				bnd.out.printf("%-30s %-20s %-6s %s\n", c.getBundleSymbolicName(), c.getVersion(), c.getType(),
					c.getFile());
			}
			bnd.getInfo(run);
		}
		bnd.getInfo(p);
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

	@Description("Validate an OBR file by trying to resolve each entry against itself")
	@Arguments(arg = {
		"index-path"
	})
	interface ValidateOptions extends Options {
		@Description("Specify the execution environment used as part of the base, default is JavaSE_1_8")
		EE ee(EE ee);

		@Description("Specify the framework version used as part of the base, [R4_0_1 R4_2_1 R4_3_0 R4_3_1 R5_0_0 R6_0_0 R7_0_0]")
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

		system.addEE(options.ee(EE.JavaSE_1_8));
		if (options.core() != null)
			system.addManifest(options.core()
				.getManifest());

		if (options.packages() != null)
			system.addExportPackages(options.packages());

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

		try (ResolverValidator validator = new ResolverValidator(bnd)) {
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

	}

	@Description("Resolve a bndrun file")
	@Arguments(arg = {
		"<path>..."
	})
	interface ResolveOptions extends ProjectWorkspaceOptions, excludeOptions {

		@Description("Print out the bundles")
		boolean bundles();

		@Description("Print out the bundle urls")
		boolean urls();

		@Description("Print out the bundle files")
		boolean files();

		@Description("Write -runbundles instruction back to the file")
		boolean write();

		@Description("Override the -runorder")
		Runorder runorder();

		@Description("Fail on changes")
		boolean xchange();

		@Description("Show the optionals")
		boolean optionals();

		@Description("Create a dependency file")
		boolean dot();

		@Description("Quiet")
		boolean quiet();
	}

	@Description("Resolve a bndrun file")
	public void _resolve(ResolveOptions options) throws Exception {
		HandledProjectWorkspaceOptions hwpo = bnd.handleOptions(options, aQute.bnd.main.bnd.BNDRUN_ALL);

		for (File f : hwpo.files()) {
			if (options.verbose())
				bnd.out.println("resolve " + f);

			if (!f.isFile()) {
				error("Missing bndrun file: %s", f);
			} else {
				try (Bndrun bndrun = Bndrun.createBndrun(hwpo.workspace(), f)) {

					try {
						if (options.runorder() != null)
							bndrun.setProperty("-runorder", options.runorder()
								.toString());

						RunResolution resolution = bndrun.resolve(quiet(options.quiet()));

						bnd.out.println();

						if (bndrun.isOk()) {

							if (options.urls()) {
								bnd.out.printf("# URLS ", f.getName());
								doUrls(resolution.getOrderedResources());
								bnd.out.println();
							}

							if (options.bundles()) {
								bnd.out.printf("# BUNDLES ", f.getName());
								doVersionedClauses(resolution.getRunBundles());
								bnd.out.println();
							}

							if (options.files()) {
								bnd.out.printf("# FILES ", f.getName());
								doFiles(resolution.getContainers());
								bnd.out.println();
							}

							if (options.optionals()) {
								bnd.out.printf("# OPTIONALS ", f.getName());
								bnd.out.println("# Optionals");
								doUrls(resolution.optional.keySet());
								bnd.out.println();
							}

							bndrun.update(resolution, options.xchange(), options.write());
						} else {
							if (!options.quiet()) {
								bnd.out.printf("Failed to resolve\n");
								bnd.out.println(resolution.report(true));
							}
						}
					} catch (Exception e) {
						bnd.out.printf("%-50s %s\n", f.getName(), e);
						exception(e, "Failed to resolve %s: %s", f, e);
					} finally {
						getInfo(bndrun);
					}
				}
			}
		}
	}

	private void doFiles(Collection<Container> runbundles) {
		try {
			for (Container r : runbundles) {
				if (r.getType() == TYPE.ERROR) {
					bnd.error("Invalid bundle reference %s", r);
				} else {
					bnd.out.println(r.getFile());
				}
			}
		} catch (Exception e) {
			bnd.error("Could not get the runbundles %s", e.getMessage());
		}
	}

	private void doVersionedClauses(Collection<VersionedClause> runbundles) {
		for (VersionedClause r : runbundles) {
			bnd.out.println(r.toString());
		}
	}

	private void doUrls(Collection<Resource> runbundles) {
		for (Resource r : runbundles) {
			URI uri = ResourceUtils.getURI(r)
				.orElse(null);
			if (uri == null) {
				bnd.error("No content capability %s", r);
			} else {
				bnd.out.println(uri);
			}
		}
	}

	@Arguments(arg = {
		"bndrun-file"
	})
	@Description("Create a dot file")
	interface DotOptions extends Options {
		@Description("Send to file")
		String output();

		@Description("Override the -runorder")
		Runorder runorder();

		@Description("Quiet")
		boolean quiet();
	}

	@Description("Create a dot file")
	public void _dot(DotOptions options) throws Exception {
		File f = bnd.getFile(options._arguments()
			.get(0));
		if (f.isFile()) {
			Workspace w = Workspace.findWorkspace(f.getParentFile());
			Bndrun bndrun = Bndrun.createBndrun(w, f);
			if (options.runorder() != null)
				bndrun.setProperty("-runorder", options.runorder()
					.toString());

			RunResolution resolution = bndrun.resolve(quiet(options.quiet()));
			if (bndrun.isOk()) {
				String dot = resolution.dot(bndrun.getName());

				if (options.output() != null) {
					IO.store(dot, IO.getFile(options.output()));
				} else {
					bnd.out.println(dot);
				}
			}
			getInfo(bndrun);
		} else {
			error("No such file " + f);
		}
	}

	private ResolutionCallback quiet(boolean quiet) {
		return quiet ? (a, b, c) -> {} : (a, b, c) -> bnd.out.print(".");
	}

}
