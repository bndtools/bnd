package test.helpers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Sed {
	private static void processObrFileInternal(BufferedReader reader, String searchPattern, String replacementPattern,
		OutputStream out) throws Exception {
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String newline = line.replaceAll(searchPattern, replacementPattern);
				out.write(newline.getBytes());
				out.write("\n".getBytes());
			}
		} finally {
			reader.close();
			out.close();
		}
	}

	public static void file2File(String filenameIn, String searchPattern, String replacementPattern, String filenameOut)
		throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filenameIn)));
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filenameOut));

		processObrFileInternal(reader, searchPattern, replacementPattern, out);

	}

	public static void file2GzFile(String filenameIn, String searchPattern, String replacementPattern,
		String filenameOut) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filenameIn)));
		BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filenameOut)));

		processObrFileInternal(reader, searchPattern, replacementPattern, out);
	}

	public static void gzFile2GzFile(String filenameIn, String searchPattern, String replacementPattern,
		String filenameOut) throws Exception {
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(new GZIPInputStream(new FileInputStream(filenameIn))));
		BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filenameOut)));

		processObrFileInternal(reader, searchPattern, replacementPattern, out);
	}
}
