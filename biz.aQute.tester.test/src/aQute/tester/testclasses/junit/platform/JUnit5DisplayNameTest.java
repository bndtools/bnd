package aQute.tester.testclasses.junit.platform;

import org.junit.jupiter.api.DisplayName;

// This test class is not supposed to be run directly; see readme.md for more info.
@DisplayName("JUnit 5 Display test")
public class JUnit5DisplayNameTest {
	@DisplayName("Test 1")
	@org.junit.jupiter.api.Test
	public void test1() {}

	// @DisplayName("Prüfung 2")
	@DisplayName("Pr\u00fcfung 2")
	@org.junit.jupiter.api.Test
	public void testWithNonASCII() {}

	// @DisplayName("Δοκιμή 3")
	@DisplayName("\u0394\u03bf\u03ba\u03b9\u03bc\u03ae 3")
	@org.junit.jupiter.api.Test
	public void testWithNonLatin() {}
}
