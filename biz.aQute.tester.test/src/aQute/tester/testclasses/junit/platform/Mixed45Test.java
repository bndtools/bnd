package aQute.tester.testclasses.junit.platform;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

// This test class is not supposed to be run directly; see readme.md for more info.
public class Mixed45Test {
	public static Set<String> methods = new HashSet<>();

	@Test
	public void junit4Test() {
		methods.add("junit4Test");
	}

	@org.junit.jupiter.api.Test
	public void junit5Test() {
		methods.add("junit5Test");
	}
}
