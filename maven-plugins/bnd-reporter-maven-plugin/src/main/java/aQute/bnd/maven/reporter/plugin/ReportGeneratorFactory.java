package aQute.bnd.maven.reporter.plugin;

import aQute.bnd.maven.reporter.plugin.entries.mavenproject.CodeSnippetPlugin;
import aQute.bnd.maven.reporter.plugin.entries.mavenproject.CommonInfoPlugin;
import aQute.bnd.maven.reporter.plugin.entries.mavenproject.FileNamePlugin;
import aQute.bnd.maven.reporter.plugin.entries.mavenproject.MavenAggregatorConcentPlugin;
import aQute.bnd.maven.reporter.plugin.entries.mavenproject.MavenProjectContentPlugin;
import biz.aQute.bnd.reporter.generator.ReportGeneratorBuilder;

/**
 * Create a report generator builder with the specific maven plugins registered.
 */
final public class ReportGeneratorFactory {

	static public ReportGeneratorBuilder create() {
		return ReportGeneratorBuilder.create()
			.registerPlugin(CommonInfoPlugin.class.getCanonicalName())
			.registerPlugin(MavenProjectContentPlugin.class.getCanonicalName())
			.registerPlugin(MavenAggregatorConcentPlugin.class.getCanonicalName())
			.registerPlugin(FileNamePlugin.class.getCanonicalName())
			.registerPlugin(CodeSnippetPlugin.class.getCanonicalName());
	}
}
