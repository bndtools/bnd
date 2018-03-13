package aQute.lib.mavenpasswordobfuscator;

import junit.framework.TestCase;

public class MavenPasswordObfuscatorTest extends TestCase {

	public void testAgainstMavenEncryptedPassword() throws Exception {
		String decrypt64 = MavenPasswordObfuscator.decrypt("{YJSiC4rcl1cH2hcwL20EL8wlQwkB2zGvdIGq8x8pX7k=}",
			"settings.security");
		assertEquals("foobar", decrypt64);

	}
}
