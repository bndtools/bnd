package test.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.properties.Document;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class PropertiesTest extends TestCase {

	public static void testBndEditModel() throws Exception {

		Document doc = new Document("# Hello\nBundle-Description:\tTest \u2649\n"
			+ "\n\nBundle-SymbolicName:\ttest.properties\n" + "Private-Package:\tpp1\n");

		BndEditModel model = new BndEditModel();

		model.loadFrom(doc);

		model.addPrivatePackage("pp2");
		model.addPrivatePackage("pp3");

		model.setBundleVersion("1.0.0");
		model.setBundleVersion("1.1.0");

		System.out.println(doc.get());

		File file = File.createTempFile("test", ".properties");
		IO.copy(model.toAsciiStream(doc), file);

		model = new BndEditModel();
		model.loadFrom(file);

		Properties props = new Properties();
		try (InputStream in = new FileInputStream(file)) {
			props.load(in);
		}

		assertEquals(props.getProperty("Bundle-Version"), model.getBundleVersionString());

		List<String> privatePackages = model.getPrivatePackages();

		String s = props.getProperty("Private-Package");
		String[] pkgs = s.split("\\,");
		for (String pkg : pkgs) {
			assertTrue(privatePackages.remove(pkg));
		}
		assertEquals(0, privatePackages.size());

		String desc = props.getProperty("Bundle-Description");
		assertEquals(desc, "Test \u2649");
	}
}
