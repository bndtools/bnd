package test;

import java.io.File;
import java.util.Properties;
import java.util.jar.Manifest;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class ExportAnnotationTest extends TestCase {

	public void testCalculated() throws Exception {
		try (Builder builder = new Builder()) {
			Jar bin = new Jar(new File("bin"));
			builder.setClasspath(new Jar[] {
				bin
			});
			Properties p = new Properties();
			p.setProperty("Private-Package", "test.export.annotation." + getName() + "*");
			builder.setProperties(p);
			Jar jar = builder.build();
			Manifest manifest = jar.getManifest();

			String exported = manifest.getMainAttributes()
				.getValue("Export-Package");
			assertNotNull(exported);
			Parameters e = OSGiHeader.parseHeader(exported);
			Attrs a = e.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals("buzz", a.get("fizz"));
			assertEquals("fizzbuzz", a.get("foobar:"));
			assertEquals(Version.valueOf("1.2.3.qual"), Version.valueOf(a.get("viking")));
			assertEquals(Attrs.Type.VERSION, a.getType("viking"));
			assertEquals("test.export.annotation." + getName() + ".used", a.get("uses:"));
			assertEquals(Version.valueOf("1.0.0"), Version.valueOf(a.get("version")));

			String imported = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNotNull(imported);
			Parameters i = OSGiHeader.parseHeader(imported);
			a = i.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals(VersionRange.valueOf("[1.0.0,1.1.0)"), VersionRange.valueOf(a.get("version")));
		}
	}

	public void testProvider() throws Exception {
		try (Builder builder = new Builder()) {
			Jar bin = new Jar(new File("bin"));
			builder.setClasspath(new Jar[] {
				bin
			});
			Properties p = new Properties();
			p.setProperty("Private-Package", "test.export.annotation." + getName() + "*");
			builder.setProperties(p);
			Jar jar = builder.build();
			Manifest manifest = jar.getManifest();

			String exported = manifest.getMainAttributes()
				.getValue("Export-Package");
			assertNotNull(exported);
			Parameters e = OSGiHeader.parseHeader(exported);
			Attrs a = e.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals("buzz", a.get("fizz"));
			assertEquals("fizzbuzz", a.get("foobar:"));
			assertEquals("test.export.annotation." + getName() + ".used", a.get("uses:"));
			assertEquals(Version.valueOf("1.0.0"), Version.valueOf(a.get("version")));

			String imported = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNotNull(imported);
			Parameters i = OSGiHeader.parseHeader(imported);
			a = i.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals(VersionRange.valueOf("[1.0.0,1.1.0)"), VersionRange.valueOf(a.get("version")));
		}
	}

	public void testProviderType() throws Exception {
		try (Builder builder = new Builder()) {
			Jar bin = new Jar(new File("bin"));
			builder.setClasspath(new Jar[] {
				bin
			});
			Properties p = new Properties();
			p.setProperty("Private-Package", "test.export.annotation." + getName() + "*");
			builder.setProperties(p);
			Jar jar = builder.build();
			Manifest manifest = jar.getManifest();

			String exported = manifest.getMainAttributes()
				.getValue("Export-Package");
			assertNotNull(exported);
			Parameters e = OSGiHeader.parseHeader(exported);
			Attrs a = e.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals(Version.valueOf("1.0.0"), Version.valueOf(a.get("version")));

			String imported = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNotNull(imported);
			Parameters i = OSGiHeader.parseHeader(imported);
			a = i.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals(VersionRange.valueOf("[1.0.0,1.1.0)"), VersionRange.valueOf(a.get("version")));
		}
	}

	public void testConsumer() throws Exception {
		try (Builder builder = new Builder()) {
			Jar bin = new Jar(new File("bin"));
			builder.setClasspath(new Jar[] {
				bin
			});
			Properties p = new Properties();
			p.setProperty("Private-Package", "test.export.annotation." + getName() + "*");
			builder.setProperties(p);
			Jar jar = builder.build();
			Manifest manifest = jar.getManifest();

			String exported = manifest.getMainAttributes()
				.getValue("Export-Package");
			assertNotNull(exported);
			Parameters e = OSGiHeader.parseHeader(exported);
			Attrs a = e.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals("buzz", a.get("fizz"));
			assertEquals("fizzbuzz", a.get("foobar:"));
			assertEquals("test.export.annotation." + getName() + ".used", a.get("uses:"));
			assertEquals(Version.valueOf("1.0.0"), Version.valueOf(a.get("version")));

			String imported = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNotNull(imported);
			Parameters i = OSGiHeader.parseHeader(imported);
			a = i.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals(VersionRange.valueOf("[1.0.0,2.0.0)"), VersionRange.valueOf(a.get("version")));
		}
	}

	public void testNoimport() throws Exception {
		try (Builder builder = new Builder()) {
			Jar bin = new Jar(new File("bin"));
			builder.setClasspath(new Jar[] {
				bin
			});
			Properties p = new Properties();
			p.setProperty("Private-Package", "test.export.annotation." + getName() + "*");
			builder.setProperties(p);
			Jar jar = builder.build();
			Manifest manifest = jar.getManifest();

			String exported = manifest.getMainAttributes()
				.getValue("Export-Package");
			assertNotNull(exported);
			Parameters e = OSGiHeader.parseHeader(exported);
			Attrs a = e.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals("buzz", a.get("fizz"));
			assertEquals("fizzbuzz", a.get("foobar:"));
			assertEquals("test.export.annotation." + getName() + ".used", a.get("uses:"));
			assertEquals(Version.valueOf("1.0.0"), Version.valueOf(a.get("version")));

			String imported = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNotNull(imported);
			Parameters i = OSGiHeader.parseHeader(imported);
			a = i.get("test.export.annotation." + getName());
			assertNull(a);
		}
	}

	public void testUses() throws Exception {
		try (Builder builder = new Builder()) {
			Jar bin = new Jar(new File("bin"));
			builder.setClasspath(new Jar[] {
				bin
			});
			Properties p = new Properties();
			p.setProperty("Private-Package", "test.export.annotation." + getName() + "*");
			builder.setProperties(p);
			Jar jar = builder.build();
			Manifest manifest = jar.getManifest();

			String exported = manifest.getMainAttributes()
				.getValue("Export-Package");
			assertNotNull(exported);
			Parameters e = OSGiHeader.parseHeader(exported);
			Attrs a = e.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals("foo,bar", a.get("uses:"));
			assertEquals(Version.valueOf("1.0.0"), Version.valueOf(a.get("version")));

			String imported = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNotNull(imported);
			Parameters i = OSGiHeader.parseHeader(imported);
			a = i.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals(VersionRange.valueOf("[1.0.0,1.1.0)"), VersionRange.valueOf(a.get("version")));
		}
	}

	public void testNouses() throws Exception {
		try (Builder builder = new Builder()) {
			Jar bin = new Jar(new File("bin"));
			builder.setClasspath(new Jar[] {
				bin
			});
			Properties p = new Properties();
			p.setProperty("Private-Package", "test.export.annotation." + getName() + "*");
			builder.setProperties(p);
			Jar jar = builder.build();
			Manifest manifest = jar.getManifest();

			String exported = manifest.getMainAttributes()
				.getValue("Export-Package");
			assertNotNull(exported);
			Parameters e = OSGiHeader.parseHeader(exported);
			Attrs a = e.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertNull(a.get("uses:"));

			String imported = manifest.getMainAttributes()
				.getValue("Import-Package");
			assertNotNull(imported);
			Parameters i = OSGiHeader.parseHeader(imported);
			a = i.get("test.export.annotation." + getName());
			assertNotNull(a);
			assertEquals(VersionRange.valueOf("[1.0.0,1.1.0)"), VersionRange.valueOf(a.get("version")));
		}
	}
}
