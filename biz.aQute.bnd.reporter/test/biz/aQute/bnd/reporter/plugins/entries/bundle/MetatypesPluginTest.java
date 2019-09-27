package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import biz.aQute.bnd.reporter.plugins.serializer.JsonReportSerializerPlugin;
import junit.framework.TestCase;

public class MetatypesPluginTest extends TestCase {

	public void testMetatype() throws Exception {

		final Jar jar = new Jar("jar", "testresources/metatypesEntry/source.jar");

		final Processor p = new Processor();
		final MetatypesPlugin e = new MetatypesPlugin();
		e.setReporter(p);
		final Map<String, Object> result = new HashMap<>();

		result.put(e.getProperties()
			.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY), e.extract(jar, Locale.forLanguageTag("und")));

		assertTrue(p.isOk());

		final ByteArrayOutputStream s = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(result, s);

		final StringBuffer ee = new StringBuffer();

		for (final String l : Files.readAllLines(Paths.get("testresources/metatypesEntry/result.json"),
			StandardCharsets.UTF_8)) {

			ee.append(l + "\n");
		}
		ee.deleteCharAt(ee.length() - 1);
		assertEquals(ee.toString(), new String(s.toByteArray()));
	}
}
