package bndtools;

import bndtools.DataURLStreamHandler.DataURLConnection;
import junit.framework.TestCase;

public class DataURLStreamHandlerTest extends TestCase {

    public void testDecodeBase64() throws Exception {
        String sample = "Blah blah blah blah blah blah blah blah blah blah blah";
        String encodedSample = "QmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFo";
        String ssp = "image/gif;base64," + encodedSample;

        byte[] bytes = DataURLConnection.parse(ssp).data;
        String result = new String(bytes, "UTF-8");
        assertEquals(sample, result);
    }

    public void testDecodeOctets() throws Exception {
        byte[] bytes = DataURLConnection.parse(",A%20brief%20note").data;
        assertEquals("A brief note", new String(bytes, "UTF-8"));
    }
}
