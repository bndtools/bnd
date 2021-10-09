package biz.aQute.bnd.reporter.plugins.resource.converter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

public class JsonConverterPluginTest {

	@Test
	public void testJsonConvertion() throws Exception {
		final String json = "\"test\"";

		final Object entry = new JsonConverterPlugin().extract(new ByteArrayInputStream(json.getBytes()));

		assertEquals("test", entry);
		assertArrayEquals(new String[] {
			"json"
		}, new JsonConverterPlugin().getHandledExtensions());
	}
}
