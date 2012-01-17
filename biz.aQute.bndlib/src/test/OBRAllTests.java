package test;

import junit.framework.*;
import test.lib.deployer.obr.*;

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
