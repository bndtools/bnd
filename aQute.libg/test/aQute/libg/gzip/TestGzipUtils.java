package aQute.libg.gzip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import aQute.lib.io.IO;

public class TestGzipUtils {
	@Test
	public void testUnzipped() throws Exception {
		FileInputStream fis = new FileInputStream("testresources/unzipped.dat");
		InputStream stream = GZipUtils.detectCompression(fis);
		try {
			assertEquals("A plan, a plan, a canal, Panama.", IO.collect(stream));
		} finally {
			stream.close();
			fis.close();
		}
	}

	@Test
	public void testZipped() throws Exception {
		FileInputStream fis = new FileInputStream("testresources/zipped.dat");
		InputStream stream = GZipUtils.detectCompression(fis);
		try {
			assertEquals("A plan, a plan, a canal, Panama.", IO.collect(stream));
		} finally {
			stream.close();
			fis.close();
		}
	}
}
