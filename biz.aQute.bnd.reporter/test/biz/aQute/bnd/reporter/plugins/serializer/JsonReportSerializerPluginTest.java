package biz.aQute.bnd.reporter.plugins.serializer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class JsonReportSerializerPluginTest {

	@Test
	public void testJsonSerialization() throws Exception {
		final Map<String, Object> toSerialize = new HashMap<>();
		toSerialize.put("test", Boolean.TRUE);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new JsonReportSerializerPlugin().serialize(toSerialize, out);

		assertTrue(out.size() > 0);
		assertArrayEquals(new String[] {
			"json"
		}, new JsonReportSerializerPlugin().getHandledExtensions());
	}
}
