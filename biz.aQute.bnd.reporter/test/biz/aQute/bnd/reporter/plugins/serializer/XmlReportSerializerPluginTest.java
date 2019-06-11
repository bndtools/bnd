package biz.aQute.bnd.reporter.plugins.serializer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class XmlReportSerializerPluginTest {

	@Test
	public void testXmlSerialization() throws Exception {
		final Map<String, Object> toSerialize = new HashMap<>();
		toSerialize.put("test", Boolean.TRUE);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new XmlReportSerializerPlugin().serialize(toSerialize, out);
		assertTrue(out.size() > 0);
		out = new ByteArrayOutputStream();
		new XmlReportSerializerPlugin().serialize(new HashMap<>(), out);
		assertTrue(out.size() > 0);
		assertArrayEquals(new String[] {
			"xml"
		}, new XmlReportSerializerPlugin().getHandledExtensions());
	}
}
