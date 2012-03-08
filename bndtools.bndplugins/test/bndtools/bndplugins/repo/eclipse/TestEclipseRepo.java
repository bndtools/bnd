package bndtools.bndplugins.repo.eclipse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.libg.version.Version;

import junit.framework.TestCase;

public class TestEclipseRepo extends TestCase {
	
	public void testEclipseRepo() {
		EclipseRepo repo = new EclipseRepo();
		
		Map<String, String> props = new HashMap<String, String>();
		props.put("name", "Eclipse Test 1");
		props.put("location", System.getProperty("user.dir") + "/testdata/eclipse1");
		repo.setProperties(props);
		
		List<String> bsnList = repo.list(null);
		assertEquals(5, bsnList.size());
		assertEquals("org.eclipse.sdk", bsnList.get(0)); // directory based
		assertEquals("javax.servlet", bsnList.get(1)); // JAR based
	}
	
	public void testInvalidShapeRepo() {
		EclipseRepo repo = new EclipseRepo();
		
		Map<String, String> props = new HashMap<String, String>();
		props.put("name", "Eclipse Test 2");
		props.put("location", System.getProperty("user.dir") + "/testdata/eclipse2");
		try {
			repo.setProperties(props);
			fail("Should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testObscuredJarName() {
		EclipseRepo repo = new EclipseRepo();
		
		Map<String, String> props = new HashMap<String, String>();
		props.put("name", "Eclipse Test 3");
		props.put("location", System.getProperty("user.dir") + "/testdata/eclipse3");
		repo.setProperties(props);
		
		List<String> bsnList = repo.list(null);
		assertEquals(1, bsnList.size());
		assertEquals("javax.servlet", bsnList.get(0));
		
		List<Version> versions = repo.versions("javax.servlet");
		assertEquals(1, versions.size());
		assertEquals(new Version("2.5.0.v200806031605"), versions.get(0));
	}

}
