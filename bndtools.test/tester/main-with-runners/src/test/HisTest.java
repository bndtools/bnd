package test;

import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import junit.framework.AssertionFailedError;

public class HisTest {

	@Test
	public void test1() {}
	
	@Test
	public void test2() {}
	
	@Test
	public void test3() {
		throw new AssertionFailedError();
	}
	
	@Test
	public void test4() {
		throw new AssertionError();
	}
	
	@Test
	public void test6() {
		throw new RuntimeException();
	}
	
	@org.junit.jupiter.api.Test
	public void test7() {
		throw new RuntimeException();
	}
	
	@DisplayName("This is test5/αυτή είναι η δοκιμή5")
	@org.junit.jupiter.api.Test
	public void test5() {
		throw new org.opentest4j.AssertionFailedError("method","expected","αλειθινο");
	}
	
	@ParameterizedTest(name = "param: {0} : {index} of ")
	@ValueSource(strings = { "racecar", "radar", "able was I ere I saw elba" })
	void palindromes(String candidate) {
	    if (candidate.equals("radar")) {
	    	throw new org.opentest4j.AssertionFailedError("Ddin't match!", candidate, "radar");
	    }
	}

	@ParameterizedTest(name = "{index} ==> param(παράμ): ''{0}''")
	@ValueSource(strings = { "1", "δύο", "3", "4", "5" })
	@DisplayName("Dummy test")
	void dummy(String candidate) {
	}

	@Disabled("this is disabled/δεν δουλεύει αυτό")
	@org.junit.jupiter.api.Test
	public void disabledTest() {
		throw new AssertionError();
	}
	
	@Disabled("this is disabled too")
	public static class NestedTest {
		@org.junit.jupiter.api.Test
		public void disabledTest1() {
			throw new AssertionError();
		}
		@org.junit.jupiter.api.Test
		public void disabledTest2() {
			throw new AssertionError();
		}
	}
}
