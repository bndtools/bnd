package biz.aQute.bnd.reporter.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;
import java.util.jar.Manifest;

import org.junit.Test;

import aQute.bnd.osgi.Jar;

public class ManifestHelperTest {

	@Test
	public void testWithoutManifest() {
		assertNull(ManifestHelper.createIfPresent(new Jar("dot"), Locale.forLanguageTag("und")));
	}

	@Test
	public void testWithoutLocalization() {
		final Jar jar = new Jar("jar");
		final Manifest manifest = new Manifest();

		manifest.getMainAttributes()
			.putValue("Bundle-Description", "desUN");

		jar.setManifest(manifest);
		final ManifestHelper h = ManifestHelper.createIfPresent(jar, Locale.forLanguageTag("und"));

		assertEquals("desUN", h.getHeader("Bundle-Description", false)
			.keySet()
			.iterator()
			.next());
	}

	@Test
	public void testWithLocalizationDefault() {
		final Jar jar = new Jar("jar");
		final Manifest manifest = new Manifest();

		manifest.getMainAttributes()
			.putValue("Bundle-Description", "%des");
		manifest.getMainAttributes()
			.putValue("Bundle-Test", "test");

		PropResource r = new PropResource();
		r.add("des", "valueUN");
		jar.putResource("OSGI-INF/l10n/bundle.properties", r);

		r = new PropResource();
		r.add("des", "valueEN");
		jar.putResource("OSGI-INF/l10n/bundle_en.properties", r);

		jar.setManifest(manifest);
		final ManifestHelper h = ManifestHelper.createIfPresent(jar, Locale.forLanguageTag("und"));

		manifest.getMainAttributes()
			.putValue("Bundle-Localization", "");

		final ManifestHelper hen = ManifestHelper.createIfPresent(jar, Locale.forLanguageTag("en"));

		assertEquals("valueUN", h.getHeader("Bundle-Description", false)
			.keySet()
			.iterator()
			.next());
		assertEquals("test", h.getHeader("Bundle-Test", false)
			.keySet()
			.iterator()
			.next());
		assertEquals("valueEN", hen.getHeader("Bundle-Description", false)
			.keySet()
			.iterator()
			.next());
		assertEquals("test", hen.getHeader("Bundle-Test", false)
			.keySet()
			.iterator()
			.next());
		assertEquals(0, hen.getHeader("Notfound", false)
			.keySet()
			.size());
		assertEquals("", hen.getHeaderAsString("Notfound"));
		assertEquals("test", hen.getHeaderAsString("Bundle-Test"));
	}

	@Test
	public void testWithLocalizationCustom() {
		final Jar jar = new Jar("jar");
		final Manifest manifest = new Manifest();

		manifest.getMainAttributes()
			.putValue("Bundle-Description", "%des");
		manifest.getMainAttributes()
			.putValue("Bundle-Test", "test");
		manifest.getMainAttributes()
			.putValue("Bundle-Localization", "bundle");

		PropResource r = new PropResource();
		r.add("des", "valueUN");
		jar.putResource("bundle.properties", r);

		r = new PropResource();
		r.add("des", "valueEN");
		jar.putResource("bundle_en.properties", r);

		jar.setManifest(manifest);
		final ManifestHelper h = ManifestHelper.createIfPresent(jar, Locale.forLanguageTag("und"));
		final ManifestHelper hen = ManifestHelper.createIfPresent(jar, Locale.forLanguageTag("en"));

		assertEquals("valueUN", h.getHeader("Bundle-Description", false)
			.keySet()
			.iterator()
			.next());
		assertEquals("test", h.getHeader("Bundle-Test", false)
			.keySet()
			.iterator()
			.next());
		assertEquals("valueEN", hen.getHeader("Bundle-Description", false)
			.keySet()
			.iterator()
			.next());
		assertEquals("test", hen.getHeader("Bundle-Test", false)
			.keySet()
			.iterator()
			.next());
	}
}
