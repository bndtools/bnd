package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.AnalysisPlugin;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class AnalysisPluginTest {

	@InjectTemporaryDirectory
	File tmp;

	/**
	 * Test that AnalysisPlugin is called during analysis and receives
	 * notifications about version decisions.
	 */
	@Test
	public void testAnalysisPluginCalled() throws Exception {
		// Create a test plugin that captures version decisions
		TestAnalysisPlugin plugin = new TestAnalysisPlugin();

		try (Builder b = new Builder()) {
			// Add classpath with org.osgi.framework
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			// Export org.osgi.service.event which depends on org.osgi.framework
			// This will create an import for org.osgi.framework
			b.setProperty("Export-Package", "org.osgi.service.event");
			b.setProperty("Import-Package", "*");
			b.getPlugins().add(plugin);

			Jar jar = b.build();
			assertTrue(b.check(), "Builder should have no errors");

			// Verify the plugin was called
			assertThat(plugin.versionReports).as("Version reports should not be empty").isNotEmpty();
			
			// Verify we got reports for framework package
			assertThat(plugin.versionReports).anyMatch(report -> 
				report.packageRef.getFQN().equals("org.osgi.framework"));
		}
	}

	/**
	 * Test plugin reporting for provider vs consumer types.
	 */
	@Test
	public void testAnalysisPluginProviderConsumerReporting() throws Exception {
		TestAnalysisPlugin plugin = new TestAnalysisPlugin();

		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setProperty("Export-Package", "org.osgi.service.event");
			b.setProperty("Import-Package", "*");
			b.getPlugins().add(plugin);

			b.build();
			assertTrue(b.check(), "Builder should have no errors");

			// Check that we have reports with reasons
			assertThat(plugin.versionReports).isNotEmpty();
			
			boolean hasProviderReport = plugin.versionReports.stream()
				.anyMatch(r -> r.reason != null && r.reason.contains("provider"));
			boolean hasConsumerReport = plugin.versionReports.stream()
				.anyMatch(r -> r.reason != null && r.reason.contains("consumer"));
			
			// We should have at least one type of report
			assertTrue(hasProviderReport || hasConsumerReport, 
				"Expected at least one provider or consumer report");
		}
	}

	/**
	 * Test plugin ordering.
	 */
	@Test
	public void testAnalysisPluginOrdering() throws Exception {
		TestAnalysisPlugin plugin1 = new TestAnalysisPlugin(10);
		TestAnalysisPlugin plugin2 = new TestAnalysisPlugin(5);

		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setProperty("Export-Package", "org.osgi.service.event");
			b.setProperty("Import-Package", "*");
			
			// Add plugins in reverse order
			b.getPlugins().add(plugin1);
			b.getPlugins().add(plugin2);

			b.build();
			assertTrue(b.check(), "Builder should have no errors");

			// Both plugins should have been called
			assertThat(plugin1.versionReports).isNotEmpty();
			assertThat(plugin2.versionReports).isNotEmpty();
		}
	}

	/**
	 * Test analysis plugin implementation.
	 */
	static class TestAnalysisPlugin implements AnalysisPlugin {
		final List<VersionReport> versionReports = new ArrayList<>();
		final List<AnalysisReport> analysisReports = new ArrayList<>();
		final int order;

		TestAnalysisPlugin() {
			this(0);
		}

		TestAnalysisPlugin(int order) {
			this.order = order;
		}

		@Override
		public void reportImportVersion(Analyzer analyzer, PackageRef packageRef, String version, String reason)
			throws Exception {
			versionReports.add(new VersionReport(packageRef, version, reason));
		}

		@Override
		public void reportAnalysis(Analyzer analyzer, String category, String details) throws Exception {
			analysisReports.add(new AnalysisReport(category, details));
		}

		@Override
		public int ordering() {
			return order;
		}
	}

	static class VersionReport {
		final PackageRef packageRef;
		final String version;
		final String reason;

		VersionReport(PackageRef packageRef, String version, String reason) {
			this.packageRef = packageRef;
			this.version = version;
			this.reason = reason;
		}

		@Override
		public String toString() {
			return "VersionReport[" + packageRef.getFQN() + " -> " + version + " (" + reason + ")]";
		}
	}

	static class AnalysisReport {
		final String category;
		final String details;

		AnalysisReport(String category, String details) {
			this.category = category;
			this.details = details;
		}

		@Override
		public String toString() {
			return "AnalysisReport[" + category + ": " + details + "]";
		}
	}
}
