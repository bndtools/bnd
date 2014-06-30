package org.foo.zar;

import static org.junit.Assert.*;

import org.junit.Test;
import org.osgi.framework.BundleActivator;

public class ZarTest {
	@Test
	public void testZar() {
		Zar z = new Zar();
		assertTrue(z instanceof BundleActivator);
	}
}
