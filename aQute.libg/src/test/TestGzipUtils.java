package test;

import java.io.InputStream;

import junit.framework.TestCase;
import aQute.lib.io.IO;
import aQute.libg.gzip.GZipUtils;

public class TestGzipUtils extends TestCase {
	
	public void testUnzipped() throws Exception {
		InputStream stream = GZipUtils.detectCompression(TestGzipUtils.class.getResourceAsStream("unzipped.dat"));
		assertEquals("A plan, a plan, a canal, Panama.", IO.collect(stream));
	}
	
	public void testZipped() throws Exception {
		InputStream stream = GZipUtils.detectCompression(TestGzipUtils.class.getResourceAsStream("zipped.dat"));
		assertEquals("A plan, a plan, a canal, Panama.", IO.collect(stream));
	}
	
}
