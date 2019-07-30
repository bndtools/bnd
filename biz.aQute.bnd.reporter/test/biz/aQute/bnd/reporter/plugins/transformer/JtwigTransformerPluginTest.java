package biz.aQute.bnd.reporter.plugins.transformer;

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.plugins.serializer.JsonReportSerializerPlugin;
import junit.framework.TestCase;

public class JtwigTransformerPluginTest extends TestCase {

	public void testJtwigTransformer() throws Exception {

		final JtwigTransformerPlugin t = new JtwigTransformerPlugin();
		final Map<String, Object> report = new HashMap<>();
		final Map<String, String> parameters = new HashMap<>();
		report.put("test", "test");
		parameters.put("param1", "param");

		try (final ByteArrayOutputStream model = new ByteArrayOutputStream()) {
			new JsonReportSerializerPlugin().serialize(report, model);

			final ByteArrayOutputStream output = new ByteArrayOutputStream();

			t.transform(IO.stream(model.toByteArray()),
				IO.stream(new File("testresources/transformer/jtwigTransformer.twig")), output, parameters);

			assertTrue(new String(output.toByteArray()).contains("test"));

			assertArrayEquals(t.getHandledModelExtensions(), new String[] {
				"json"
			});
			assertArrayEquals(t.getHandledTemplateExtensions(), new String[] {
				"twig", "jtwig"
			});
		}
	}

	public void testJtwigTransformerDefault() throws Exception {
		final JtwigTransformerPlugin t = new JtwigTransformerPlugin();

		final Map<String, Object> report = new HashMap<>();
		final Map<String, Object> manifest = new HashMap<>();
		final Map<String, String> bundleSymbolicName = new HashMap<>();

		bundleSymbolicName.put("symbolicName", "test");
		manifest.put("bundleSymbolicName", bundleSymbolicName);
		report.put("manifest", manifest);

		try (final ByteArrayOutputStream model = new ByteArrayOutputStream()) {
			new JsonReportSerializerPlugin().serialize(report, model);

			final ByteArrayOutputStream output = new ByteArrayOutputStream();

			t.transform(IO.stream(model.toByteArray()),
				JtwigTransformerPlugin.class.getResourceAsStream("templates/readme.twig"), output,
				new HashMap<String, String>());

			assertTrue(new String(output.toByteArray()).contains("# test"));
		}
	}

	public void testJtwigTransformerShowSection() throws Exception {

		final JtwigTransformerPlugin t = new JtwigTransformerPlugin();
		final Map<String, Object> report = new HashMap<>();
		final Map<String, String> parameters = new TreeMap<>();

		parameters.put(JTwigFunctions.BND_REPORTER_SHOW_PREFIX + "optional.paramsetTrue", "true");
		parameters.put(JTwigFunctions.BND_REPORTER_SHOW_PREFIX + "optional.paramsetFalse", "false");
		parameters.put(JTwigFunctions.BND_REPORTER_SHOW_PREFIX + "default.paramsetTrue", "TRUE");
		parameters.put(JTwigFunctions.BND_REPORTER_SHOW_PREFIX + "default.paramsetFalse", "false");

		try (final ByteArrayOutputStream model = new ByteArrayOutputStream()) {
			new JsonReportSerializerPlugin().serialize(report, model);

			final ByteArrayOutputStream output = new ByteArrayOutputStream();

			t.transform(IO.stream(model.toByteArray()),
				IO.stream(new File("testresources/transformer/jtwigShowSection.twig")), output, parameters);

			BufferedReader bfReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(output.toByteArray())));
			String temp;
			int i = 0;
			while ((temp = bfReader.readLine()) != null) {
				i++;
				assertEquals(i + "ok", temp);

			}

			assertArrayEquals(t.getHandledModelExtensions(), new String[] {
				"json"
			});
			assertArrayEquals(t.getHandledTemplateExtensions(), new String[] {
				"twig", "jtwig"
			});
		}
	}
}
