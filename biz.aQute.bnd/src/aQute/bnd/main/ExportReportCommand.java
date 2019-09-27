package aQute.bnd.main;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.main.bnd.HandledProjectWorkspaceOptions;
import aQute.bnd.main.bnd.ProjectWorkspaceOptions;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.justif.Justif;
import biz.aQute.bnd.reporter.exporter.ReportExporterBuilder;
import biz.aQute.bnd.reporter.generator.ReportGeneratorBuilder;

@Description(value = "Generate and export reports of a workspace, a project or of a jar.")
public class ExportReportCommand extends Processor {

	private bnd _bnd;

	public ExportReportCommand(final bnd bnd) {
		super(bnd);
		_bnd = bnd;
		use(bnd);
		setBase(bnd.getBase());
	}

	@Description(value = "Generate and export reports of a workspace, a project or of a jar.")
	@Arguments(arg = {
		"[list | export | jarexport | readme | jarreadme]", "..."
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

	@Description(value = "List the user defined reports.")
	public interface ListOptions extends ProjectWorkspaceOptions {}

	@Description(value = "List the user defined reports.")
	public void _list(ListOptions options) throws Exception {
		HandledProjectWorkspaceOptions o = _bnd.handleOptions(options);

		if (options.project() == null && o.workspace()
			.getProject(getBase().getName()) == null) {
			listReports(o.workspace(), "workspace", "Workspace[" + o.workspace()
				.getBase()
				.getName() + "]");
		}
		_bnd.perProject(options, p -> listReports(p, "project", "Project[" + p.getName() + "]"), false);
	}

	private void listReports(Processor p, String scope, String resultPrefix) {
		List<String> reports = ReportExporterBuilder.create()
			.setProcessor(p)
			.setScope(scope)
			.build()
			.getAvailableReportsOf(p);

		if (reports.isEmpty()) {
			System.out.println(resultPrefix + ": no reports");
		} else {
			reports.forEach(path -> {
				System.out.println(resultPrefix + ": " + path);
			});
		}
		getInfo(p);
	}

	@Description(value = "Export the user defined reports.")
	public interface ExportOptions extends ProjectWorkspaceOptions {}

	@Description(value = "Export the user defined reports.")
	public void _export(ExportOptions options) throws Exception {
		HandledProjectWorkspaceOptions o = _bnd.handleOptions(options);

		if (options.project() == null && o.workspace()
			.getProject(getBase().getName()) == null) {
			exportReports(o.workspace(), "workspace");
		}
		_bnd.perProject(options, p -> this.exportReports(p, "project"), false);
	}

	private void exportReports(Workspace ws, String scope) {
		writeReports(ReportExporterBuilder.create()
			.setProcessor(ws)
			.setGenerator(ReportGeneratorBuilder.create()
				.setProcessor(ws)
				.useCustomConfig()
				.withAggregatorProjectDefaultPlugins()
				.build())
			.setScope(scope)
			.build()
			.exportReportsOf(ws),
			"Workspace[" + ws.getBase()
				.getName() + "]: ");
	}

	private void exportReports(Project p, String scope) {
		writeReports(ReportExporterBuilder.create()
			.setProcessor(p)
			.setGenerator(ReportGeneratorBuilder.create()
				.setProcessor(p)
				.useCustomConfig()
				.withProjectDefaultPlugins()
				.build())
			.setScope(scope)
			.build()
			.exportReportsOf(p), "Project[" + p.getName() + "]: ");
	}

	private void writeReports(Map<String, Resource> reports, String prefix) {
		if (reports.isEmpty()) {
			System.out.println(prefix + "no reports");
		} else {
			reports.forEach((f, r) -> {
				try {
					r.write(new FileOutputStream(f));
					System.out.println(prefix + f);
				} catch (Exception e1) {
					exception(e1, prefix + "failed to write report at %s", f);
				}
			});
		}
	}

	@Description(value = "Export a set of readme files (template can be parametrized with system properties starting with 'bnd.reporter.*').")
	public interface ReadmeOptions extends ProjectWorkspaceOptions {}

	@Description(value = "Export a set of readme files (template can be parametrized with system properties starting with 'bnd.reporter.*').")
	public void _readme(ExportOptions options) throws Exception {
		HandledProjectWorkspaceOptions o = _bnd.handleOptions(options);

		if (options.project() == null && o.workspace()
			.getProject(getBase().getName()) == null) {
			configureReadme(o.workspace());
			exportReports(o.workspace(), "__autoreadme-workspace");
		}
		_bnd.perProject(options, this::configureReadme, false);
		_bnd.perProject(options, p -> this.exportReports(p, "__autoreadme-project"), false);
	}

	private void configureReadme(Workspace ws) {
		ws.set("-exportreport.__autoreadme",
			"${workspace}/readme.md;scope=__autoreadme-workspace;template=default:readme.twig" + getParameters());
	}

	private void configureReadme(Project p) throws Exception {
		Map<String, String> subNames = new HashMap<>();

		try (ProjectBuilder pb = p.getBuilder(null)) {
			List<Builder> builders = pb.getSubBuilders();

			if (builders.size() > 1) {
				for (Builder b : builders) {
					String bns = b.getBsn();
					String name = bns;
					if (name.startsWith(p.getBase()
						.getName() + ".")) {
						name = name.substring(p.getBase()
							.getName()
							.length() + 1);
					}
					subNames.put(bns, name);
				}
			}
		}

		StringBuilder generatedProp = new StringBuilder();

		generatedProp.append("readme.md;scope=__autoreadme-project;template=default:readme.twig" + getParameters());
		subNames.forEach((bns, subname) -> {
			Map<String, String> params = new HashMap<>();
			params.put("currentBsn", bns);

			generatedProp.append(",readme." + subname + ".md;scope=__autoreadme-project;template=default:readme.twig"
				+ getParameters(params));
		});

		p.set("-exportreport.__autoreadme-" + p.getBase()
			.getName(), generatedProp.toString());
	}

	private String getParameters(Map<String, String> otherParameters) {
		Map<String, String> params = new HashMap<>(otherParameters);

		System.getProperties()
			.stringPropertyNames()
			.stream()
			.filter(k -> k.startsWith("bnd.reporter."))
			.forEach(k -> {
				if (System.getProperty(k) != null) {
					params.put(k, System.getProperty(k));
				}
			});

		StringBuilder param = new StringBuilder();

		if (!params.isEmpty()) {
			param.append(";parameters='");
		}

		params.entrySet()
			.forEach(e -> {
				param.append(e.getKey() + "=" + e.getValue() + ",");
			});

		if (!params.isEmpty()) {
			param.deleteCharAt(param.length() - 1);
			param.append("'");
		}
		return param.toString();
	}

	private String getParameters() {
		return getParameters(new HashMap<>());
	}

	@Description(value = "Export a custom report of a Jar.")
	@Arguments(arg = {
		"jar path", "output path"
	})
	public interface JarExportOptions extends Options {
		@Description("Path to a property file")
		String properties(String deflt);

		@Description("A locale (language-COUNTRY-variant) used to localized the report data.")
		String locale();

		@Description("A configuration name defined in the property file (check -reportconfig documentation), if not set a default configuration will be used.")
		String configName();

		@Description("The template type (aka template file extension), must be set if it "
			+ "could not be guess from the template file name.")
		String templateType();

		@Description("Path or URL to a template file used to transform "
			+ "the generated report (twig or xslt). eg: --template bundle.xslt")
		String template();

		@Description("A list of parameters that will be provided to the transformation process if any. eg: --parameters 'param1=value1,param2=value2'")
		String[] parameters();
	}

	@Description(value = "Export a custom report of a Jar.")
	public void _jarexport(JarExportOptions options) throws Exception {
		Processor processor = getProcessor(options.properties(""));

		String jarPath = options._arguments()
			.get(0);
		String outputPath = options._arguments()
			.get(1);
		Jar jar = _bnd.getJar(jarPath);

		if (isOk() && jar != null) {
			processor.setProperty("-exportreport.bundle.commandline", buildExportInstruction(options, outputPath));

			Map<String, Resource> reports = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(ReportGeneratorBuilder.create()
					.setProcessor(processor)
					.useCustomConfig()
					.withBundleDefaultPlugins()
					.build())
				.build()
				.exportReportsOf(jar);

			getInfo(processor);

			String prefix = "Jar[" + jar.getName() + "]: ";
			reports.forEach((f, r) -> {
				try {
					r.write(new FileOutputStream(f));
					System.out.println(prefix + f);
				} catch (Exception e1) {
					exception(e1, prefix + "failed to write report at %s", f);
				}
			});
		}
	}

	public String buildExportInstruction(JarExportOptions options, String destination) {
		final StringBuilder propBuilder = new StringBuilder();

		propBuilder.append(destination);

		if (options.template() != null) {
			propBuilder.append(";template=" + options.template());
		}

		if (options.templateType() != null) {
			propBuilder.append(";templateType=" + options.templateType());
		}

		if (options.locale() != null) {
			propBuilder.append(";locale=" + options.locale());
		}

		if (options.configName() != null) {
			propBuilder.append(";configName=" + options.configName());
		}

		if (options.parameters() != null && options.parameters().length > 0) {
			propBuilder.append(";parameters='");
			for (final String parameter : options.parameters()) {
				propBuilder.append(parameter + ",");
			}
			propBuilder.deleteCharAt(propBuilder.length() - 1);
			propBuilder.append("'");
		}
		return propBuilder.toString();
	}

	private Processor getProcessor(String basePath) throws Exception {
		Processor processor = new Processor();

		File f = getFile(getBase(), basePath);
		if (f.isFile()) {
			processor.setProperties(f);
		} else {
			processor.setBase(getBase());
		}

		return processor;
	}

	@Description(value = "Export a readme file of a Jar (template can be parametrized with system properties starting with 'bnd.reporter.*').")
	@Arguments(arg = {
		"jar path", "output path"
	})
	public interface JarReadmeOptions extends ProjectWorkspaceOptions {}

	@Description(value = "Export a readme file of a Jar (template can be parametrized with system properties starting with 'bnd.reporter.*').")
	public void _jarreadme(JarReadmeOptions options) throws Exception {
		Processor processor = getProcessor("");

		String jarPath = options._arguments()
			.get(0);
		String outputPath = options._arguments()
			.get(1);
		Jar jar = _bnd.getJar(jarPath);

		if (isOk() && jar != null) {
			processor.setProperty("-exportreport.bundle.commandline",
				outputPath + ";template=default:readme.twig" + getParameters());

			Map<String, Resource> reports = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(ReportGeneratorBuilder.create()
					.setProcessor(processor)
					.useCustomConfig()
					.withBundleDefaultPlugins()
					.build())
				.build()
				.exportReportsOf(jar);

			getInfo(processor);

			String prefix = "Jar[" + jar.getName() + "]: ";
			reports.forEach((f, r) -> {
				try {
					r.write(new FileOutputStream(f));
					System.out.println(prefix + f);
				} catch (Exception e1) {
					exception(e1, prefix + "failed to write report at %s", f);
				}
			});
		}
	}
}
