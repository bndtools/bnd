package bndtools.repository.orbit;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class OrbitGetMapParserTest extends TestCase {

    public void testParse() throws IOException {
        CloseCheckingInputStream stream = new CloseCheckingInputStream(getClass().getResourceAsStream("orbitBundles_small.map.txt"));

        OrbitGetMapParser parser = new OrbitGetMapParser(stream);
        List<Map<String, String>> result = parser.parse();

        assertTrue("stream should be closed", stream.isClosed());
        assertNotNull("result should be non-null", result);
        assertEquals(5, result.size());

        Map<String, String> entry;

        entry = result.get(0);
        assertEquals("ch.ethz.iks.slp", entry.get("bsn"));
        assertEquals("1.0.0", entry.get("version"));
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/ch.ethz.iks.slp_1.0.0.RC5_v20080820-1500.jar", entry.get("url"));

        entry = result.get(1);
        assertEquals("com.ibm.icu", entry.get("bsn"));
        assertEquals("3.6.1", entry.get("version"));
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_3.6.1.v20080530.jar", entry.get("url"));

        entry = result.get(2);
        assertEquals("com.ibm.icu", entry.get("bsn"));
        assertEquals("3.6.0", entry.get("version"));
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_3.6.0.v20080530.jar", entry.get("url"));

        entry = result.get(3);
        assertEquals("com.ibm.icu", entry.get("bsn"));
        assertEquals("4.0.1", entry.get("version"));
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.1.v20090822.jar", entry.get("url"));

        entry = result.get(4);
        assertEquals("com.ibm.icu", entry.get("bsn"));
        assertEquals("4.0.0", entry.get("version"));
        assertEquals("http://download.eclipse.org/tools/orbit/downloads/drops/R20100114021427/bundles/com.ibm.icu_4.0.0.v20081201.jar", entry.get("url"));
    }

    public void testParseFull() throws IOException {
        CloseCheckingInputStream stream = new CloseCheckingInputStream(getClass().getResourceAsStream("orbitBundles.map.txt"));

        OrbitGetMapParser parser = new OrbitGetMapParser(stream);
        List<Map<String, String>> result = parser.parse();

        assertTrue("stream should be closed", stream.isClosed());
        assertNotNull("result should be non-null", result);
        assertEquals(306, result.size());
    }
}
