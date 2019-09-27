package aQute.bnd.maven.reporter.plugin;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.reporter.ReportExporterService;
import aQute.bnd.service.reporter.ReportGeneratorService;
import biz.aQute.bnd.reporter.exporter.ReportExporterBuilder;

/**
 * Exports a set of user defined reports.
 */
@Mojo(name = "export", threadSafe = true)
public class ExportMojo extends AbstractMojo {

	private static final String			AGGREGATOR_SCOPE	= "aggregator";
	private static final String			PROJECT_SCOPE		= "project";

	private static final Logger			logger				= LoggerFactory.getLogger(ExportMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject				project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession				session;

	@Parameter(property = "bnd.reporter.skip", defaultValue = "false")
	private boolean						skip;

	@Parameter
	private List<Report>				reports				= new ArrayList<>();

	@Parameter
	private Map<String, ReportConfig>	reportConfigs		= new HashMap<>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			logger.debug("skip project as configured");
			return;
		}

		try (Processor processor = new Processor()) {
			processor.setTrace(logger.isDebugEnabled());
			processor.setBase(project.getBasedir());

			MavenProjectWrapper toAnalyze = new MavenProjectWrapper(session.getProjects(), project);

			// Add the report configurations to the processor
			for (Entry<String, ReportConfig> reportConfig : reportConfigs.entrySet()) {

				String key = "-reportconfig." + reportConfig.getKey();
				String instruction = reportConfig.getValue()
					.toInstruction();

				processor.setProperty(key, instruction);

				// Add the report configuration to the analyzed object in case
				// of aggregator.
				toAnalyze.getReportConfig()
					.setProperty(key, instruction);
			}

			// Add the defined reports to the processor
			int index = 0;
			for (Report report : reports) {
				processor.setProperty("-exportreport." + index++, report.toInstruction());
			}

			// Create the generator service.
			ReportGeneratorService generator = null;
			String scope = null;
			if (toAnalyze.isAggregator()) {
				generator = ReportGeneratorFactory.create()
					.setProcessor(processor)
					.useCustomConfig()
					.withAggregatorProjectDefaultPlugins()
					.build();
				scope = AGGREGATOR_SCOPE;
			} else {
				generator = ReportGeneratorFactory.create()
					.setProcessor(processor)
					.useCustomConfig()
					.withProjectDefaultPlugins()
					.build();
				scope = PROJECT_SCOPE;
			}

			// Create the exporter service.
			ReportExporterService reporter = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(generator)
				.setScope(scope)
				.build();

			logger.info("Generating reports...");

			Map<String, Resource> reportResults = reporter.exportReportsOf(toAnalyze);

			report(processor);

			if (!processor.isOk()) {
				throw new MojoExecutionException("Errors in bnd processing, see log for details.");
			}

			if (reportResults.isEmpty()) {
				logger.info("No report matching the '{}' scope has been found.", scope);
			}

			for (Entry<String, Resource> result : reportResults.entrySet()) {
				try {
					result.getValue()
						.write(new FileOutputStream(result.getKey()));
					logger.info("The report at {} has been successfully created.", result.getKey());
				} catch (Exception exception) {
					throw new MojoExecutionException("Failed to write the report at " + result.getKey(), exception);
				}
			}
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException("bnd error: " + e.getMessage(), e);
		}
	}

	private void report(Processor processor) {
		for (String warning : processor.getWarnings()) {
			logger.warn("Warning : {}", warning);
		}
		for (String error : processor.getErrors()) {
			logger.error("Error   : {}", error);
		}
	}
}
