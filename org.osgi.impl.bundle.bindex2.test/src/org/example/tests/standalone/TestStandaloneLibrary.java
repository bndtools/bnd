package org.example.tests.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.Collections;

import org.example.tests.utils.Utils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.bindex.ResourceIndexer;
import org.osgi.service.bindex.impl.BIndex2;

import junit.framework.TestCase;

public class TestStandaloneLibrary extends TestCase {

	public void testBasicServiceInvocation() throws Exception {
		ResourceIndexer indexer = new BIndex2();
		
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/01-bsn+version.jar")), writer, null);
		
		assertEquals(Utils.readStream(new FileInputStream("testdata/fragment-basic.txt")), writer.toString().trim());
	}

}
