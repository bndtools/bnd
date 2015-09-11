package aQute.libg.gzip;

import java.io.*;

import junit.framework.*;
import aQute.lib.io.*;

public class TestGzipUtils extends TestCase {
	public void testUnzipped() throws Exception {
		FileInputStream fis = new FileInputStream("testresources/unzipped.dat");
		InputStream stream = GZipUtils.detectCompression(fis);
		try {
			assertEquals("A plan, a plan, a canal, Panama.", IO.collect(stream));
		}
		finally {
			stream.close();
			fis.close();
		}
	}

	public void testZipped() throws Exception {
		FileInputStream fis = new FileInputStream("testresources/zipped.dat");
		InputStream stream = GZipUtils.detectCompression(fis);
		try {
			assertEquals("A plan, a plan, a canal, Panama.", IO.collect(stream));
		}
		finally {
			stream.close();
			fis.close();
		}
	}
}