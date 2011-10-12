package test;

import java.util.*;

import junit.framework.*;
import aQute.libg.reporter.*;

public class BndTestCase extends TestCase {

	void assertOk(Reporter reporter) {
		try {
			assertEquals(0, reporter.getErrors().size());
			assertEquals(0, reporter.getWarnings().size());
		} catch (AssertionFailedError t) {
			print("Errors", reporter.getErrors());
			print("Warnings", reporter.getWarnings());
			throw t;
		}
	}


	private void print(String title, List<?> strings) {
		System.out.println("-------------------------------------------------------------------------");
		System.out.println(title + " " + strings.size());
		System.out.println("-------------------------------------------------------------------------");
		for ( Object s : strings) {
			System.out.println(s);
		}
	}
}
