package biz.aQute.bnd.reporter.plugins.entries.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import org.junit.Test;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.WriteResource;

public class MavenCoordinatePluginTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testMavenCoordinatePlugin() {
		final MavenCoordinatePlugin plugin = new MavenCoordinatePlugin();
		final Jar jar = new Jar("jar");
		final Manifest manifest = new Manifest();
		jar.setManifest(manifest);
		manifest.getMainAttributes()
			.putValue("Bundle-Name", "test");

		final Properties prop = new Properties();
		prop.setProperty("version", "1.0.0");
		prop.setProperty("groupId", "com.test");
		prop.setProperty("artifactId", "test");

		jar.putResource("META-INF/maven/pom.properties", new WriteResource() {

			@Override
			public void write(final OutputStream out) throws Exception {
				prop.store(out, null);
			}

			@Override
			public long lastModified() {
				return 0;
			}
		});

		final Processor p = new Processor();
		plugin.setReporter(p);

		final Map<String, Object> r = (Map<String, Object>) plugin.extract(jar, Locale.forLanguageTag("und"));

		assertEquals(r.get("version"), "1.0.0");
		assertEquals(r.get("groupId"), "com.test");
		assertEquals(r.get("artifactId"), "test");
		assertTrue(p.isOk());
	}
}
