package test.lib.deployer.obr;

import java.util.*;
import java.util.concurrent.atomic.*;

import javax.xml.parsers.*;

import junit.framework.*;
import aQute.lib.deployer.obr.*;

public class OBRParseTest extends TestCase {
	
	
	public void testParseOBR() throws Exception {
		final List<Resource> resources = new ArrayList<Resource>();
		IRepositoryListener listener = new IRepositoryListener() {
			public boolean processResource(Resource resource) {
				resources.add(resource);
				return true;
			}

			public boolean processReferral(String fromUrl, Referral referral, int maxDepth, int currentDepth) {
				fail("Method processReferral should not be called from this test!");
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
	
	public void testParseOBRReferral() throws Exception {

		final SAXParserFactory parserFactory = SAXParserFactory.newInstance();

		final List<Resource> resources = new ArrayList<Resource>();
		IRepositoryListener listener = new IRepositoryListener() {
			public boolean processResource(Resource resource) {
				resources.add(resource);
				return true;
			}

			public boolean processReferral(String fromUrl, Referral referral, int maxDepth, int currentDepth) {
				if ("file:sample/testobr.xml".equals(referral.getUrl())) {
					try {
						SAXParser parser = parserFactory.newSAXParser();
						parser.parse(OBRParseTest.class.getResourceAsStream("testobr.xml"),
								new OBRSAXHandler("file:sample/testobr.xml", this, maxDepth, currentDepth));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				return true;
			}
		};
		
		SAXParser parser = parserFactory.newSAXParser();

		parser.parse(OBRParseTest.class.getResourceAsStream("referralobr.xml"),
				new OBRSAXHandler("file:sample/referral.xml", listener));

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
		IRepositoryListener listener = new IRepositoryListener() {
			public boolean processResource(Resource resource) {
				counter.incrementAndGet();
				return false;
			}

			public boolean processReferral(String fromUrl, Referral referral, int maxDepth, int currentDepth) {
				fail("Method processReferral should not be called from this test!");
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
