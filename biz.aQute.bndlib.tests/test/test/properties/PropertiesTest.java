package test.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.properties.Document;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class PropertiesTest {

	@Test
	public void testBndEditModel(@InjectTemporaryDirectory
	File tmp) throws Exception {

		Document doc = new Document("# Hello\nBundle-Description:\tTest \u2649\n"
			+ "\n\nBundle-SymbolicName:\ttest.properties\n" + "Private-Package:\tpp1\n" + "-privatepackage:\tppA\n");

		BndEditModel model = new BndEditModel();

		model.loadFrom(doc);

		model.addPrivatePackage("pp2");
		model.addPrivatePackage("pp3");

		model.setBundleVersion("1.0.0");
		model.setBundleVersion("1.1.0");

		System.out.println(doc.get());

		File file = File.createTempFile("test", ".properties", tmp);
		IO.copy(model.toAsciiStream(doc), file);

		model = new BndEditModel();
		model.loadFrom(file);

		Properties props = new Properties();
		try (InputStream in = new FileInputStream(file)) {
			props.load(in);
		}

		assertEquals(props.getProperty("Bundle-Version"), model.getBundleVersionString());

		assertEquals(4, model.getPrivatePackages()
			.size());

		String desc = props.getProperty("Bundle-Description");
		assertEquals(desc, "Test \u2649");

		Document doc2 = new Document("# Hello\nBundle-Description:\tTest \u2649\n"
			+ "\n\nBundle-SymbolicName:\ttest.properties\n" + "Private-Package:\tpp1\n" + "-privatepackage:\tppA\n");

		BndEditModel model2 = new BndEditModel();

		model2.loadFrom(doc2);

		List<String> newPackages = Arrays.asList("pp1", "pp3", "pp5");
		model2.setPrivatePackages(newPackages);

		assertEquals(3, model2.getPrivatePackages()
			.size());
	}
}
