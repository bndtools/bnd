package test;

import java.util.jar.Manifest;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import junit.framework.TestCase;

public class LibDirectiveTest extends TestCase {

	public void testLibDirective() throws Exception {
		Builder b = new Builder();
		try {
			b.setProperty("Bundle-ClassPath", ".");
			b.setProperty("-includeresource", "lib/=jar/asm.jar;lib:=true");
			b.build();
			assertTrue(b.check());
			Manifest m = b.getJar()
				.getManifest();

			assertNotNull(m.getMainAttributes()
				.getValue(Constants.BUNDLE_CLASSPATH));
			assertEquals(".,lib/asm.jar", m.getMainAttributes()
				.getValue(Constants.BUNDLE_CLASSPATH));
		} finally {
			b.close();
		}
	}

	public void testLibDirectiveWithDefaultedBundleClassPath() throws Exception {
		Builder b = new Builder();
		try {
			b.setProperty("-includeresource", "lib/=jar/asm.jar;lib:=true");
			b.build();
			assertTrue(b.check());
			Manifest m = b.getJar()
				.getManifest();

			assertNotNull(m.getMainAttributes()
				.getValue(Constants.BUNDLE_CLASSPATH));
			assertEquals(".,lib/asm.jar", m.getMainAttributes()
				.getValue(Constants.BUNDLE_CLASSPATH));
		} finally {
			b.close();
		}
	}

}