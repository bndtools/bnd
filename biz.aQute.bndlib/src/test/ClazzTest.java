package test;

import junit.framework.*;
import aQute.lib.osgi.*;

public class ClazzTest extends TestCase {

	
	
	/**
	 * Test the uncamel
	 */
	
	public void testUncamel() throws Exception {
		assertEquals("New", Clazz.unCamel("_new"));
		assertEquals("An XMLMessage", Clazz.unCamel("anXMLMessage"));
		assertEquals("A message", Clazz.unCamel("aMessage"));
		assertEquals("URL", Clazz.unCamel("URL"));
		assertEquals("A nice party", Clazz.unCamel("aNiceParty"));
	}
}
