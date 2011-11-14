package test;

import test.component.*;
import test.lib.deployer.obr.*;
import junit.framework.Test;
import junit.framework.TestSuite;

public class ComponentTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(ComponentTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(DSAnnotationTest.class);
		//$JUnit-END$
		return suite;
	}

}
