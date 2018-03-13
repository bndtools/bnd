package aQute.bnd.test;

import java.util.List;

import aQute.service.reporter.Reporter;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public abstract class BndTestCase extends TestCase {

	protected static void assertOk(Reporter reporter) {
		assertOk(reporter, 0, 0);
	}

	protected static void assertOk(Reporter reporter, int errors, int warnings) throws AssertionFailedError {
		try {
			assertEquals(errors, reporter.getErrors()
				.size());
			assertEquals(warnings, reporter.getWarnings()
				.size());
		} catch (AssertionFailedError t) {
			print("Errors", reporter.getErrors());
			print("Warnings", reporter.getWarnings());
			throw t;
		}
	}

	private static void print(String title, List<?> strings) {
		System.err.println("-------------------------------------------------------------------------");
		System.err.println(title + " " + strings.size());
		System.err.println("-------------------------------------------------------------------------");
		for (Object s : strings) {
			System.err.println(s);
		}
	}
}
