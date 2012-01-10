package test;

import junit.framework.*;
import test.component.*;

public class ComponentTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(ComponentTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(DSAnnotationTest.class);
		//$JUnit-END$
		return suite;
	}

}
