package test;

import junit.framework.*;
import test.http.*;
import test.obr.*;
import test.r5repository.*;
import test.repository.*;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(CachingUriResourceHandlerTest.class);
		suite.addTestSuite(HttpConnectorTest.class);
		suite.addTestSuite(OBRTest.class);
		suite.addTestSuite(TestFixedIndexedRepo.class);
		suite.addTestSuite(TestLocalIndexedRepo.class);
		suite.addTestSuite(TestCompressedObrRepo.class);
		suite.addTestSuite(TestLocalIndexGeneration.class);
		suite.addTestSuite(TestLocalObrGeneration.class);
		suite.addTestSuite(TestObrRepo.class);
		suite.addTestSuite(TestMultipleLocalIndexGeneration.class);
		suite.addTestSuite(TestObrCapReqParsing.class);
		suite.addTestSuite(TestObrRecognition.class);
		suite.addTestSuite(TestR5Recognition.class);
		suite.addTestSuite(FindProvidersTest.class);
		// $JUnit-END$
		return suite;
	}

}
