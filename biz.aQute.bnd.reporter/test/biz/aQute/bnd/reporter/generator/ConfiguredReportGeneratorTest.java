package biz.aQute.bnd.reporter.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportGeneratorService;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ManifestPlugin;
import junit.framework.TestCase;

public class ConfiguredReportGeneratorTest extends TestCase {

	public void testNoConfiguredEntryPluginWithNoDefaults() throws IOException {
		try (final Processor processor = new Processor();) {
			final ReportGeneratorService generator = getGenerator(processor);

			assertTrue(generator.generateReportOf(new String("any"))
				.isEmpty());
		}
	}

	public void testNoConfiguredEntryPluginWithDefaults() throws IOException {
		try (final Processor processor = new Processor(); final Jar jar = new Jar("jar");) {
			final ReportGeneratorService generator = getGenerator(processor, new ArrayList<>(),
				Collections.singletonList(ManifestPlugin.class.getCanonicalName()));

			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_SYMBOLICNAME, "test.test");

			final Map<String, Object> generated = generator.generateReportOf(jar);

			assertEquals(1, generated.size());
			assertTrue(generated.containsKey("manifest"));
			assertTrue(processor.isOk());
		}
	}

	public void testNoConfiguredEntryPluginWithExtendedDefaults() throws IOException {
		try (final Processor processor = new Processor();) {

			final ReportGeneratorService generator = getGenerator(processor,
				Collections.singletonList(TestEntryPlugin.class.getCanonicalName()),
				Collections.singletonList("entryTest"));

			final Map<String, Object> generated = generator.generateReportOf(new String());

			assertEquals(1, generated.size());
			assertTrue(generated.containsKey("entryTest"));
			assertTrue(processor.isOk());
		}
	}

	public void testConfiguredEntryPlugin() throws IOException {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			processor.setProperty("-reportconfig.myConfigName", "anyEntry;key=any;value=test");

			final ReportGeneratorService generator = getGenerator(processor);

			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_SYMBOLICNAME, "test.test");

			final Map<String, Object> generated = generator.generateReportOf(jar,
				"(" + ReportGeneratorConstants.CONFIG_NAME_PROPERTY + "=myConfigName)");

			assertEquals(1, generated.size());
			assertTrue(generated.containsKey("any"));

			assertTrue(processor.isOk());
		}
	}

	public void testConfiguredEntryPluginWithDuplicate() throws IOException {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {
			processor.setProperty("-reportconfig.myConfigName",
				"anyEntry;key=any;value=test,anyEntry;key=any2;value=test2");

			final ReportGeneratorService generator = getGenerator(processor);

			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_SYMBOLICNAME, "test.test");

			final Map<String, Object> generated = generator.generateReportOf(jar,
				"(" + ReportGeneratorConstants.CONFIG_NAME_PROPERTY + "=myConfigName)");

			assertEquals(2, generated.size());
			assertTrue(generated.containsKey("any"));
			assertTrue(generated.containsKey("any2"));

			assertTrue(processor.isOk());
		}
	}

	public void testConfiguredEntryPluginWithoutDefaults() throws IOException {
		try (final Jar jar = new Jar("jar"); final Processor processor = new Processor();) {

			processor.setProperty("-reportconfig.myConfigName",
				"anyEntry;key=any;value=test,anyEntry;key=any2;value=test2,clearDefaults");

			final ReportGeneratorService generator = getGenerator(processor, new ArrayList<>(),
				Collections.singletonList(ManifestPlugin.class.getCanonicalName()));

			final Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue(Constants.BUNDLE_SYMBOLICNAME, "test.test");

			final Map<String, Object> generated = generator.generateReportOf(jar,
				"(" + ReportGeneratorConstants.CONFIG_NAME_PROPERTY + "=myConfigName)");

			assertEquals(2, generated.size());
			assertTrue(generated.containsKey("any"));
			assertTrue(generated.containsKey("any2"));

			assertTrue(processor.isOk());
		}
	}

	public ReportGeneratorService getGenerator(final Processor processor) {
		return ReportGeneratorBuilder.create()
			.setProcessor(processor)
			.useCustomConfig()
			.build();
	}

	public ReportGeneratorService getGenerator(final Processor processor, final List<String> toRegister,
		final List<String> defaultPlugins) {
		final ReportGeneratorBuilder b = ReportGeneratorBuilder.create()
			.setProcessor(processor)
			.useCustomConfig();

		toRegister.forEach(p -> b.registerPlugin(p));
		defaultPlugins.forEach(p -> b.addPlugin(p));

		return b.build();
	}
}
