package test;

import junit.framework.*;

public class FileRepoTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(FileRepoTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(test.deployer.FileRepoTest.class);
		// $JUnit-END$
		return suite;
	}
}