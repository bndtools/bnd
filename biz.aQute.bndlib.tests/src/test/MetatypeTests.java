package test;

import junit.framework.*;

public class MetatypeTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(MetatypeTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(test.metatype.MetatypeTest.class);
		//$JUnit-END$
		return suite;
	}

}
