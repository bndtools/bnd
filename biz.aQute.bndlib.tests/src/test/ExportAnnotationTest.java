package test;

import java.io.File;
import java.util.Properties;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class ExportAnnotationTest extends TestCase {

	public static void testStandardAnnotation() throws Exception {
		Builder builder = new Builder();
		Jar bin = new Jar(new File("bin"));
		builder.setClasspath(new Jar[] {
				bin
		});
		Properties p = new Properties();
		p.setProperty("Private-Package", "test.export.annotation");
		builder.setProperties(p);
		Jar jar = builder.build();
		Manifest manifest = jar.getManifest();

		String imph = manifest.getMainAttributes().getValue("Export-Package");
		assertEquals("test.export.annotation;fizz=buzz;foobar:=fizzbuzz;uses:=\"foo,bar\";version=\"1.0.0\"", imph);
	}
}
