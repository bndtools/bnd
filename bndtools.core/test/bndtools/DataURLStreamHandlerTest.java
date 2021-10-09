package bndtools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import bndtools.DataURLStreamHandler.DataURLConnection;

public class DataURLStreamHandlerTest {

	@Test
	public void testDecodeBase64() throws Exception {
		String sample = "Blah blah blah blah blah blah blah blah blah blah blah";
		String encodedSample = "QmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFo";
		String ssp = "image/gif;base64," + encodedSample;

		byte[] bytes = DataURLConnection.parse(ssp).data;
		String result = new String(bytes, "UTF-8");
		assertEquals(sample, result);
	}

	@Test
	public void testDecodeOctets() throws Exception {
		byte[] bytes = DataURLConnection.parse(",A%20brief%20note").data;
		assertEquals("A brief note", new String(bytes, "UTF-8"));
	}
}
