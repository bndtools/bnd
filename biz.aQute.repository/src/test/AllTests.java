package test;

import test.http.CachingURLResourceHandlerTest;
import test.http.HttpConnectorTest;
import test.obr.OBRTest;
import test.repository.TestCompressedObrRepo;
import test.repository.TestFixedIndexedRepo;
import test.repository.TestLocalIndexGeneration;
import test.repository.TestLocalObrGeneration;
import test.repository.TestObrRepo;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(CachingURLResourceHandlerTest.class);
		suite.addTestSuite(HttpConnectorTest.class);
		suite.addTestSuite(OBRTest.class);
		suite.addTestSuite(TestFixedIndexedRepo.class);
		suite.addTestSuite(TestCompressedObrRepo.class);
		suite.addTestSuite(TestLocalIndexGeneration.class);
		suite.addTestSuite(TestLocalObrGeneration.class);
		suite.addTestSuite(TestObrRepo.class);
		//$JUnit-END$
		return suite;
	}

}
