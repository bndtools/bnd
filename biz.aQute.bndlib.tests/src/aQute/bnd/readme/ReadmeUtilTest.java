package aQute.bnd.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import aQute.bnd.readme.ReadmeInformation.ComponentInformation;
import aQute.bnd.readme.ReadmeInformation.ContactInformation;
import aQute.bnd.readme.ReadmeInformation.ContentInformation;
import junit.framework.TestCase;

public class ReadmeUtilTest extends TestCase {

	final static private String	TAG_NAME	= "bnd-gen";
	final static private String	TAG_OPEN	= "<" + TAG_NAME + ">\n\n";
	final static private String	TAG_CLOSE	= "</" + TAG_NAME + ">\n\n";

	boolean goodFormat(String s) {

		return s.startsWith(TAG_OPEN) && s.endsWith(TAG_CLOSE);
	}

	public void testContactsGeneration() {


		assertTrue(goodFormat(ReadmeUtil.generateContacts(new LinkedList<ReadmeInformation.ContactInformation>(), "")));
		assertTrue(goodFormat(
				ReadmeUtil.generateContacts(new LinkedList<ReadmeInformation.ContactInformation>(), "test")));

		Collection<ContactInformation> contacts;

		contacts = ReadmeInformation.builder().addContact("bob", "bob@bob.bob", "President").build().getContacts();

		assertTrue(goodFormat(ReadmeUtil.generateContacts(contacts, "test")));

		contacts = ReadmeInformation.builder()
				.addContact(null, null, null)
				.addContact("", "", "")
				.addContact("", "bob@bob.bob", "President")
				.addContact("", null, "President")
				.addContact("bob", "http://www.domain.com", "")
				.build()
				.getContacts();

		assertTrue(goodFormat(ReadmeUtil.generateContacts(contacts, "test")));
	}

	public void testLicenseGeneration() {

		assertTrue(goodFormat(ReadmeUtil
				.generateLicense(ReadmeInformation.builder().setLicense(null, null, null).build().getLicense())));
		assertTrue(goodFormat(ReadmeUtil.generateLicense(
				ReadmeInformation.builder().setLicense("Apache", null, "test test").build().getLicense())));
		assertTrue(goodFormat(ReadmeUtil.generateLicense(ReadmeInformation.builder()
				.setLicense(null, "www.google.com", "test test")
				.build()
				.getLicense())));
		assertTrue(goodFormat(ReadmeUtil.generateLicense(ReadmeInformation.builder()
				.setLicense("Apache", "www.google.com", "test test")
				.build()
				.getLicense())));
	}

	public void testTitleDescriptionGeneration() {

		assertTrue(goodFormat(ReadmeUtil.generateTitle("")));
		assertTrue(goodFormat(ReadmeUtil.generateTitle("Cool Bundle")));
		assertTrue(goodFormat(ReadmeUtil.generateDescription("")));
		assertTrue(goodFormat(ReadmeUtil.generateDescription("This is a cool Bundle")));
		assertTrue(goodFormat(ReadmeUtil.generateTitleDescription("This is a cool Bundle", "This is a cool Bundle")));
		assertTrue(goodFormat(ReadmeUtil.generateTitleDescription("", "This is a cool Bundle")));
		assertTrue(goodFormat(ReadmeUtil.generateTitleDescription("test", "")));
		assertTrue(goodFormat(ReadmeUtil.generateTitleDescription("", "")));
	}

	public void testCopyrightGeneration() {

		assertTrue(goodFormat(ReadmeUtil.generateCopyright("")));
		assertTrue(goodFormat(ReadmeUtil.generateCopyright("hahaha !")));
	}
	
	public void testContentsGeneration() {

		List<ContentInformation> contents = ReadmeInformation.builder()
				.addContent("MyProject1", "/com/dede/", "This project is cool.")
				.addContent("MyProject2", null, "This project is cool.")
				.addContent("MyProject3", null, null)
				.addContent("", null, null)
				.addContent("", "/com/dede/", null)
				.addContent("", null, "This project is cool.")
				.build()
				.getContents();



		assertTrue(goodFormat(ReadmeUtil.generateContents(contents)));

		contents = ReadmeInformation.builder()
				.addContent("MyProject3", null, "This project is cool.")
				.build()
				.getContents();

		assertTrue(goodFormat(ReadmeUtil.generateContents(contents)));
	}

	public void testVendorVersionGeneration() {

		assertTrue(goodFormat(
				ReadmeUtil.generateVendor(ReadmeInformation.builder().setVendor("cde.org", null).build().getVendor())));
		assertTrue(goodFormat(
				ReadmeUtil.generateVendor(ReadmeInformation.builder().setVendor(null, null).build().getVendor())));
		assertTrue(goodFormat(
				ReadmeUtil.generateVendor(ReadmeInformation.builder().setVendor(null, "wou").build().getVendor())));
		assertTrue(goodFormat(ReadmeUtil.generateVersion("1.0.0")));
		assertTrue(goodFormat(ReadmeUtil.generateVersion("")));
		assertTrue(goodFormat(ReadmeUtil.generateVendorVersion(
				ReadmeInformation.builder().setVendor("cde.org", "cde.org").build().getVendor(), "1.0.0")));
		assertTrue(goodFormat(ReadmeUtil
				.generateVendorVersion(ReadmeInformation.builder().setVendor("", "cde.org").build().getVendor(), "")));
	}

	public void testServicesGeneration() {


		List<String> prop = new LinkedList<>();

		ReadmeInformation.Builder component = ReadmeInformation.builder();
		ReadmeInformation.Builder components = ReadmeInformation.builder();

		prop.add("compo.name");
		prop.add("http.port");

		component.addComponent()
				.addProperty("test.test.3", null, null, "dede")
				.addProperty("test.test", "String", "A test config", "truc")
				.addProperty("test.test2", "String", "A test config", null)
				.addProperty("test.test4", "String", null, "")
				.addPid("com.test.api.test", true, prop)
				.addService("com.test.api.test", null)
				.setName("com.component.name")
				.setConfigurationPolicy("REQUIRE")
				.setServiceScope("PROTOTYPE")
				.isEnabled(true)
				.isImmediate(false);

		components.addComponent()
				.setName("com.component.name")
				.setConfigurationPolicy("OPTIONAL")
				.isEnabled(true)
				.isImmediate(false);

		components.addComponent()
				.addProperty("test.test2", "Boolean", "A test config", "")
				.addProperty("test.test2", "String", "A test config", null)
				.addPid("com.test.api.test", true, prop)
				.addService("com.test.api.test", null)
				.addService("com.test.api.test", "www.google.com")
				.setName("com.component.name")
				.setConfigurationPolicy("IGNORE")
				.setServiceScope("DEFAULT")
				.setFactory("compofac")
				.isEnabled(true)
				.isImmediate(false);


		assertTrue(goodFormat(ReadmeUtil.generateComponents(new LinkedList<ComponentInformation>(), "")));
		assertTrue(goodFormat(ReadmeUtil.generateComponents(new LinkedList<ComponentInformation>(), "test test")));
		assertTrue(goodFormat(ReadmeUtil.generateComponents(component.build().getComponents(), "test test")));
		assertTrue(goodFormat(ReadmeUtil.generateComponents(components.build().getComponents(), "test test")));
	}

	public void testTagsRemoval() throws IOException {

		String removed = ReadmeUtil
				.removeAllTags(new String(Files.readAllBytes(Paths.get("testresources/readme/generatedReadme.md"))))
				.toString();

		assertTrue(removed.isEmpty());

		assertTrue(ReadmeUtil.removeAllTags("").toString().isEmpty());
		assertTrue(ReadmeUtil.removeAllTags("test  \n\n # Title <oth> test </oth>")
				.toString()
				.equals("test  \n\n # Title <oth> test </oth>"));
	}

	public void testIndexSearch() throws IOException {

		String readmeWithoutTitle = new String(
				Files.readAllBytes(Paths.get("testresources/readme/readmeWithoutTitle.md")));
		String readmeWithTitle = new String(Files.readAllBytes(Paths.get("testresources/readme/readmeWithTitle.md")));
		String readmeWith3Sections = new String(
				Files.readAllBytes(Paths.get("testresources/readme/readmeWith3Sections.md")));

		assertEquals(54, ReadmeUtil.findPosition(new StringBuffer(readmeWithoutTitle), 1));
		assertEquals(0, ReadmeUtil.findPosition(new StringBuffer(readmeWithTitle), 0));
		assertEquals(78, ReadmeUtil.findPosition(new StringBuffer(readmeWithTitle), 1));
		assertEquals(91, ReadmeUtil.findPosition(new StringBuffer(readmeWith3Sections), 2));
		assertEquals(108, ReadmeUtil.findPosition(new StringBuffer(readmeWith3Sections), 3));
		assertEquals(108, ReadmeUtil.findPosition(new StringBuffer(readmeWith3Sections), 4));
	}
}
