package org.osgi.service.bindex.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.osgi.service.bindex.Resource;

public class TestJarResource extends TestCase {

	public void testJarName() throws Exception {
		JarResource resource = new JarResource(new File("testdata/org.example.a.jar"));
		assertEquals("testdata/org.example.a.jar", resource.getLocation());
	}

	public void testJarSize() throws Exception {
		JarResource resource = new JarResource(new File("testdata/org.example.a.jar"));
		assertEquals(1104L, resource.getSize());
	}
	
	public void testJarListing() throws Exception {
		JarResource resource = new JarResource(new File("testdata/org.example.a.jar"));
		List<String> children = resource.listChildren("org/example/a/");
		assertEquals(2, children.size());
		assertEquals("A.class", children.get(0));
		assertEquals("packageinfo", children.get(1));
	}
	
	public void testJarListingInvalidPaths() throws Exception {
		JarResource resource = new JarResource(new File("testdata/org.example.a.jar"));
		assertNull(resource.listChildren("org/wibble/"));
		assertNull(resource.listChildren("org/example/a"));
	}
	
	public void testJarFileContent() throws Exception {
		JarResource resource = new JarResource(new File("testdata/org.example.a.jar"));
		Resource pkgInfoResource = resource.getChild("org/example/a/packageinfo");
		
		assertEquals("version 1.0", readStream(pkgInfoResource.getStream()));
	}
	
	public void testJarManifest() throws Exception {
		JarResource resource = new JarResource(new File("testdata/org.example.a.jar"));
		Manifest manifest = resource.getManifest();
		assertEquals("org.example.a", manifest.getMainAttributes().getValue("Bundle-SymbolicName"));
	}
	
	static final String readStream(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream);
		StringBuilder result = new StringBuilder();
		
		char[] buf = new char[1024];
		int charsRead = reader.read(buf, 0, buf.length);
		while (charsRead > -1) {
			result.append(buf, 0, charsRead);
			charsRead = reader.read(buf, 0, buf.length);
		}
		
		return result.toString();
	}
}
