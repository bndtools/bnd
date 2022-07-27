package biz.aQute.bnd.reporter.plugins.entries.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import biz.aQute.bnd.reporter.plugins.entries.any.ImportResourcePlugin;
import biz.aQute.bnd.reporter.plugins.resource.converter.PropertiesConverterPlugin;

public class ImportJarResourcePluginTest {

	@Test
	public void testImportJarResourcePath() throws Exception {
		try (final Jar jar = new Jar("jar"); final Processor p = new Processor();) {
			final ImportJarResourcePlugin plugin = new ImportJarResourcePlugin();

			p.addBasicPlugin(new PropertiesConverterPlugin());
			plugin.setReporter(p);
			plugin.setRegistry(p);

			jar.putResource("myDir/file.cool", new EmbeddedResource("test=test", 0L));

			final Map<String, String> prop = new HashMap<>();
			prop.put(ImportJarResourcePlugin.PATH_PROPERTY, "myDir/file.cool");
			prop.put(ImportResourcePlugin.TYPE_PROPERTY, "properties");
			plugin.setProperties(prop);

			assertEquals("file", plugin.getProperties()
				.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY));
			assertNotNull(plugin.extract(jar, Locale.forLanguageTag("und")));
			assertTrue(p.isOk());
		}
	}
}
