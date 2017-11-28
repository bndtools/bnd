package biz.aQute.bnd.reporter.plugins;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import junit.framework.TestCase;

public class ComponentsEntryPluginTest extends TestCase {

	public void testComponents() throws Exception {

		final Jar jar = new Jar("jar", "testresources/org.component.test.jar");
		final Processor p = new Processor();
		final ComponentsEntryPlugin e = new ComponentsEntryPlugin();
		final Map<String, Object> result = new HashMap<>();

		result.put(e.getEntryName(), e.extract(jar, "", p));

		assertTrue(p.isOk());

		final ByteArrayOutputStream s = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(result, s);

		final StringBuffer ee = new StringBuffer();

		for (final String l : Files.readAllLines(Paths.get("testresources/resultComponent.json"),
				StandardCharsets.UTF_8)) {

			ee.append(l + "\n");
		}
		ee.deleteCharAt(ee.length() - 1);

		assertEquals(ee.toString(), new String(s.toByteArray()));
	}
}
