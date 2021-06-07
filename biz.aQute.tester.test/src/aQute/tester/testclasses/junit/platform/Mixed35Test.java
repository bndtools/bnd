package aQute.tester.testclasses.junit.platform;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

// This test class is not supposed to be run directly; see readme.md for more info.
public class Mixed35Test extends TestCase {
	public static Set<String> methods = new HashSet<>();

	public void testJUnit3() {
		methods.add("testJUnit3");
	}

	@org.junit.jupiter.api.Test
	public void junit5Test() {
		methods.add("junit5Test");
	}
}
