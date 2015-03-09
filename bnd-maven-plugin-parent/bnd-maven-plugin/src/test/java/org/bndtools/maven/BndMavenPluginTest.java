package org.bndtools.maven;

import junit.framework.TestCase;

public class BndMavenPluginTest extends TestCase {

	public void testStringReplacement() {
		String orig = "a, {local-packages}, d, e";
		
		assertEquals("a, b, c, d, e", BndMavenPlugin.replaceString(orig, "{local-packages}", "b, c"));
	}
}
