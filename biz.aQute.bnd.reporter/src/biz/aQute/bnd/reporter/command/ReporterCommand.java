package biz.aQute.bnd.reporter.command;

import aQute.bnd.osgi.Processor;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import biz.aQute.bnd.reporter.generator.ReportConfig;
import biz.aQute.bnd.reporter.generator.ReportGenerator;

public class ReporterCommand extends Processor {

	public ReporterCommand(final Processor p) {
		super(p);
	}

	@Description("The `report` command allows to generate a report from a Jar. "
			+ "The command will first extract and convert the input Jar metadata into an XML representation, then the provided XSLT will "
			+ "be apply in order. If no ouput file is provided, the result will be printed to the standard output."
			+ " The content of the generated report can be controlled by plugins.")
	@Arguments(arg = { "path" })
	public interface ReporterOptions extends Options {

		@Description("Path to the XSLT templates.")
		String[] templates();

		@Description("Path to an output file. The path is relative to the root of the input Jar.")
		String output();

		@Description("Locale <language_COUNTRY_variant>. Used to extract the Jar metadata for a specific locale.")
		String locale();

		@Description("Plugins to use for the report generation.")
		String[] plugins();

		@Description("Paths to the files that need to be included in the report. If a path start with `@`, "
				+ "it will be relative to the root of the input Jar. Supported file types are xml, json, properties and manifest. "
				+ "Types and parent element name will be set automatically, but can be overridden with the syntax <path>:<type>:<parentName>")
		String[] includes();
	}

	public void run(final ReporterOptions options) throws Exception {

		final ReportConfig.Builder config = ReportConfig.builder(options._arguments().remove(0));

		if (options.output() != null) {
			config.setOutput(options.output());
		}

		if (options.templates() != null) {
			for (final String template : options.templates()) {
				config.addTemplates(template);
			}
		}

		if (options.locale() != null) {
			config.setLocale(options.locale());
		}

		if (options.includes() != null) {
			for (final String include : options.includes()) {
				final String[] parts = include.trim().split(":");
				if (parts.length == 1) {
					config.addIncludePath(parts[0], null, null);
				} else if (parts.length == 2) {
					if (parts[1].isEmpty()) {
						parts[1] = null;
					}
					config.addIncludePath(parts[0], parts[1], null);
				} else if (parts.length == 3) {
					if (parts[1].isEmpty()) {
						parts[1] = null;
					}
					if (parts[2].isEmpty()) {
						parts[2] = null;
					}
					config.addIncludePath(parts[0], parts[1], parts[2]);
				}
			}
		}

		try (ReportGenerator rg = new ReportGenerator(this)) {
			if (options.plugins() != null && options.plugins().length > 0) {
				final StringBuilder b = new StringBuilder();
				for (final String o : options.plugins()) {
					b.append(o + ",");
				}
				b.deleteCharAt(b.length() - 1);
				rg.getParent().setProperty("-plugin.ReportGenerator", b.toString());
			}
			rg.generate(config.build());
			getInfo(rg);
		}
	}
}
