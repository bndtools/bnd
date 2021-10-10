package biz.aQute.bnd.reporter.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import biz.aQute.bnd.reporter.generator.ReportGeneratorBuilder;
import biz.aQute.bnd.reporter.generator.ReportGeneratorConstants;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ManifestPlugin;

public class ReportExporterTest {

	@Test
	public void testUnscopedAvailableReport() throws IOException {
		try (final Processor processor = new Processor();) {
			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION + ".first",
				"readme1.md;scope=scope1");
			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION + ".second",
				"readme2.md;scope=scope2");

			assertEquals(2, ReportExporterBuilder.create()
				.setProcessor(processor)
				.build()
				.getAvailableReportsOf(new Object())
				.size());
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testScopedAvailableReport() throws IOException {
		try (final Processor processor = new Processor();) {

			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION + ".first",
				"readme1.md;scope=scope1");
			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION + ".second",
				"readme2.md;scope=scope2");

			assertEquals(1, ReportExporterBuilder.create()
				.setProcessor(processor)
				.setScope("scope2")
				.build()
				.getAvailableReportsOf(new Object())
				.size());
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testBasicExport() throws Exception {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "test");

			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION, "readme.json");

			final Map<String, Resource> result = ReportExporterBuilder.create()
				.setProcessor(processor)
				.build()
				.exportReportsOf(jar);

			assertEquals(1, result.size());
			Resource resource = result.values()
				.stream()
				.findFirst()
				.get();
			assertTrue(resource.buffer()
				.array().length > 0);
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testExportWithTemplate() throws Exception {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "test");

			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION,
				"readme.md;template=testresources/exporter/template.twig");

			final Map<String, Resource> result = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(ReportGeneratorBuilder.create()
					.addPlugin(ManifestPlugin.class.getCanonicalName())
					.build())
				.build()
				.exportReportsOf(jar);

			assertEquals(1, result.size());
			assertEquals("test", new String(result.values()
				.stream()
				.findFirst()
				.get()
				.buffer()
				.array()));
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testExportWithDefaultTemplate() throws Exception {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			final Manifest manifest = new Manifest();

			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "test");

			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION,
				"readme.md;template=default:readme.twig");

			final Map<String, Resource> result = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(ReportGeneratorBuilder.create()
					.addPlugin(ManifestPlugin.class.getCanonicalName())
					.build())
				.build()
				.exportReportsOf(jar);

			assertEquals(1, result.size());
			assertTrue(new String(result.values()
				.stream()
				.findFirst()
				.get()
				.buffer()
				.array()).startsWith("# test"));
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testExportWithTemplateOverride() throws Exception {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "test");

			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION,
				"testresources/exporter/readme.md;template=testresources/exporter/template.twig");

			final Map<String, Resource> result = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(ReportGeneratorBuilder.create()
					.addPlugin(ManifestPlugin.class.getCanonicalName())
					.build())
				.build()
				.exportReportsOf(jar);

			assertEquals(1, result.size());
			assertEquals("testOverride", new String(result.values()
				.stream()
				.findFirst()
				.get()
				.buffer()
				.array()));
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testExportWithTemplateUnknowType() throws Exception {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "test");

			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION,
				"readme.md;template=testresources/exporter/template.un;templateType=twig");

			final Map<String, Resource> result = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(ReportGeneratorBuilder.create()
					.addPlugin(ManifestPlugin.class.getCanonicalName())
					.build())
				.build()
				.exportReportsOf(jar);

			assertEquals(1, result.size());
			assertEquals("test", new String(result.values()
				.stream()
				.findFirst()
				.get()
				.buffer()
				.array()));
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testExportWithTemplateParameter() throws Exception {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "test");

			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION,
				"readme.md;template=testresources/exporter/template.twig;parameters='key=value'");

			final Map<String, Resource> result = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(ReportGeneratorBuilder.create()
					.addPlugin(ManifestPlugin.class.getCanonicalName())
					.build())
				.build()
				.exportReportsOf(jar);

			assertEquals(1, result.size());
			assertEquals("testvalue", new String(result.values()
				.stream()
				.findFirst()
				.get()
				.buffer()
				.array()));
			assertTrue(processor.isOk());
		}

	}

	@Test
	public void testExportWithConfigName() throws Exception {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue("Bundle-SymbolicName", "test");

			processor.setProperty(ReportGeneratorConstants.REPORT_CONFIG_INSTRUCTION + ".api",
				"anyEntry;key=test;value=valueTest");
			processor.setProperty(ReportExporterConstants.EXPORT_REPORT_INSTRUCTION, "readme.json;configName=api");

			final Map<String, Resource> result = ReportExporterBuilder.create()
				.setProcessor(processor)
				.setGenerator(ReportGeneratorBuilder.create()
					.setProcessor(processor)
					.useCustomConfig()
					.build())
				.build()
				.exportReportsOf(jar);

			assertEquals(1, result.size());
			assertTrue(new String(result.values()
				.stream()
				.findFirst()
				.get()
				.buffer()
				.array()).contains("valueTest"));
			assertTrue(processor.isOk());
		}
	}
}
