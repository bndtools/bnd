package biz.aQute.bnd.reporter.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportGeneratorService;

public class BasicReportGeneratorTest {

	@Test
	public void testNoConfiguredEntryPlugin() throws IOException {
		try (final Processor processor = new Processor();) {

			final ReportGeneratorService generator = getGenerator(processor);

			assertTrue(generator.generateReportOf(new String("any"))
				.isEmpty());
		}
	}

	@Test
	public void testTypeFilter() throws IOException {
		try (final Processor processor = new Processor();) {
			final ReportGeneratorService generator = getGenerator(processor);

			processor.setProperty("-plugin.ReportConfig", "biz.aQute.bnd.reporter.plugins.entries.any.AnyEntryPlugin;"
				+ "key='test';value='valuetest'," + "biz.aQute.bnd.reporter.plugins.entries.processor.FileNamePlugin");

			final Map<String, Object> generated = generator.generateReportOf(new String("any"));

			assertEquals(1, generated.size());
			assertTrue(generated.containsKey("test"));
			assertEquals("valuetest", generated.get("test"));
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testTypeMatch() throws IOException {
		try (final Processor processor = new Processor();) {
			final ReportGeneratorService generator = getGenerator(processor);

			processor.setProperty("-plugin.ReportConfig", "biz.aQute.bnd.reporter.plugins.entries.any.AnyEntryPlugin;"
				+ "key='test';value='valuetest'," + "biz.aQute.bnd.reporter.plugins.entries.bundle.ManifestPlugin");

			final Jar jar = new Jar("jar");
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_SYMBOLICNAME, "test.test");

			final Map<String, Object> generated = generator.generateReportOf(jar);

			assertEquals(2, generated.size());
			assertTrue(generated.containsKey("test"));
			assertTrue(generated.containsKey("manifest"));
			assertEquals("valuetest", generated.get("test"));
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testFilterFilter() throws IOException {
		try (final Processor processor = new Processor();) {
			final ReportGeneratorService generator = getGenerator(processor);

			processor.setProperty("-plugin.ReportConfig", "biz.aQute.bnd.reporter.plugins.entries.any.AnyEntryPlugin;"
				+ "key='test';value='valuetest'," + "biz.aQute.bnd.reporter.plugins.entries.bundle.ManifestPlugin");

			final Jar jar = new Jar("jar");
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_SYMBOLICNAME, "test.test");

			final Map<String, Object> generated = generator.generateReportOf(jar,
				"(|(sourceClass=aQute.bnd.osgi.Jar)(scope=bundle))");

			assertEquals(1, generated.size());
			assertTrue(generated.containsKey("manifest"));
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testFilterMatch() throws IOException {
		try (final Processor processor = new Processor();) {
			final ReportGeneratorService generator = getGenerator(processor);

			processor.setProperty("-plugin.ReportConfig",
				"biz.aQute.bnd.reporter.plugins.entries.any.AnyEntryPlugin;"
					+ "key='test';value='valuetest';scope='bundle',"
					+ "biz.aQute.bnd.reporter.plugins.entries.bundle.ManifestPlugin");

			final Jar jar = new Jar("jar");
			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_SYMBOLICNAME, "test.test");

			final Map<String, Object> generated = generator.generateReportOf(jar,
				"(|(sourceClass=aQute.bnd.osgi.Jar)(scope=bundle))");

			assertEquals(2, generated.size());
			assertTrue(generated.containsKey("test"));
			assertTrue(generated.containsKey("manifest"));
			assertEquals("valuetest", generated.get("test"));
			assertTrue(processor.isOk());
		}
	}

	@Test
	public void testTypeBadConfig() throws IOException {
		try (final Processor processor = new Processor();) {
			final ReportGeneratorService generator = getGenerator(processor);

			processor.setProperty("-plugin.ReportConfig",
				"biz.aQute.bnd.reporter.plugins.entries.bundle.ManifestPlugin;" + "sourceClass=java.lang.String");

			generator.generateReportOf(new String());

			assertFalse(processor.isOk());
		}
	}

	public ReportGeneratorService getGenerator(final Processor processor) {
		return ReportGeneratorBuilder.create()
			.setProcessor(processor)
			.build();
	}
}
