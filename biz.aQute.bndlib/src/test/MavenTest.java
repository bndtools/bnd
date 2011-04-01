package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.maven.*;
import aQute.lib.osgi.*;
import aQute.libg.version.*;

public class MavenTest extends TestCase {
	Processor	processor	= new Processor();
	
	
	
	
	
	/**
	 * Test the maven repositories.
	 * @throws Exception 
	 */
	
	public void testMavenRepo() throws Exception {
		Maven maven = new Maven();
		Pom pom = maven.getPom("dom4j", "dom4j", "1.6.1");
		System.out.println( pom.getGroupId() + " = "  + pom.getArtifactId() + "-" + pom.getVersion());
		
		System.out.println( pom.getDependencies(Pom.Scope.compile));
		
		File artifact = pom.getArtifact();
		System.out.println(artifact);
	}
	
	

	/**
	 * Test the pom parser which will turn the pom into a set of properties,
	 * which will make it actually readable according to some.
	 * 
	 * @throws Exception
	 */

	public void testPomParser() throws Exception {
		PomParser parser = new PomParser();
		Properties p = parser.getProperties(new File("maven/pom.xml"));
		p.store(System.out, "testing");
		assertEquals("Apache Felix Metatype Service", p.get("pom.name"));
		assertEquals("org.apache.felix", p.get("pom.groupId")); //is from parent
		assertEquals("org.apache.felix.metatype", p.get("pom.artifactId"));
		assertEquals("bundle", p.get("pom.packaging"));
		
		Map<String,Map<String,String>> map = parser.parseHeader( p.getProperty("pom.scope.test"));
		Map<String,String> junit = map.get("junit.junit");
		assertNotNull(junit);
		assertEquals( "4.0", junit.get("version"));
		Map<String,String> easymock = map.get("org.easymock.easymock");
		assertNotNull(easymock);
		assertEquals( "2.4", easymock.get("version"));
	}
	
	// public void testDependencies() throws Exception {
	// MavenDependencyGraph graph;
	//
	// graph = new MavenDependencyGraph();
	// File home = new File( System.getProperty("user.home"));
	// File m2 = new File( home, ".m2");
	// File m2Repo = new File( m2, "repository");
	// if ( m2Repo.isDirectory())
	// graph.addRepository( m2Repo.toURI().toURL());
	//
	// graph.addRepository( new URL("http://repo1.maven.org/maven2/"));
	// graph.addRepository( new
	// URL("http://repository.springsource.com/maven/bundles/external"));
	// // graph.root.add( new File("test/poms/pom-1.xml").toURI().toURL());
	//
	// }

	public void testMaven() throws Exception {
		MavenRepository maven = new MavenRepository();
		maven.setReporter(processor);
		maven.setProperties(new HashMap<String, String>());
		maven.setRoot(processor.getFile("test/maven-repo"));

		File files[] = maven.get("activation.activation", null);
		assertNotNull(files);
		assertEquals("activation-1.0.2.jar", files[0].getName());

		files = maven.get("biz.aQute.bndlib", null);
		assertNotNull(files);
		assertEquals(5, files.length);
		assertEquals("bndlib-0.0.145.jar", files[0].getName());
		assertEquals("bndlib-0.0.255.jar", files[4].getName());

		List<String> names = maven.list(null);
		System.out.println(names);
		assertEquals(13, names.size());
		assertTrue(names.contains("biz.aQute.bndlib"));
		assertTrue(names.contains("org.apache.felix.javax.servlet"));
		assertTrue(names.contains("org.apache.felix.org.osgi.core"));

		List<Version> versions = maven.versions("org.apache.felix.javax.servlet");
		assertEquals(1, versions.size());
		versions.contains(new Version("1.0.0"));

		versions = maven.versions("biz.aQute.bndlib");
		assertEquals(5, versions.size());
		versions.contains(new Version("0.0.148"));
		versions.contains(new Version("0.0.255"));
	}

	public void testMavenBsnMapping() throws Exception {
		Processor processor = new Processor();
		processor
				.setProperty("-plugin",
						"aQute.bnd.maven.MavenGroup; groupId=org.apache.felix, aQute.bnd.maven.MavenRepository");
		MavenRepository maven = new MavenRepository();
		maven.setReporter(processor);
		Map<String, String> map = new HashMap<String, String>();
		map.put("root", "test/maven-repo");
		maven.setProperties(map);

		File files[] = maven.get("org.apache.felix.framework", null);
		assertNotNull(files);
		;
		assertEquals(1, files.length);

		files = maven.get("biz.aQute.bndlib", null);
		assertNotNull(files);
		;
		assertEquals(5, files.length);
	}
}
