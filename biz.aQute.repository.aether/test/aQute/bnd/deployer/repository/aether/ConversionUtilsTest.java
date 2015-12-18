package aQute.bnd.deployer.repository.aether;

import org.eclipse.aether.artifact.Artifact;

import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class ConversionUtilsTest extends TestCase {

	public void testGuessGroupId() throws Exception {
		Jar jar = new Jar(IO.getFile("testdata/1.jar"));
		Artifact artifact = ConversionUtils.fromBundleJar(jar);
		assertEquals("org.example", artifact.getGroupId());
		assertEquals("api", artifact.getArtifactId());
	}

	public void testBsnMappingWithGroupId() throws Exception {
		Jar jar = new Jar(IO.getFile("testdata/2.jar"));
		Artifact artifact = ConversionUtils.fromBundleJar(jar);
		assertEquals("org.bndtools", artifact.getGroupId());
		assertEquals("example.foo", artifact.getArtifactId());
	}

	public void testBsnMappingWithGroupIdNotPrefix() throws Exception {
		Jar jar = new Jar(IO.getFile("testdata/3.jar"));
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
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		try {
			ConversionUtils.maybeMavenCoordsToBsn("org.example:");
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testGroupAndArtifactForBsn() throws Exception {
		String[] coords = ConversionUtils.getGroupAndArtifactForBsn("com.example.group:example-api");
		assertEquals("com.example.group", coords[0]);
		assertEquals("example-api", coords[1]);
	}
}
