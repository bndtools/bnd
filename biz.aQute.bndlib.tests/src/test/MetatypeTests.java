package test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class MetatypeTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(MetatypeTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(test.metatype.BNDMetatypeTest.class);
		// $JUnit-END$
		return suite;
	}

}
