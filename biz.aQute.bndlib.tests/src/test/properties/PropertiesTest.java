package test.properties;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.model.*;
import aQute.bnd.properties.*;
import aQute.lib.io.*;

public class PropertiesTest extends TestCase {

	public static void testBndEditModel() throws Exception {
		
		Document doc = new Document("Bundle-Description:\tTest\n" +
									"Bundle-SymbolicName:\ttest.properties\n" +
									"Private-Package:\tpp1\n");
		
		
		BndEditModel model = new BndEditModel();

		model.loadFrom(doc);
		
		model.addPrivatePackage("pp2");
		model.addPrivatePackage("pp3");
		
		model.setBundleVersion("1.0.0");
		model.setBundleVersion("1.1.0");
		
		model.saveChangesTo(doc);
		
		File file = File.createTempFile("test", ".properties");
		IO.store(doc.get(), file, "ISO-8859-1");

		model = new BndEditModel();
		model.loadFrom(file);
		
		Properties props = new Properties();
		props.load(new FileInputStream(file));
		
		
		assertEquals(props.getProperty("Bundle-Version"), model.getBundleVersionString());
	
		List<String> privatePackages = model.getPrivatePackages();
		
		String s = props.getProperty("Private-Package");
		String[] pkgs = s.split("\\,");
		for (String pkg : pkgs) {
			assertTrue(privatePackages.remove(pkg));
		}
		assertEquals(0, privatePackages.size());
	}
}
