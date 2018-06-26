package aQute.bnd.osgi;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JarTest {

	@Test
	public void testMove() {
		try (Jar jar = new Jar("foo")) {
			Resource r = new EmbeddedResource(new byte[0], 0);
			jar.putResource("OSGI-INF/foo.xml", r);
			jar.move("OSGI-INF/*.xml", "src/main/resources/OSGI-INF/");

			assertTrue(jar.getResources().containsKey("src/main/resources/OSGI-INF/foo.xml"));
		}
	}
}
