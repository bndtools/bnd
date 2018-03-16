package test;

import java.util.jar.Manifest;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

@SuppressWarnings("resource")

public class InlineTest extends TestCase {
	public static void testSimple() throws Exception {
		Builder builder = new Builder();
		builder.setProperty("Include-Resource", "@jar/osgi.jar");
		Jar jar = builder.build();
		Manifest manifest = jar.getManifest();

		// See if the version is the default and not copied from the inline
		// bundle.
		String version = manifest.getMainAttributes()
			.getValue("Bundle-Version");
		assertEquals("0", version);

		// Check if we got some relevant directories
		assertTrue(jar.getDirectories()
			.containsKey("org/osgi/framework"));
		assertTrue(jar.getDirectories()
			.containsKey("org/osgi/util/tracker"));
	}
}
