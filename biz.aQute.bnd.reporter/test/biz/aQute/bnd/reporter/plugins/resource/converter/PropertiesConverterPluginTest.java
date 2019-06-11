package biz.aQute.bnd.reporter.plugins.resource.converter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class PropertiesConverterPluginTest {

	@Test
	public void testPropertiesConvertion() throws Exception {
		String prop = null;
		Object entry = null;
		Map<String, Object> expected = null;

		prop = "";
		entry = new PropertiesConverterPlugin().extract(new ByteArrayInputStream(prop.getBytes()));
		assertEquals(expected, entry);

		expected = new LinkedHashMap<>();
		expected.put("test", "");
		prop = "test";
		entry = new PropertiesConverterPlugin().extract(new ByteArrayInputStream(prop.getBytes()));
		assertEquals(expected, entry);

		expected = new LinkedHashMap<>();
		expected.put("test", "value");
		prop = "test=value";
		entry = new PropertiesConverterPlugin().extract(new ByteArrayInputStream(prop.getBytes()));
		assertEquals(expected, entry);

		expected = new LinkedHashMap<>();
		expected.put("test", "value");
		expected.put("test2", "value2");
		prop = "test=value\ntest2=value2";
		entry = new PropertiesConverterPlugin().extract(new ByteArrayInputStream(prop.getBytes()));
		assertEquals(expected, entry);

		assertArrayEquals(new String[] {
			"properties"
		}, new PropertiesConverterPlugin().getHandledExtensions());
	}
}
