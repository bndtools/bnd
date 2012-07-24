package test;

import java.io.*;

import junit.framework.*;
import aQute.lib.io.*;
import aQute.libg.gzip.*;

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
