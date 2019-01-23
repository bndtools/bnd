package aQute.bnd.main;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.main.bnd.HandledProjectWorkspaceOptions;
import aQute.bnd.main.bnd.ProjectWorkspaceOptions;
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
		"[list | export | jarexport]", "..."
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

	@Description(value = "List available reports.")
	public interface ListOptions extends ProjectWorkspaceOptions {}

	@Description(value = "List available reports.")
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

	@Description(value = "Export the reports.")
	public interface ExportOptions extends ProjectWorkspaceOptions {}

	@Description(value = "Export the reports.")
	public void _export(ExportOptions options) throws Exception {
		HandledProjectWorkspaceOptions o = _bnd.handleOptions(options);

		if (options.project() == null && o.workspace()
			.getProject(getBase().getName()) == null) {
			exportReports(o.workspace());
		}
		_bnd.perProject(options, this::exportReports, false);
	}

	private void exportReports(Workspace ws) {
		printExportResult(ReportExporterBuilder.create()
			.setProcessor(ws)
			.setGenerator(ReportGeneratorBuilder.create()
				.setProcessor(ws)
				.useCustomConfig()
				.withAggregatorProjectDefaultPlugins()
				.build())
			.setScope("workspace")
			.build()
			.exportReportsOf(ws),
			"Workspace[" + ws.getBase()
				.getName() + "]: ");
	}

	private void exportReports(Project p) {
		printExportResult(ReportExporterBuilder.create()
			.setProcessor(p)
			.setGenerator(ReportGeneratorBuilder.create()
				.setProcessor(p)
				.useCustomConfig()
				.withProjectDefaultPlugins()
				.build())
			.setScope("project")
			.build()
			.exportReportsOf(p), "Project[" + p.getName() + "]: ");
	}

	private void printExportResult(Map<String, Resource> reports, String prefix) {
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

	@Description(value = "Export a report of a Jar.")
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

	@Description(value = "Export a report of a Jar.")
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
}
