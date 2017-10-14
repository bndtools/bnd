package biz.aQute.bnd.reporter.generator;

import static org.junit.Assert.*;

import java.util.jar.Manifest;

import org.junit.Test;

import aQute.bnd.osgi.Jar;

public class ManifestHelperTest {

	@Test
	public void testWithoutManifest() {
		assertNull(ManifestHelper.get(new Jar("dot"), ""));
	}

	
	@Test
	public void testWithoutLocalization() {
		Jar jar = new Jar("jar");
		Manifest manifest = new Manifest();

		manifest.getMainAttributes().putValue("Bundle-Description", "desUN");
		
		jar.setManifest(manifest);
		ManifestHelper h = ManifestHelper.get(jar, "");

		assertEquals("desUN", h.getHeader("Bundle-Description",false).keySet().iterator().next());
	}

	
	@Test
	public void testWithLocalizationDefault() {
		Jar jar = new Jar("jar");
		Manifest manifest = new Manifest();

		manifest.getMainAttributes().putValue("Bundle-Description", "%des");
		manifest.getMainAttributes().putValue("Bundle-Test", "test");
		
		PropResource r = new PropResource();
		r.add("des", "valueUN");
		jar.putResource("OSGI-INF/l10n/bundle.properties", r);
		
		r = new PropResource();
		r.add("des", "valueEN");
		jar.putResource("OSGI-INF/l10n/bundle_en.properties", r);
		
		jar.setManifest(manifest);
		ManifestHelper h = ManifestHelper.get(jar, "");
		ManifestHelper hen = ManifestHelper.get(jar, "en");

		assertEquals("valueUN", h.getHeader("Bundle-Description",false).keySet().iterator().next());
		assertEquals("test", h.getHeader("Bundle-Test",false).keySet().iterator().next());
		assertEquals("valueEN", hen.getHeader("Bundle-Description",false).keySet().iterator().next());
		assertEquals("test", hen.getHeader("Bundle-Test",false).keySet().iterator().next());
	}
	
	@Test
	public void testWithLocalizationCustom() {
		Jar jar = new Jar("jar");
		Manifest manifest = new Manifest();

		manifest.getMainAttributes().putValue("Bundle-Description", "%des");
		manifest.getMainAttributes().putValue("Bundle-Test", "test");
		manifest.getMainAttributes().putValue("Bundle-Localization", "bundle");
		
		PropResource r = new PropResource();
		r.add("des", "valueUN");
		jar.putResource("bundle.properties", r);
		
		r = new PropResource();
		r.add("des", "valueEN");
		jar.putResource("bundle_en.properties", r);
		
		jar.setManifest(manifest);
		ManifestHelper h = ManifestHelper.get(jar, "");
		ManifestHelper hen = ManifestHelper.get(jar, "en");

		assertEquals("valueUN", h.getHeader("Bundle-Description",false).keySet().iterator().next());
		assertEquals("test", h.getHeader("Bundle-Test",false).keySet().iterator().next());
		assertEquals("valueEN", hen.getHeader("Bundle-Description",false).keySet().iterator().next());
		assertEquals("test", hen.getHeader("Bundle-Test",false).keySet().iterator().next());
	}	
}
