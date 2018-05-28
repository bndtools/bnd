package biz.aQute.bnd.reporter.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import java.util.Map;
import java.util.jar.Manifest;
import org.junit.Test;

public class ManifestEntryPluginTest {
	
	@Test
	public void testManifestEntryPlugin() {
		final ManifestEntryPlugin plugin = new ManifestEntryPlugin();
		Jar jar = new Jar("jar");
		Manifest manifest = new Manifest();
		jar.setManifest(manifest);
		manifest.getMainAttributes().putValue("Bundle-Name", "test");
		Processor p = new Processor();
		Object result;
		result = plugin.extract(jar, "", p);
		
		assertTrue(p.isOk());
		assertTrue(((Map<?, ?>) result).size() > 0);
		
		jar = new Jar("jar");
		p = new Processor();
		result = plugin.extract(jar, "", p);
		
		assertTrue(p.isOk());
		assertNull(result);
		
		jar = new Jar("jar");
		manifest = new Manifest();
		jar.setManifest(manifest);
		p = new Processor();
		result = plugin.extract(jar, "", p);
		
		assertTrue(p.isOk());
		assertEquals("manifest", plugin.getEntryName());
	}
}
