package org.osgi.service.indexer.impl;

import java.util.Properties;

import junit.framework.TestCase;

public class TestMacro extends TestCase {

	public void testSimpleProperty() {
		Properties props = new Properties();
		props.setProperty("foo", "bar");
		
		assertEquals("bar", Util.readProcessedProperty("foo", props));
	}
	
	public void testMacroProperty() {
		Properties props = new Properties();
		props.setProperty("gnu", "GNU is not UNIX");
		props.setProperty("message", "The meaning of GNU is \"${gnu}\".");
		
		assertEquals("The meaning of GNU is \"GNU is not UNIX\".", Util.readProcessedProperty("message", props));
	}
	
	public void testMultiLevelPropertiesMacro() {
		Properties baseProps = new Properties();
		baseProps.setProperty("gnu", "GNU is not UNIX");
		
		Properties extensionProps = new Properties();
		extensionProps.put("message", "The meaning of GNU is \"${gnu}\".");
		
		assertEquals("The meaning of GNU is \"GNU is not UNIX\".", Util.readProcessedProperty("message", extensionProps, baseProps));
	}
}
