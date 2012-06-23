package org.osgi.service.indexer.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class Utils {
	public static final String readStream(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream);
		StringBuilder result = new StringBuilder();
		
		char[] buf = new char[1024];
		int charsRead = reader.read(buf, 0, buf.length);
		while (charsRead > -1) {
			result.append(buf, 0, charsRead);
			charsRead = reader.read(buf, 0, buf.length);
		}
		
		return result.toString();
	}
	
	public static final String decompress(InputStream compressedStream) throws IOException {
		GZIPInputStream decompressedStream = new GZIPInputStream(compressedStream);
		return readStream(decompressedStream);
	}

	public static String decompress(String string) throws IOException {
		return decompress(new ByteArrayInputStream(string.getBytes()));
	}

}
