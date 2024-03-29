package biz.aQute.bnd.reporter.plugins.entries.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Locale;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import biz.aQute.bnd.reporter.manifest.dto.OSGiHeadersDTO;

public class ManifestPluginTest {

	@Test
	public void testManifestEntryPlugin() throws IOException {
		final ManifestPlugin plugin = new ManifestPlugin();
		OSGiHeadersDTO result;
		try (Jar jar = new Jar("jar"); Processor p = new Processor();) {

			Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			manifest.getMainAttributes()
				.putValue("Bundle-Name", "test");

			plugin.setReporter(p);
			result = plugin.extract(jar, Locale.forLanguageTag("und"));

			assertTrue(p.isOk());
			assertNotNull(result);
			assertEquals("test", result.bundleName);
		}

		try (Jar jar = new Jar("jar"); Processor p = new Processor();) {
			plugin.setReporter(p);
			result = plugin.extract(jar, Locale.forLanguageTag("und"));

			assertTrue(p.isOk());
			assertEquals(null, result);
		}

		try (Jar jar = new Jar("jar"); Processor p = new Processor();) {
			Manifest manifest = new Manifest();
			jar.setManifest(manifest);
			plugin.setReporter(p);
			result = plugin.extract(jar, Locale.forLanguageTag("und"));

			assertTrue(p.isOk());
			assertEquals("manifest", plugin.getProperties()
				.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY));
		}
	}
}
