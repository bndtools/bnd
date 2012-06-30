package org.osgi.service.indexer.impl;

import java.util.Properties;

import junit.framework.TestCase;

public class TestMacro extends TestCase {

	public void testSimpleProperty() {
		Properties props = new Properties();
		props.setProperty("foo", "bar");
		
		assertEquals("bar", Util.readProcessedProperty(props, "foo"));
	}
	
	public void testMacroProperty() {
		Properties props = new Properties();
		props.setProperty("foo", "bar means ${bar}!");
		props.setProperty("bar", "big armies rippled");
		
		assertEquals("bar means big armies rippled!", Util.readProcessedProperty(props, "foo"));
	}
}
