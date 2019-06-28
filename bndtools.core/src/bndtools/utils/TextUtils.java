package bndtools.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

public class TextUtils {

	public static void indentText(String initialIndent, String innerIndent, Reader input, StringBuffer result)
		throws IOException {
		BufferedReader reader = (input instanceof BufferedReader) ? (BufferedReader) input : new BufferedReader(input);
		String line = reader.readLine();
		while (line != null) {
			line = initialIndent + line.replaceAll("\t", innerIndent);
			result.append(line);

			line = reader.readLine();
			if (line != null)
				result.append('\n');
		}
	}

	public static String generateIndent(int spaces) {
		char[] indentArray = new char[spaces];
		Arrays.fill(indentArray, ' ');
		String indentStr = new String(indentArray);
		return indentStr;
	}
}
