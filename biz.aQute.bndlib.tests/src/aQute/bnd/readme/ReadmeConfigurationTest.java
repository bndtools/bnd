package aQute.bnd.readme;

import junit.framework.TestCase;

public class ReadmeConfigurationTest extends TestCase {

	public void testEmptyConfiguration() {

		ReadmeConfiguration option = ReadmeConfiguration.builder().build();

		assertTrue(option.showContacts());
		assertTrue(option.showCopyright());
		assertTrue(option.showDescription());
		assertTrue(option.showLicense());
		assertTrue(option.showComponents());
		assertTrue(option.showVendor());
		assertTrue(option.showVersion());
		assertEquals("", option.contactsMessage());
		assertEquals("", option.componentsMessage());
		assertEquals(1, option.componentsIndex());
	}

	public void testDontshowConfiguration() {

		ReadmeConfiguration option = ReadmeConfiguration.builder().showDefault(false).build();

		assertFalse(option.showContacts());
		assertFalse(option.showCopyright());
		assertFalse(option.showDescription());
		assertFalse(option.showLicense());
		assertFalse(option.showComponents());
		assertFalse(option.showVendor());
		assertFalse(option.showVersion());
		assertEquals("", option.contactsMessage());
		assertEquals("", option.componentsMessage());
		assertEquals(1, option.componentsIndex());
	}

	public void testConfiguration() {

		ReadmeConfiguration option = ReadmeConfiguration.builder()
				.showDefault(false)
				.showDescription(true)
				.showLicense(true)
				.contactsMessage(null)
				.componentsMessage("test")
				.componentsIndex(5)
				.build();

		assertFalse(option.showContacts());
		assertFalse(option.showCopyright());
		assertTrue(option.showDescription());
		assertTrue(option.showLicense());
		assertFalse(option.showComponents());
		assertFalse(option.showVendor());
		assertFalse(option.showVersion());
		assertEquals("", option.contactsMessage());
		assertEquals("test", option.componentsMessage());
		assertEquals(5, option.componentsIndex());
	}

	public void testNegativeAndZeroIndex() {

		ReadmeConfiguration option = ReadmeConfiguration.builder().componentsIndex(-5).build();

		assertEquals(0, option.componentsIndex());

		option = ReadmeConfiguration.builder().componentsIndex(0).build();

		assertEquals(0, option.componentsIndex());
	}
}
