package biz.aQute.bnd.reporter.plugins.entries.any;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.plugins.resource.converter.PropertiesConverterPlugin;

public class ImportResourcePluginTest {
	@InjectTemporaryDirectory
	File tmp;
	@Test
	public void testImportFilePath() throws Exception {
		final Processor p = new Processor();
		final ImportResourcePlugin plugin = new ImportResourcePlugin();

		p.addBasicPlugin(new PropertiesConverterPlugin());
		plugin.setReporter(p);
		plugin.setRegistry(p);

		final File f = createTempFile();

		final Map<String, String> prop = new HashMap<>();
		prop.put(ImportResourcePlugin.URL_PROPERTY, f.getPath());
		plugin.setProperties(prop);

		assertEquals("prop", plugin.getProperties()
			.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY));
		assertNotNull(plugin.extract(new Object(), Locale.forLanguageTag("und")));
		assertTrue(p.isOk());
	}

	@Test
	public void testImportFileURL() throws Exception {
		final Processor p = new Processor();
		final ImportResourcePlugin plugin = new ImportResourcePlugin();

		p.addBasicPlugin(new PropertiesConverterPlugin());
		plugin.setReporter(p);
		plugin.setRegistry(p);

		final Map<String, String> prop = new HashMap<>();
		prop.put(ImportResourcePlugin.URL_PROPERTY,
			"https://raw.githubusercontent.com/bndtools/bnd/master/cnf/build.bnd");
		prop.put(ImportResourcePlugin.TYPE_PROPERTY, "properties");
		plugin.setProperties(prop);

		assertEquals("build", plugin.getProperties()
			.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY));
		assertNotNull(plugin.extract(new Object(), Locale.forLanguageTag("und")));
		assertTrue(p.isOk());
	}

	private File createTempFile() throws Exception {
		final File file = new File(tmp, "prop.properties");
		file.createNewFile();

		IO.store("test=testValue", file);

		return file;
	}
}
