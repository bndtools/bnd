package aQute.bnd.test;

import java.util.*;

import junit.framework.*;
import aQute.libg.reporter.*;

public abstract class BndTestCase extends TestCase {

	protected void assertOk(Reporter reporter) {
		try {
			assertEquals(0, reporter.getErrors().size());
			assertEquals(0, reporter.getWarnings().size());
		}
		catch (AssertionFailedError t) {
			print("Errors", reporter.getErrors());
			print("Warnings", reporter.getWarnings());
			throw t;
		}
	}

	private void print(String title, List< ? > strings) {
		System.err.println("-------------------------------------------------------------------------");
		System.err.println(title + " " + strings.size());
		System.err.println("-------------------------------------------------------------------------");
		for (Object s : strings) {
			System.err.println(s);
		}
	}
}
