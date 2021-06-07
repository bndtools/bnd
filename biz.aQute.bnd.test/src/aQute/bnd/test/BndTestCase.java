package aQute.bnd.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.opentest4j.AssertionFailedError;

import aQute.service.reporter.Reporter;

public class BndTestCase {

	public static void assertOk(Reporter reporter) {
		assertOk(reporter, 0, 0);
	}

	public static void assertOk(Reporter reporter, int errors, int warnings) {
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
