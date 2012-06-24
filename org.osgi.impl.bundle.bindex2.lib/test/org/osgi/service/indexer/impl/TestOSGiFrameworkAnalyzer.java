package org.osgi.service.indexer.impl;

import static org.osgi.service.indexer.impl.Utils.*;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;

import junit.framework.TestCase;

public class TestOSGiFrameworkAnalyzer extends TestCase {

	public void testOsgiFramework() throws Exception {
		LinkedList<Capability> caps = new LinkedList<Capability>();
		LinkedList<Requirement> reqs = new LinkedList<Requirement>();
		
		JarResource input = new JarResource(new File("testdata/org.apache.felix.framework-4.0.2.jar"));
		new BundleAnalyzer().analyzeResource(input, caps, reqs);
		new OSGiFrameworkAnalyzer().analyzeResource(input, caps, reqs);
		
		List<Capability> fwkCaps = findCaps("osgi.framework", caps);
		assertNotNull(fwkCaps);
		assertEquals(1, fwkCaps.size());
		Capability fwkCap = fwkCaps.get(0);
		
		assertEquals("org.apache.felix.framework", fwkCap.getAttributes().get("osgi.framework"));
		assertEquals(new Version("4.0.2"), fwkCap.getAttributes().get("version"));
		assertEquals("org.osgi.framework.startlevel,org.osgi.framework.wiring,org.osgi.framework.hooks.bundle,org.osgi.framework.hooks.service,org.osgi.framework.hooks.resolver,org.osgi.framework.launch,org.osgi.framework,org.osgi.framework.hooks.weaving,org.osgi.service.packageadmin,org.osgi.service.url,org.osgi.service.startlevel,org.osgi.util.tracker", fwkCap.getDirectives().get("uses"));
		assertEquals(new Version("4.3.0"), fwkCap.getAttributes().get("specification-version"));
	}
	
	public void testOsgiFrameworkSpecificationVersions() throws Exception {
		LinkedList<Capability> caps;
		LinkedList<Requirement> reqs;
		
		caps = new LinkedList<Capability>();
		reqs = new LinkedList<Requirement>();
		JarResource inputResource = new JarResource(new File("testdata/org.apache.felix.framework-4.0.2.jar"));
		new BundleAnalyzer().analyzeResource(inputResource, caps, reqs);
		new OSGiFrameworkAnalyzer().analyzeResource(inputResource, caps, reqs);
		assertEquals(new Version("4.3.0"), findCaps("osgi.framework", caps).get(0).getAttributes().get("specification-version"));

		caps = new LinkedList<Capability>();
		reqs = new LinkedList<Requirement>();
		inputResource = new JarResource(new File("testdata/org.eclipse.osgi_3.7.2.v20120110-1415.jar"));
		new BundleAnalyzer().analyzeResource(inputResource, caps, reqs);
		new OSGiFrameworkAnalyzer().analyzeResource(inputResource, caps, reqs);
		assertEquals(new Version("4.3.0"), findCaps("osgi.framework", caps).get(0).getAttributes().get("specification-version"));

		caps = new LinkedList<Capability>();
		reqs = new LinkedList<Requirement>();
		inputResource = new JarResource(new File("testdata/org.apache.felix.framework-3.2.2.jar"));
		new BundleAnalyzer().analyzeResource(inputResource, caps, reqs);
		new OSGiFrameworkAnalyzer().analyzeResource(inputResource, caps, reqs);
		assertEquals(new Version("4.2.0"), findCaps("osgi.framework", caps).get(0).getAttributes().get("specification-version"));

		caps = new LinkedList<Capability>();
		reqs = new LinkedList<Requirement>();
		inputResource = new JarResource(new File("testdata/org.eclipse.osgi_3.6.2.R36x_v20110210.jar"));
		new BundleAnalyzer().analyzeResource(inputResource, caps, reqs);
		new OSGiFrameworkAnalyzer().analyzeResource(inputResource, caps, reqs);
		assertEquals(new Version("4.2.0"), findCaps("osgi.framework", caps).get(0).getAttributes().get("specification-version"));
		
	}
	
	public void testNonOsgiFramework() throws Exception {
		OSGiFrameworkAnalyzer a = new OSGiFrameworkAnalyzer();
		LinkedList<Capability> caps = new LinkedList<Capability>();
		LinkedList<Requirement> reqs = new LinkedList<Requirement>();
		
		a.analyzeResource(new JarResource(new File("testdata/03-export.jar")), caps, reqs);
		
		List<Capability> fwkCaps = findCaps("osgi.framework", caps);
		assertNotNull(fwkCaps);
		assertEquals(0, fwkCaps.size());
	}
	}
