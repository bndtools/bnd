package org.example.tests.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

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
	
	public static final void copyFully(InputStream input, OutputStream output) throws IOException {
		try {
			byte[] buf = new byte[1024];
			int bytesRead;
			while (true) {
				if ((bytesRead = input.read(buf, 0, 1024)) < 0)
					break;
				output.write(buf, 0, bytesRead);
			}
		} finally {
			try { input.close(); } catch (IOException e) { /* ignore */ }
			try { output.close(); } catch (IOException e) { /* ignore */ }
		}
	}
	
	public static File createTempDir() throws IOException {
		File tempDir = File.createTempFile("bindex_testing", ".dir");
		tempDir.delete();
		if (!tempDir.mkdir())
			throw new IOException("Failed to create temporary directory");
		return tempDir;
	}
	
	/**
	 * Deletes the specified file. Folders are recursively deleted.<br>
	 * If file(s) cannot be deleted, no feedback is provided (fail silently).
	 * 
	 * @param f
	 *            file to be deleted
	 */
	public static void delete(File f) {
		try {
			deleteWithException(f);
		} catch (IOException e) {
			// Ignore a failed delete
		}
	}
	
	/**
	 * Deletes the specified file. Folders are recursively deleted.<br>
	 * Throws exception if any of the files could not be deleted.
	 * 
	 * @param f
	 *            file to be deleted
	 * @throws IOException
	 *             if the file (or contents of a folder) could not be deleted
	 */
	public static void deleteWithException(File f) throws IOException {
		f = f.getAbsoluteFile();
		if (!f.exists()) return;
		if (f.getParentFile() == null)
			throw new IllegalArgumentException("Cannot recursively delete root for safety reasons");

		boolean wasDeleted = true;
		if (f.isDirectory()) {
			File[] subs = f.listFiles();
			for (File sub : subs) {
				try {
					deleteWithException(sub);
				} catch (IOException e) {
					wasDeleted = false;
				}
			}
		}

		boolean fDeleted = f.delete();
		if (!fDeleted || !wasDeleted) {
			throw new IOException("Failed to delete " + f.getAbsoluteFile());
		}
	}
	
	public static File copyToTempFile(File tempDir, String resourcePath) throws IOException {
		File tempFile = new File(tempDir, resourcePath);
		
		tempFile.deleteOnExit();
		tempFile.getParentFile().mkdirs();
		
		Utils.copyFully(Utils.class.getResourceAsStream("/" + resourcePath), new FileOutputStream(tempFile));
		return tempFile;
	}

}
