package test.lib.deployer.obr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;
import aQute.lib.deployer.obr.Capability;
import aQute.lib.deployer.obr.IResourceListener;
import aQute.lib.deployer.obr.OBRSAXHandler;
import aQute.lib.deployer.obr.Property;
import aQute.lib.deployer.obr.Resource;
import aQute.lib.deployer.obr.StopParseException;

public class OBRParseTest extends TestCase {
	
	
	public void testParseOBR() throws Exception {
		final List<Resource> resources = new ArrayList<Resource>();
		IResourceListener listener = new IResourceListener() {
			public boolean processResource(Resource resource) {
				resources.add(resource);
				return true;
			}
		};
		
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();

		parser.parse(OBRParseTest.class.getResourceAsStream("testobr.xml"),
				new OBRSAXHandler("file:sample/testobr.xml", listener));

		assertEquals(2, resources.size());
		
		Resource firstResource = resources.get(0);
		assertEquals("org.apache.felix.shell", firstResource.getSymbolicName());
		assertEquals("file:sample/testobr.xml", firstResource.getBaseUrl());
		assertEquals(6, firstResource.getCapabilities().size());
		
		Capability lastCapability = firstResource.getCapabilities().get(5);
		assertEquals("package", lastCapability.getName());
		assertEquals(3, lastCapability.getProperties().size());
		assertEquals(new Property("package", null, "org.apache.felix.shell"), lastCapability.getProperties().get(0));
		assertEquals(new Property("uses", null, "org.osgi.framework"), lastCapability.getProperties().get(1));
		assertEquals(new Property("version", "version", "1.0.0"), lastCapability.getProperties().get(2));
	}
	
	public void testEarlyTermination() throws Exception {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		
		final AtomicInteger counter = new AtomicInteger(0);
		IResourceListener listener = new IResourceListener() {
			public boolean processResource(Resource resource) {
				counter.incrementAndGet();
				return false;
			}
		};
		
		boolean parseStopped = false;
		try {
			parser.parse(OBRParseTest.class.getResourceAsStream("unparseable.xml"), new OBRSAXHandler("", listener));
			fail("Parser not stopped");
		} catch (StopParseException e) {
			// Expected
			parseStopped = true;
		}
		
		assertEquals(1, counter.get());
		assertTrue(parseStopped);
	}
	
	
}
