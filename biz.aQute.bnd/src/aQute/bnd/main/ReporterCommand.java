package aQute.bnd.main;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.justif.Justif;
import biz.aQute.bnd.reporter.generator.BundleReportGenerator;
import biz.aQute.bnd.reporter.generator.ProjectReportGenerator;
import biz.aQute.bnd.reporter.generator.WorkspaceReportGenerator;

@Description(value = "Extract and generate documents of a workspace, a project or a jar.")
public class ReporterCommand extends Processor {

	public ReporterCommand(final Processor p) {
		super(p);
	}

	@Description(value = "Extract and generate documents of a workspace, a project or a jar.")
	@Arguments(arg = {
			"[worskpace | project | jar]", "..."
	})
	public interface ReporterOptions extends Options {}

	public void run(final ReporterOptions options) throws Exception {
		CommandLine handler = options._command();
		List<String> arguments = options._arguments();

		if (arguments.isEmpty()) {
			Justif f = new Justif(80, 20, 22, 72);
			handler.help(f.formatter(), this);
			System.err.append(f.wrap());
		} else {
			String cmd = arguments.remove(0);
			String help = handler.execute(this, cmd, arguments);
			if (help != null) {
				System.err.println(help);
			}
		}
	}

	@Description(value = "Extract and generate documents of a workspace."
			+ " By default, show the list of documents that can be generated."
			+ " If a glob expression is specified, it will generate the matched documents.")
	@Arguments(arg = {
			"[glob expression]"
	})
	public interface ReporterWsOptions extends Options {
		@Description("Path to workspace, default current directory")
		String workspace();
	}

	@Description(value = "Extract and generate documents of a workspace.")
	public void _workspace(ReporterWsOptions wsOptions) throws Exception {
		File base = null;
		String glob = null;

		if (!wsOptions._arguments().isEmpty()) {
			glob = wsOptions._arguments().get(0);
		}

		if (wsOptions.workspace() == null) {
			base = getBase();
		} else {
			base = getFile(wsOptions.workspace());
		}

		Workspace ws = Workspace.findWorkspace(base);

		if (ws != null) {
			if (glob == null) {
				try (WorkspaceReportGenerator g = new WorkspaceReportGenerator(ws)) {
					System.out.println("-------------------");
					System.out.println("Available reports:");
					Set<String> reports = g.getAvailableReports();
					reports.forEach(System.out::println);
					this.getInfo(g);
				}
			} else {
				try (WorkspaceReportGenerator g = new WorkspaceReportGenerator(ws)) {
					System.out.println("-------------------");
					System.out.println("Generate reports: [glob: " + glob + "]");
					g.generateReports(glob);
					this.getInfo(g);
				}
			}
		} else {
			error("Workspace not found: %s", base);
		}
	}

	@Description(value = "Extract and generate documents of a project."
			+ " By default, show the list of documents that can be generated."
			+ " If a glob expression is specified, it will generate the matched documents.")
	@Arguments(arg = {
			"[glob expression]"
	})
	public interface ReporterProjOptions extends Options {
		@Description("Path to project, default current directory")
		String project();
	}

	@Description(value = "Extract and generate documents of a project.")
	public void _project(ReporterProjOptions projOptions) throws Exception {
		Project project = getProject(projOptions.project());
		String glob = null;

		if (!projOptions._arguments().isEmpty()) {
			glob = projOptions._arguments().get(0);
		}

		if (project != null) {
			if (glob == null) {
				try (ProjectReportGenerator g = new ProjectReportGenerator(project)) {
					System.out.println("-------------------");
					System.out.println("Available reports:");
					Set<String> reports = g.getAvailableReports();
					reports.forEach(System.out::println);
					this.getInfo(g);
				}
			} else {
				try (ProjectReportGenerator g = new ProjectReportGenerator(project)) {
					System.out.println("-------------------");
					System.out.println("Generate reports: [glob: " + glob + "]");
					g.generateReports(glob);
					this.getInfo(g);
				}
			}
		}
	}

	private Project getProject(String where) throws Exception {
		if (where == null || where.equals("."))
			where = Project.BNDFILE;

		File f = getFile(where);
		if (f.isDirectory()) {
			f = new File(f, Project.BNDFILE);
		}

		if (f.isFile()) {
			if (f.getName().endsWith(Run.DEFAULT_BNDRUN_EXTENSION)) {
				Workspace ws = Workspace.findWorkspace(f.getParentFile());
				Run run = Run.createRun(ws, f);
				return run;
			}

			File projectDir = f.getParentFile();
			File workspaceDir = projectDir.getParentFile();
			Workspace ws = Workspace.findWorkspace(workspaceDir);
			Project project = ws.getProject(projectDir.getName());
			if (project.isValid()) {
				project.use(this);
				return project;
			}
		}

		error("Project not found: %s", f);
		return null;
	}

	@Description(value = "Extract and generate a document of a jar.")
	@Arguments(arg = {
			"jar path", "output path"
	})
	public interface ReporterJarOptions extends Options {
		@Description("Path to a bnd file to use as base.")
		String bndfile();

		@Description("A list of locales <language_COUNTRY_variant>. Used to extract the Jar"
				+ " metadata for different languages. eg: --locales 'en,en_US,fr'")
		String[] locales();

		@Description("Paths to files that must be imported into the extracted metadata. If a path start with `@`, "
				+ "it will be relative to the root of the Jar. "
				+ "The entry name and the format will be derived from the file name and its extension,"
				+ " but can be overridden with the syntax <path>:<parentName>:<type>. "
				+ "eg: --imports '@maven/pom.xml, ./config.cfg:configuration:properties'")
		String[] imports();

		@Description("A list of properties to include in the extracted metadata. eg: --properties 'oneProp=Rambo,anotherProp=Titeuf'")
		String[] properties();

		@Description("Paths to a template file used to transform the extracted metadata. eg: --template bundle.xslt")
		String template();

		@Description("A list of parameters that will be provided to the transformation process if any. eg: --parameters 'param1=value1,param2=value2'")
		String[] parameters();
	}

	@Description(value = "Extract and generate documents of a jar.")
	public void _jar(ReporterJarOptions jarOptions) throws Exception {
		String jarPath = jarOptions._arguments().get(0);
		String outputPath = jarOptions._arguments().get(1);
		Jar jar = getJar(jarPath);

		if (jarOptions.bndfile() != null) {
			setProperties(getFile(jarOptions.bndfile()));
		}

		if (isOk() && jar != null) {
			setProperty(Constants.REPORT_BUNDLE_MODEL, getMetadataProperty(jarOptions));

			if (jarOptions.template() != null) {

				final StringBuilder parameterBuilder = new StringBuilder();

				if (jarOptions.parameters() != null) {
					for (final String parameter : jarOptions.parameters()) {
						final String[] parts = parameter.split("=");

						if (parts.length > 1) {
							String value = "";
							for (int i = 1; i < parts.length; i++) {
								value = value + parts[i];
							}
							parameterBuilder.append(parts[0].trim() + "='" + value.trim() + "';");
						}
					}
					if (parameterBuilder.length() > 0) {
						parameterBuilder.deleteCharAt(parameterBuilder.length() - 1);
						parameterBuilder.insert(0, ";");
					}
				}

				setProperty(Constants.REPORT_BUNDLE, outputPath + ";"
						+ BundleReportGenerator.TEMPLATE_DIRECTIVE + "=" + jarOptions.template()
						+ parameterBuilder.toString());
			} else {
				setProperty(Constants.REPORT_BUNDLE, outputPath);
			}

			try (BundleReportGenerator g = new BundleReportGenerator(jar, this)) {
				g.generateReports("*");
				this.getInfo(g);
			}
		}
	}

	public String getMetadataProperty(ReporterJarOptions jarOptions) {
		final StringBuilder propBuilder = new StringBuilder();

		if (jarOptions.properties() != null) {
			for (final String property : jarOptions.properties()) {
				final String[] parts = property.split("=");

				if (parts.length > 1) {
					String value = "";
					for (int i = 1; i < parts.length; i++) {
						value = value + parts[i];
					}
					propBuilder.append("@" + parts[0].trim() + "='" + value.trim() + "',");
				}
			}
		}

		if (jarOptions.locales() != null && jarOptions.locales().length > 0) {
			propBuilder.append(BundleReportGenerator.LOCALES_PROPERTY + "='");
			for (final String entry : jarOptions.locales()) {
				propBuilder.append(entry + ",");
			}
			if (jarOptions.locales().length == 1) {
				propBuilder.deleteCharAt(propBuilder.length() - 1);
			}
			propBuilder.append("',");
		}

		if (jarOptions.imports() != null && jarOptions.imports().length > 0) {
			propBuilder.append(BundleReportGenerator.IMPORTS_PROPERTY + "='");
			for (final String importPath : jarOptions.imports()) {
				propBuilder.append(importPath + ",");
			}
			if (jarOptions.imports().length == 1) {
				propBuilder.deleteCharAt(propBuilder.length() - 1);
			}
			propBuilder.append("',");
		}

		if (propBuilder.length() > 0) {
			propBuilder.deleteCharAt(propBuilder.length() - 1);
		}
		return propBuilder.toString();
	}

	public Jar getJar(String s) {
		File f = getFile(s);
		if (f.isFile()) {
			try {
				return new Jar(f);
			} catch (ZipException e) {
				exception(e, "Not a jar/zip file: %s", f);
			} catch (Exception e) {
				exception(e, "Opening file: %s", f);
			}
			return null;
		}

		try {
			URL url = new URL(s);
			return new Jar(s, url.openStream());
		} catch (Exception e) {
			// Ignore
		}

		error("Not a file or proper url: %s", f);
		return null;
	}
}
