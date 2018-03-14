package aQute.launcher.minifw;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.osgi.framework.Bundle;

import junit.framework.TestCase;

public class MiniFrameworkTest extends TestCase {

	public static void testSimple() throws Exception {
		Properties properties = new Properties();
		MiniFramework framework = new MiniFramework(properties);
		URL url = new File("../demo/generated/demo.jar").getCanonicalFile()
			.toURI()
			.toURL();

		url.openStream()
			.close();
		framework.init();

		Bundle b = framework.installBundle("reference:" + url.toExternalForm());
		assertNotNull(b);

		Bundle[] bundles = framework.getBundles();
		assertNotNull(bundles);
		assertEquals(2, bundles.length);

		Class<?> c = b.loadClass("test.TestActivator");
		assertNotNull(c);

	}
}
