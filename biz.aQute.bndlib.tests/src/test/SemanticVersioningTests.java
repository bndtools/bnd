package test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class SemanticVersioningTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(SemanticVersioningTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(test.diff.DiffTest.class);
		// $JUnit-END$
		return suite;
	}

}
