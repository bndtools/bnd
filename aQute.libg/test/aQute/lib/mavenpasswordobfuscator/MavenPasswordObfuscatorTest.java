package aQute.lib.mavenpasswordobfuscator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MavenPasswordObfuscatorTest {

	@Test
	public void testAgainstMavenEncryptedPassword() throws Exception {
		String decrypt64 = MavenPasswordObfuscator.decrypt("{YJSiC4rcl1cH2hcwL20EL8wlQwkB2zGvdIGq8x8pX7k=}",
			"settings.security");
		assertEquals("foobar", decrypt64);

	}
}
