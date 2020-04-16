package biz.aQute.bnd.reporter.plugins.transformer;

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.bnd.reporter.plugins.serializer.JsonReportSerializerPlugin;
import junit.framework.TestCase;

public class JtwigTransformerPluginTest extends TestCase {

	public void testJtwigTransformer() throws Exception {

		final JtwigTransformerPlugin t = new JtwigTransformerPlugin();
		final Map<String, Object> report = new HashMap<>();
		final Map<String, String> parameters = new HashMap<>();
		report.put("test", "test");
		parameters.put("param1", "param");

		final ByteArrayOutputStream model = new ByteArrayOutputStream();
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

	public void testJtwigTransformerDefault() throws Exception {
		final JtwigTransformerPlugin t = new JtwigTransformerPlugin();

		final Map<String, Object> report = new HashMap<>();
		final Map<String, Object> manifest = new HashMap<>();
		final Map<String, String> bundleSymbolicName = new HashMap<>();

		bundleSymbolicName.put("symbolicName", "test");
		manifest.put("bundleSymbolicName", bundleSymbolicName);
		report.put("manifest", manifest);

		final ByteArrayOutputStream model = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(report, model);

		final ByteArrayOutputStream output = new ByteArrayOutputStream();

		t.transform(IO.stream(model.toByteArray()),
			JtwigTransformerPlugin.class.getResourceAsStream("templates/readme.twig"),
			output,
			new HashMap<String, String>());

		assertTrue(new String(output.toByteArray()).contains("# test"));
	}

	public void testTaggesFunction() throws Exception {

		assertTag(execTemplateTaggesFunction(null), "abbaaaa");
		assertTag(execTemplateTaggesFunction(Arrays.asList("")), "abbaaaa");
		assertTag(execTemplateTaggesFunction(Arrays.asList(".*")), "abbaaaa");
		assertTag(execTemplateTaggesFunction(Arrays.asList("*")), "abbaaaa");
		assertTag(execTemplateTaggesFunction(Arrays.asList("default")), "abbbaaa");
		assertTag(execTemplateTaggesFunction(Arrays.asList("default", "other")), "abbbaaa");
		assertTag(execTemplateTaggesFunction(Arrays.asList("osgi-spec")), "babaaaa");
		assertTag(execTemplateTaggesFunction(Arrays.asList("default", "osgi-spec")), "abbaaaa");

	}

	private void assertTag(List<String> actual, String expected) {
		// System.out.println(actual
		// .stream()
		// .collect(Collectors.joining("")));
		assertEquals(expected, actual.stream()
			.collect(Collectors.joining("")));

	}

	private List<String> execTemplateTaggesFunction(List<String> tags) throws Exception {

		final Map<String, Object> report = new HashMap<>();
		final Map<String, String> parameters = new TreeMap<>();

		parameters.put(JtwigTransformerPlugin.BND_REPORTER_MD_TAGS, Strings.join(",", tags));
		try (final ByteArrayOutputStream model = new ByteArrayOutputStream()) {
			new JsonReportSerializerPlugin().serialize(report, model);

			final ByteArrayOutputStream output = new ByteArrayOutputStream();

			final JtwigTransformerPlugin t = new JtwigTransformerPlugin();
			t.transform(IO.stream(model.toByteArray()),
				IO.stream(new File("testresources/transformer/jtwigShowSection.twig")), output, parameters);

			BufferedReader bfReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(output.toByteArray())));

			return bfReader.lines()
				.collect(Collectors.toList());
		}
	}
}
