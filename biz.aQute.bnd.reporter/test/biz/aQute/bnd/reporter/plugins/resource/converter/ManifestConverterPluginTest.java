package biz.aQute.bnd.reporter.plugins.resource.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.Test;

public class ManifestConverterPluginTest {

	@Test
	public void testManifestConvertion() throws Exception {
		ByteArrayOutputStream out;
		Manifest manifest = null;
		Object entry = null;
		final Map<String, Object> expected = null;

		manifest = new Manifest();
		out = new ByteArrayOutputStream();
		manifest.write(out);
		entry = new ManifestConverterPlugin().extract(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(expected, entry);

		manifest = new Manifest();
		manifest.getMainAttributes()
			.putValue("Bundle-SymbolicName", "test");
		manifest.getMainAttributes()
			.putValue("Export-Package", "test;version=\"1.0.0\";direc:=test");
		manifest.getMainAttributes()
			.putValue("Manifest-Version", "1.0");
		out = new ByteArrayOutputStream();
		manifest.write(out);
		entry = new ManifestConverterPlugin().extract(new ByteArrayInputStream(out.toByteArray()));

		@SuppressWarnings("unchecked")
		final Map<Object, Object> entryMap = (Map<Object, Object>) entry;
		assertEquals(3, entryMap.size());
		assertTrue(entryMap.containsKey("Bundle-SymbolicName"));
		assertTrue(entryMap.containsKey("Export-Package"));
		assertTrue(entryMap.containsKey("Manifest-Version"));
	}
}
