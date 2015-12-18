package test;

import junit.framework.Test;
import junit.framework.TestSuite;
import test.component.BNDAnnotationTest;
import test.component.ComponentTest;
import test.component.DSAnnotationTest;

public class ComponentTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(ComponentTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(DSAnnotationTest.class);
		suite.addTestSuite(BNDAnnotationTest.class);
		suite.addTestSuite(ComponentTest.class);
		// $JUnit-END$
		return suite;
	}

}
