package org.foo.zar;

import org.osgi.framework.BundleActivator;

import junit.framework.TestCase;

public class ZarTest extends TestCase {
	public void testZar() {
		Zar z = new Zar();
		assertTrue(z instanceof BundleActivator);
	}
}
