package test.properties;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.model.*;
import aQute.bnd.osgi.*;
import aQute.bnd.properties.*;
import aQute.lib.io.*;

public class PropertiesTest extends TestCase {

	public static void testBndEditModel() throws Exception {
		
		Document doc = new Document("# Hello\n" + Constants.BUNDLE_DESCRIPTION + ":\tTest \u2649\n" +
									"\n\n" + Constants.BUNDLE_SYMBOLICNAME + ":\ttest.properties\n" +
									Constants.PRIVATE_PACKAGE + ":\tpp1\n");
		
		
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
		props.load(new FileInputStream(file));
		
		
		assertEquals(props.getProperty(Constants.BUNDLE_VERSION), model.getBundleVersionString());
	
		List<String> privatePackages = model.getPrivatePackages();
		
		String s = props.getProperty(Constants.PRIVATE_PACKAGE);
		String[] pkgs = s.split("\\,");
		for (String pkg : pkgs) {
			assertTrue(privatePackages.remove(pkg));
		}
		assertEquals(0, privatePackages.size());

		String desc = props.getProperty(Constants.BUNDLE_DESCRIPTION);
		assertEquals(desc,"Test \u2649");
	}
}
