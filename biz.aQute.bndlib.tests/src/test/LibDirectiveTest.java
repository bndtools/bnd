package test;

import java.util.jar.Manifest;

import org.osgi.framework.Constants;

import aQute.bnd.osgi.Builder;
import junit.framework.TestCase;

public class LibDirectiveTest extends TestCase {

	public void testLibDirective() throws Exception {
		Builder b = new Builder();
		try {
			b.set("Bundle-ClassPath", ".");
			b.set("-includeresource", "lib/=jar/asm.jar;lib:=true");
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
			b.set("-includeresource", "lib/=jar/asm.jar;lib:=true");
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