package biz.aQute.bnd.reporter.plugins;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.Test;

import aQute.lib.io.IO;

public class ReportCodecPluginTest {

	@Test
	public void testJsonSerialization() throws Exception {
		final Map<String, Object> toSerialize = new HashMap<>();
		toSerialize.put("test", true);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(toSerialize, out);

		assertTrue(out.size() > 0);
		assertArrayEquals(new String[] { "json" }, new JsonReportSerializerPlugin().getHandledExtensions());
	}

	@Test
	public void testXmlSerialization() throws Exception {
		final Map<String, Object> toSerialize = new HashMap<>();
		toSerialize.put("test", true);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new XmlReportSerializerPlugin().serialize(toSerialize, out);
		assertTrue(out.size() > 0);
		out = new ByteArrayOutputStream();
		new XmlReportSerializerPlugin().serialize(new HashMap<>(), out);
		assertTrue(out.size() > 0);
		assertArrayEquals(new String[] { "xml" }, new XmlReportSerializerPlugin().getHandledExtensions());
	}

	@Test
	public void testJsonDeserialization() throws Exception {
		final String json = "\"test\"";

		final Object entry = new JsonImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(json.getBytes()));

		assertEquals("test", entry);
		assertArrayEquals(new String[] { "json" }, new JsonImportDeserializerPlugin().getHandledExtensions());
	}

	@Test
	public void testPropertiesDeserialization() throws Exception {
		String prop = null;
		Object entry = null;
		Map<String, Object> expected = null;

		prop = "";
		entry = new PropertiesImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(prop.getBytes()));
		assertEquals(expected, entry);

		expected = new LinkedHashMap<>();
		expected.put("test", "");
		prop = "test";
		entry = new PropertiesImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(prop.getBytes()));
		assertEquals(expected, entry);

		expected = new LinkedHashMap<>();
		expected.put("test", "value");
		prop = "test=value";
		entry = new PropertiesImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(prop.getBytes()));
		assertEquals(expected, entry);

		expected = new LinkedHashMap<>();
		expected.put("test", "value");
		expected.put("test2", "value2");
		prop = "test=value\ntest2=value2";
		entry = new PropertiesImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(prop.getBytes()));
		assertEquals(expected, entry);

		assertArrayEquals(new String[] { "properties" },
				new PropertiesImportDeserializerPlugin().getHandledExtensions());
	}

	@Test
	public void testManifestDeserialization() throws Exception {
		ByteArrayOutputStream out;
		Manifest manifest = null;
		Object entry = null;
		final Map<String, Object> expected = null;

		manifest = new Manifest();
		out = new ByteArrayOutputStream();
		manifest.write(out);
		entry = new ManifestImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(expected, entry);

		manifest = new Manifest();
		manifest.read(IO.stream(Paths.get("testresources/MANIFEST.MF")));
		out = new ByteArrayOutputStream();
		manifest.write(out);
		entry = new ManifestImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(out.toByteArray()));

		final Map<String, Object> toSerialize = new HashMap<>();
		toSerialize.put("root", entry);
		out = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(toSerialize, out);
		assertEquals(new String(IO.read(IO.stream(Paths.get("testresources/manifestJsonResult.json")))),
				new String(out.toByteArray()));
		assertArrayEquals(new String[] { "mf" }, new ManifestImportDeserializerPlugin().getHandledExtensions());
	}

	@Test
	public void testXmlDeserialization() throws Exception {
		String xml = null;
		Object entry = null;
		Map<String, Object> objectExpected = null;
		final Object expected = null;
		List<Object> values = null;

		xml = "<rootInFile><!-- comment --></rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(expected, entry);

		xml = "<rootInFile></rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(expected, entry);

		xml = "<rootInFile>justAValue</rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals("justAValue", entry);

		xml = "<rootInFile><oneElement></oneElement></rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(expected, entry);

		objectExpected = new LinkedHashMap<>();
		objectExpected.put("oneElement", "oneValue");
		xml = "<rootInFile><oneElement>oneValue</oneElement></rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(objectExpected, entry);

		objectExpected = new LinkedHashMap<>();
		values = new LinkedList<>();
		values.add("oneValue");
		values.add("otherValue");
		objectExpected.put("oneElement", values);
		xml = "<rootInFile><oneElement>oneValue</oneElement><oneElement>otherValue</oneElement></rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(objectExpected, entry);

		objectExpected = new LinkedHashMap<>();
		values = new LinkedList<>();
		values.add("oneValue");
		values.add("otherValue");
		objectExpected.put("oneElement", values);
		objectExpected.put("oneElement", values);
		objectExpected.put("_text", "just a text");
		xml = "<rootInFile><oneElement>oneValue</oneElement>just a text<oneElement>otherValue</oneElement></rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(objectExpected, entry);

		objectExpected = new LinkedHashMap<>();
		values = new LinkedList<>();
		values.add("oneValue");
		values.add("otherValue");
		objectExpected.put("oneElement", values);
		values = new LinkedList<>();
		values.add("just a text");
		values.add("another text");
		objectExpected.put("_text", values);
		xml = "<rootInFile><oneElement>oneValue</oneElement>just a text<oneElement>otherValue</oneElement>another text</rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(objectExpected, entry);

		objectExpected = new LinkedHashMap<>();
		values = new LinkedList<>();
		values.add("oneValue");

		final Map<String, Object> other = new LinkedHashMap<>();
		final List<Object> values2 = new LinkedList<>();
		values2.add("otherValue");
		values2.add("otherValue2");
		other.put("otherElement", values2);
		other.put("yetAnotherElement", "otherValue3");
		values.add(other);
		objectExpected.put("oneElement", values);
		values = new LinkedList<>();
		values.add("just a text");
		values.add("another text");
		objectExpected.put("_text", values);
		xml = "<rootInFile><oneElement>oneValue</oneElement>just a text<oneElement><otherElement>otherValue</otherElement><otherElement>otherValue2</otherElement><yetAnotherElement>otherValue3</yetAnotherElement></oneElement>another text</rootInFile>";
		entry = new XmlImportDeserializerPlugin().deserialyze(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(objectExpected, entry);

		entry = new XmlImportDeserializerPlugin().deserialyze(IO.stream(Paths.get("testresources/complex.xml")));

		final Map<String, Object> toSerialize = new HashMap<>();
		toSerialize.put("root", entry);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(toSerialize, out);
		assertEquals(new String(IO.read(IO.stream(Paths.get("testresources/xmlJsonResult.json")))),
				new String(out.toByteArray()));
		assertArrayEquals(new String[] { "xml" }, new XmlImportDeserializerPlugin().getHandledExtensions());
	}
}
