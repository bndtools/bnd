package aQute.bnd.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import junit.framework.TestCase;

public class ReadmeGenerationTest extends TestCase {

	public void testReadmeGeneration() throws Exception {

		testReadme("path='generated/readmeForGeneration.md'", "My Bundle Name",
				"My Bundle Description", "My Bundle Copyright", "My Bundle Vendor http://vendor.org/", "1.0.0",
				"Apache-2.0;description='Apache License, Version 2.0';link='http://www.apache.org/licenses/LICENSE-2.0'",
				" myid;email=xxx@yyy.zz; name='Name Name';roles='architect, developer',myid2;name='Name2 LastName2';roles='architect, developer'");
	}

	void testReadme(String readmeOption, String bundleName, String bundleDescription,
			String bundleCopyright, String bundleVendor, String bundleVersion, String bundleLicence,
			String bundleContacts)
			throws IOException, SAXException, ParserConfigurationException, Exception {

		Builder b = new Builder();

		b.setProperty("-readme", readmeOption);
		b.setProperty(Constants.BUNDLE_NAME, bundleName);
		b.setProperty(Constants.BUNDLE_DESCRIPTION, bundleDescription);
		b.setProperty(Constants.BUNDLE_COPYRIGHT, bundleCopyright);
		b.setProperty(Constants.BUNDLE_VENDOR, bundleVendor);
		b.setProperty(Constants.BUNDLE_VERSION, bundleVersion);
		b.setProperty(Constants.BUNDLE_LICENSE, bundleLicence);
		b.setProperty(Constants.BUNDLE_DEVELOPERS, bundleContacts);

		Jar jar = b.build();

		Resource r = jar.getResource("readmeForGeneration.md");

		assertNotNull(r);
		assertEquals(new String(Files.readAllBytes(Paths.get("testresources/readme/readmeForGeneration.md"))),
				new String(Files.readAllBytes(Paths.get("generated/readmeForGeneration.md"))));

		b.close();
	}

	public void testReadmeFromJar() throws Exception {

		Jar jar = new Jar("com.service.provider.jar", "testresources/readme/com.service.provider.jar");

		Builder b = new Builder();
		b.setJar(jar);

		b.setProperty("-readme", "path='generated/readmeForGenerationFromJar.md'");
		b.setProperty("Service-Component",
				"OSGI-INF/com.service.provider.Provider1.xml,OSGI-INF/com.service.provider2.xml,OSGI-INF/com.service.provider3.xml");

		b.build();

		assertEquals(new String(Files.readAllBytes(Paths.get("testresources/readme/readmeForGenerationFromJar.md"))),
				new String(Files.readAllBytes(Paths.get("generated/readmeForGenerationFromJar.md"))));
		
		b.close();
	}
}
