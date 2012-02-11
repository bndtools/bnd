package org.example.tests.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

}
