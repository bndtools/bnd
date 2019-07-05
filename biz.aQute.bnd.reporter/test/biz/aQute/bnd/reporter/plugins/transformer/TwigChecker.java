package biz.aQute.bnd.reporter.plugins.transformer;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.plugins.serializer.JsonReportSerializerPlugin;

public class TwigChecker {

	private String						templatePath;
	private String						functionName;

	private File						templateFile;

	private JsonReportSerializerPlugin	serializer;
	private JtwigTransformerPlugin		transformer;

	private StringBuilder				expected	= new StringBuilder();
	private List<Object>				inputs		= new LinkedList<>();

	public TwigChecker(String templatePath, String functionName) {
		this.templatePath = templatePath;
		this.functionName = functionName;

		serializer = new JsonReportSerializerPlugin();
		transformer = new JtwigTransformerPlugin();
	}

	public TwigChecker(File templateFile) {
		this.templateFile = templateFile;

		serializer = new JsonReportSerializerPlugin();
		transformer = new JtwigTransformerPlugin();
	}

	public TwigChecker expect(String line) {
		if (expected.length() != 0) {
			expected.append("\n");
		}
		expected.append(line);
		return this;
	}

	public TwigChecker expectBlankLine() {
		expected.append("\n");
		return this;
	}

	public TwigChecker with(Object... in) {
		if (inputs != null) {
			for (Object i : in) {
				inputs.add(i);
			}
		}
		return this;
	}

	public TwigChecker check() {
		try {
			String actual = templatePath != null ? transformFunction() : transformAll();
			assertEquals(expected.toString(), actual);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		expected = new StringBuilder();
		inputs = new LinkedList<>();

		return this;
	}

	@SuppressWarnings("unchecked")
	private String transformAll() throws Exception {

		Map<String, Object> model = new HashMap<>();

		if (inputs != null && inputs.size() == 1) {
			model = (Map<String, Object>) inputs.get(0);
		}

		return transform(model, IO.stream(templateFile));
	}

	private String transformFunction() throws Exception {

		Map<String, Object> model = new HashMap<>();
		StringBuilder template = new StringBuilder();

		template.append("{%- import '" + templatePath + "' as imported -%}");
		template.append("{{ imported." + functionName + "(");

		if (inputs != null) {
			for (int i = 0; i < inputs.size(); i++) {
				if (i != 0) {
					template.append(",");
				}
				template.append("report.input" + i);
				model.put("input" + i, inputs.get(i));
			}
		}

		template.append(") }}");

		return transform(model, IO.stream(template.toString()
			.getBytes()));
	}

	private String transform(Map<String, Object> model, InputStream template) throws Exception {
		final ByteArrayOutputStream data = new ByteArrayOutputStream();
		final ByteArrayOutputStream output = new ByteArrayOutputStream();

		serializer.serialize(model, data);

		transformer.transform(IO.stream(data.toByteArray()), template, output, new HashMap<>());

		if (System.getProperty("line.separator")
			.equals("\n")) {
			return new String(output.toByteArray());
		} else {
			return new String(output.toByteArray()).replaceAll(System.getProperty("line.separator"), "\n");
		}
	}

	static public class ListBuilder extends LinkedList<Object> {

		private static final long serialVersionUID = 1L;

		public ListBuilder xadd(Object element) {
			this.add(element);
			return this;
		}
	}

	static public class MapBuilder extends LinkedHashMap<String, Object> {

		private static final long serialVersionUID = 1L;

		public MapBuilder set(String key, Object value) {
			this.put(key, value);
			return this;
		}
	}
}
