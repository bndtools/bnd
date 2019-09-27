package biz.aQute.bnd.reporter.plugins.resource.converter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import aQute.lib.io.IO;
import biz.aQute.bnd.reporter.plugins.serializer.JsonReportSerializerPlugin;

public class XmlConverterPluginTest {

	@Test
	public void testXmlConvertion() throws Exception {
		String xml = null;
		Object entry = null;
		Map<String, Object> objectExpected = null;
		final Object expected = null;
		List<Object> values = null;

		xml = "<rootInFile><!-- comment --></rootInFile>";
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(expected, entry);

		xml = "<rootInFile></rootInFile>";
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(expected, entry);

		xml = "<rootInFile>justAValue</rootInFile>";
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
		assertEquals("justAValue", entry);

		xml = "<rootInFile><oneElement></oneElement></rootInFile>";
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(expected, entry);

		objectExpected = new LinkedHashMap<>();
		objectExpected.put("oneElement", "oneValue");
		xml = "<rootInFile><oneElement>oneValue</oneElement></rootInFile>";
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(objectExpected, entry);

		objectExpected = new LinkedHashMap<>();
		values = new LinkedList<>();
		values.add("oneValue");
		values.add("otherValue");
		objectExpected.put("oneElement", values);
		xml = "<rootInFile><oneElement>oneValue</oneElement><oneElement>otherValue</oneElement></rootInFile>";
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(objectExpected, entry);

		objectExpected = new LinkedHashMap<>();
		values = new LinkedList<>();
		values.add("oneValue");
		values.add("otherValue");
		objectExpected.put("oneElement", values);
		objectExpected.put("oneElement", values);
		objectExpected.put("_text", "just a text");
		xml = "<rootInFile><oneElement>oneValue</oneElement>just a text<oneElement>otherValue</oneElement></rootInFile>";
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
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
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
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
		entry = new XmlConverterPlugin().extract(new ByteArrayInputStream(xml.getBytes()));
		assertEquals(objectExpected, entry);

		entry = new XmlConverterPlugin().extract(IO.stream(Paths.get("testresources/xmlConverter/complex.xml")));

		final Map<String, Object> toSerialize = new HashMap<>();
		toSerialize.put("root", entry);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(toSerialize, out);
		assertEquals(new String(IO.read(IO.stream(Paths.get("testresources/xmlConverter/xmlJsonResult.json")))),
			new String(out.toByteArray()));
		assertArrayEquals(new String[] {
			"xml"
		}, new XmlConverterPlugin().getHandledExtensions());
	}
}
