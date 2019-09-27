package test.component;

import java.io.File;
import java.util.jar.Manifest;

import org.osgi.service.component.annotations.Component;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;
import junit.framework.TestCase;

/**
 * The Service-Component header is cleaned up when it contains wildcards. The
 * wildcards are matched against the actual file paths. If they overlap, the
 * actual file paths are removed. If the Service-Component header is not set
 * then the names are not touched.
 */
public class WildcardComponents extends TestCase {
	@Component
	static class WildcardTestComponent {}

	/**
	 * Test to see if we ignore scala.ScalaObject as interface
	 *
	 * @throws Exception
	 */
	public void testWildcardSpecMatchingOldStyleComponents() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/com.test.scala.jar"));
			b.setProperty("Service-Component", "OSGI-INF/*.xml,not.matching.path.xml,com.test.scala.Service");
			b.setIncludeResource("not.matching.path.xml;literal=''");
			Jar jar = b.build();
			assertTrue(b.check());

			Manifest m = jar.getManifest();
			String value = m.getMainAttributes()
				.getValue("Service-Component");
			assertEquals("OSGI-INF/*.xml,not.matching.path.xml", value);
		}
	}

	public void testAnnotationsAndNoHeader() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.*WildcardTestComponent");
			b.setProperty("Private-Package", "test.component");
			b.addClasspath(new File("bin_test"));

			Jar jar = b.build();
			assertTrue(b.check());

			String value = jar.getManifest()
				.getMainAttributes()
				.getValue("Service-Component");
			assertEquals("OSGI-INF/test.component.WildcardComponents$WildcardTestComponent.xml", value);
		}
	}

	public void testWildcardWithAnnotations() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty(Constants.DSANNOTATIONS, "test.component.*WildcardTestComponent");
			b.setProperty("Private-Package", "test.component");
			b.setProperty(Constants.SERVICE_COMPONENT, "OSGI-INF/*.xml");
			b.addClasspath(new File("bin_test"));

			Jar jar = b.build();
			assertTrue(b.check());

			String value = jar.getManifest()
				.getMainAttributes()
				.getValue("Service-Component");
			assertEquals("OSGI-INF/*.xml", value);
		}
	}

	public void testWildcardNotMatching() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/com.test.scala.jar"));
			b.setProperty("Service-Component", "BLABLA/*.xml,foo.bar.xml,com.test.scala.Service");
			b.setIncludeResource("foo.bar.xml;literal=''");
			Jar jar = b.build();
			assertTrue(b.check());

			Manifest m = jar.getManifest();
			String value = m.getMainAttributes()
				.getValue("Service-Component");
			assertEquals("BLABLA/*.xml,foo.bar.xml,OSGI-INF/com.test.scala.Service.xml", value);
		}
	}

}
