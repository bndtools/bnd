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

public class ComponentsPluginTest extends TestCase {

	public void testComponents() throws Exception {

		final Jar jar = new Jar("jar", "testresources/componentsEntry/source.jar");
		final Processor p = new Processor();
		final ComponentsPlugin e = new ComponentsPlugin();
		final Map<String, Object> result = new HashMap<>();
		e.setReporter(p);

		result.put(e.getProperties()
			.get(ReportEntryPlugin.ENTRY_NAME_PROPERTY), e.extract(jar, Locale.forLanguageTag("und")));

		assertTrue(p.isOk());

		final ByteArrayOutputStream s = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(result, s);

		final StringBuffer ee = new StringBuffer();

		for (final String l : Files.readAllLines(Paths.get("testresources/componentsEntry/result.json"),
			StandardCharsets.UTF_8)) {

			ee.append(l + "\n");
		}
		ee.deleteCharAt(ee.length() - 1);

		assertEquals(ee.toString(), new String(s.toByteArray()));
	}
}
