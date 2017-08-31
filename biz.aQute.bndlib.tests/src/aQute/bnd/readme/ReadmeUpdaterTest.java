package aQute.bnd.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import aQute.bnd.readme.ReadmeInformation.ComponentInformation;
import aQute.bnd.readme.ReadmeInformation.ContactInformation;
import junit.framework.TestCase;

public class ReadmeUpdaterTest extends TestCase {

	ReadmeInformation getInfo() {

		ReadmeInformation.Builder builder = ReadmeInformation.builder();
		
		builder.setTitle("Title")
				.setDescription("A short description")
				.setCopyright("A copyright")
				.setVersion("1.0.0")
				.setVendor("Vendor", "www.vendor.org")
				.setLicense("Apache-2.0", "www.apache.com", "Do what you want!");

		
		List<ComponentInformation> components = new LinkedList<>();
		List<String> prop = new LinkedList<>();
		List<ContactInformation> contacts = new LinkedList<>();

		prop.add("prop1");
		prop.add("prop2");
		prop.add("prop3");

		builder.addComponent()
				.addPid("pid1", false, null)
				.addPid("pid1", false, prop)
				.addPid("pid1", false, null)
				.addService("com.dede.Dada", "url")
				.addService("com.dede.Dada2", "url")
				.addProperty("test.test.3", null, null, "dede")
				.addProperty("test.test", "String", "A test config", "truc")
				.addProperty("test.test2", "String", "A test config", null)
				.addProperty("test.test4", "String", null, "")
				.setName("compo1")
				.setConfigurationPolicy("REQUIRE")
				.setServiceScope("SINGLETON")
				.setFactory("")
				.isEnabled(false)
				.isImmediate(false);

		builder.addComponent()
				.setName("compo1")
				.setConfigurationPolicy("optional")
				.setServiceScope("DEFAULT")
				.setFactory("")
				.isEnabled(true)
				.isImmediate(true);
		
		prop.clear();
		prop.add("");
		prop.add("");

		builder.addComponent()
				.addPid("", false, null)
				.addPid(null, false, prop)
				.addService("", null)
				.addService(null, null)
				.addProperty(null, null, null, null)
				.setName("compo2")
				.setConfigurationPolicy("IGNORE")
				.setServiceScope("PROTOTYPE")
				.setFactory("")
				.isEnabled(false)
				.isImmediate(true);

		builder.addComponent()
				.setName("compo3")
				.setServiceScope("BUNDLE")
				.setFactory("")
				.isEnabled(true)
				.isImmediate(false);

		builder.addComponent()
				.setName("compo4")
				.setConfigurationPolicy("REQUIRE")
				.setFactory("facto")
				.isEnabled(false)
				.isImmediate(false);

		prop.clear();
		prop.add("prop1");

		builder.addComponent()
				.addPid("pid1", false, null)
				.addService("com.dede.Dada", "url")
				.addProperty("test.test.3", null, null, "dede")
				.setName("compo5")
				.setFactory("")
				.isEnabled(false)
				.isImmediate(false);

		return builder.build();
	}

	public void testEmptyGeneration() throws IOException {

		assertEquals(new String(Files.readAllBytes(Paths.get("testresources/readme/generatedReadme.md"))),
				ReadmeUpdater.updateReadme("",
						ReadmeConfiguration.builder()
								.contactsMessage("Feel free to contact us: ")
								.componentsMessage("* This is really good")
								.build(),
						getInfo()));

	}

	public void testExistingGeneration() throws IOException {

		assertEquals(new String(Files.readAllBytes(Paths.get("testresources/readme/generatedReadme2.md"))),
				ReadmeUpdater.updateReadme(
						new String(Files.readAllBytes(Paths.get("testresources/readme/readmeWith3Sections.md"))),
						ReadmeConfiguration.builder()
								.contactsMessage("Feel free to contact us: ")
								.componentsMessage("* This is really good")
								.componentsIndex(1)
								.build(),
						getInfo()));
	}

	public void testTaggedGeneration() throws IOException {

		assertEquals(new String(Files.readAllBytes(Paths.get("testresources/readme/generatedReadme3.md"))),
				ReadmeUpdater.updateReadme(
						new String(Files.readAllBytes(Paths.get("testresources/readme/generatedReadme2.md"))),
						ReadmeConfiguration.builder()
								.contactsMessage("Feel free to contact us: ")
								.componentsMessage("* This is really good")
								.componentsIndex(3)
								.build(),
						getInfo()));

	}
}
