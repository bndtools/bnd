package test;

import test.lib.deployer.obr.*;
import junit.framework.Test;
import junit.framework.TestSuite;

public class OBRAllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(OBRAllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(OBRTest.class);
		suite.addTestSuite(OBRParseTest.class);
		//$JUnit-END$
		return suite;
	}

}
