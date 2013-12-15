package aQute.bnd.deployer.repository.aether;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.aether.artifact.Artifact;

import aQute.bnd.deployer.repository.aether.ConversionUtils;
import aQute.bnd.osgi.Jar;

public class ConversionUtilsTest extends TestCase {

	public void testGuessGroupId() throws Exception {
		Jar jar = new Jar(new File("testdata/1.jar"));
		Artifact artifact = ConversionUtils.fromBundleJar(jar);
		assertEquals("org.example", artifact.getGroupId());
		assertEquals("api", artifact.getArtifactId());
	}

	public void testBsnMappingWithGroupId() throws Exception {
		Jar jar = new Jar(new File("testdata/2.jar"));
		Artifact artifact = ConversionUtils.fromBundleJar(jar);
		assertEquals("org.bndtools", artifact.getGroupId());
		assertEquals("example.foo", artifact.getArtifactId());
	}

	public void testBsnMappingWithGroupIdNotPrefix() throws Exception {
		Jar jar = new Jar(new File("testdata/3.jar"));
		Artifact artifact = ConversionUtils.fromBundleJar(jar);
		assertEquals("com.paremus", artifact.getGroupId());
		assertEquals("org.bndtools.example.foo", artifact.getArtifactId());
	}
	
	public void testMaybeMavenCoordsToBsn() throws Exception {
		assertEquals("org.example.foo", ConversionUtils.maybeMavenCoordsToBsn("org.example.foo"));
		assertEquals("org.example.foo.bar", ConversionUtils.maybeMavenCoordsToBsn("org.example:foo.bar"));
		
		try {
			ConversionUtils.maybeMavenCoordsToBsn(":foo.bar");
			fail("Should have thrown IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
		try {
			ConversionUtils.maybeMavenCoordsToBsn("org.example:");
			fail("Should have thrown IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

}
