package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;

@SuppressWarnings("resource")
public class ExportHeaderTest extends TestCase {

	/**
	 * If you import a range then the maven guys can have the silly -SNAPSHOT in
	 * the version. This tests if ranges are correcly cleaned up.
	 * 
	 * @throws Exception
	 */
	public static void testImportHeaderWithMessedUpRange() throws Exception {
		Builder builder = new Builder();
		Jar bin = new Jar(new File("bin"));
		builder.setClasspath(new Jar[] {
			bin
		});
		Properties p = new Properties();
		p.setProperty(Constants.PRIVATE_PACKAGE, "test.packageinfo.ref");
		p.setProperty(Constants.IMPORT_PACKAGE, "test.packageinfo;version=\"[1.1.1-SNAPSHOT,1.1.1-SNAPSHOT]");
		builder.setProperties(p);
		Jar jar = builder.build();
		Manifest manifest = jar.getManifest();

		String imph = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		assertEquals("test.packageinfo;version=\"[1.1.1.SNAPSHOT,1.1.1.SNAPSHOT]\"", imph);
	}

	public static void testPickupExportVersion() throws Exception {
		Builder builder = new Builder();
		Jar bin = new Jar(new File("bin"));
		builder.setClasspath(new Jar[] {
			bin
		});
		Properties p = new Properties();
		p.setProperty(Constants.PRIVATE_PACKAGE, "test.packageinfo.ref");
		builder.setProperties(p);
		Jar jar = builder.build();
		Manifest manifest = jar.getManifest();

		String imph = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		assertEquals("test.packageinfo;version=\"[1.0,2)\"", imph);
	}

	public static void testExportVersionWithPackageInfo() throws Exception {
		Builder builder = new Builder();
		Jar bin = new Jar(new File("bin"));
		builder.setClasspath(new Jar[] {
			bin
		});
		Properties p = new Properties();
		p.setProperty(Constants.EXPORT_PACKAGE, "test.packageinfo");
		builder.setProperties(p);

		Jar jar = builder.build();
		Manifest manifest = jar.getManifest();

		String exph = manifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
		Map<String,String> exports = OSGiHeader.parseHeader(exph).get("test.packageinfo");
		assertEquals("1.0.0.SNAPSHOT", exports.get(Constants.VERSION_ATTRIBUTE));
		assertEquals("1.2.3", exports.get("Implementation-Version"));
		assertEquals("Best Snapshot in the world", exports.get("Implementation-Title"));
	}
}
