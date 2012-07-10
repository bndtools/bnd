package org.osgi.service.indexer.impl;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

public class TestUtils extends TestCase {
	
	public void testFindPlainPath() throws Exception {
		JarResource jar = new JarResource(new File("testdata/org.eclipse.osgi_3.7.2.v20120110-1415.jar"));
		List<String> list = Util.findMatchingPaths(jar, "META-INF/services/org.osgi.framework.launch.FrameworkFactory");
		assertEquals(1, list.size());
		assertEquals("META-INF/services/org.osgi.framework.launch.FrameworkFactory", list.get(0));
	}

	public void testFindGlobPattern() throws Exception {
		JarResource jar = new JarResource(new File("testdata/org.eclipse.osgi_3.7.2.v20120110-1415.jar"));
		List<String> list = Util.findMatchingPaths(jar, "*.profile");
		
		assertEquals(12, list.size());
		assertEquals("CDC-1.0_Foundation-1.0.profile", list.get(0));
	}
}
