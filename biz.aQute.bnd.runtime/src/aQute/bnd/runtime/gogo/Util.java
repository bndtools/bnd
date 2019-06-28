/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package aQute.bnd.runtime.gogo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class Util {

	static final String CWD = "_cwd";

	public static String getBundleName(Bundle bundle) {
		if (bundle != null) {
			String name = bundle.getHeaders()
				.get(Constants.BUNDLE_NAME);
			return (name == null) ? "Bundle " + Long.toString(bundle.getBundleId())
				: name + " (" + Long.toString(bundle.getBundleId()) + ")";
		}
		return "[STALE BUNDLE]";
	}

	private final static StringBuffer m_sb = new StringBuffer();

	public static String getUnderlineString(int len) {
		synchronized (m_sb) {
			m_sb.delete(0, m_sb.length());
			for (int i = 0; i < len; i++) {
				m_sb.append('-');
			}
			return m_sb.toString();
		}
	}

	public static String getValueString(Object obj) {
		synchronized (m_sb) {
			if (obj instanceof String) {
				return (String) obj;
			} else if (obj instanceof String[]) {
				String[] array = (String[]) obj;
				m_sb.delete(0, m_sb.length());
				for (int i = 0; i < array.length; i++) {
					if (i != 0) {
						m_sb.append(", ");
					}
					m_sb.append(array[i]);
				}
				return m_sb.toString();
			} else if (obj instanceof Boolean) {
				return ((Boolean) obj).toString();
			} else if (obj instanceof Long) {
				return ((Long) obj).toString();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).toString();
			} else if (obj instanceof Short) {
				return ((Short) obj).toString();
			} else if (obj instanceof Double) {
				return obj.toString();
			} else if (obj instanceof Float) {
				return obj.toString();
			} else if (obj == null) {
				return "null";
			} else {
				return obj.toString();
			}
		}
	}

	public static <T> T getService(BundleContext bc, Class<T> clazz, List<ServiceReference<?>> refs) {
		@SuppressWarnings("unchecked")
		ServiceReference<T> ref = (ServiceReference<T>) bc.getServiceReference(clazz.getName());
		if (ref == null) {
			return null;
		}
		T t = bc.getService(ref);
		if (t != null) {
			refs.add(ref);
		}
		return t;
	}

	public static void ungetServices(BundleContext bc, List<ServiceReference<?>> refs) {
		while (refs.size() > 0) {
			bc.ungetService(refs.remove(0));
		}
	}

	public static void downloadSource(PrintStream out, PrintStream err, URL srcURL, File localDir, boolean extract) {
		// Get the file name from the URL.
		String fileName = (srcURL.getFile()
			.lastIndexOf('/') > 0) ? srcURL.getFile()
				.substring(srcURL.getFile()
					.lastIndexOf('/') + 1)
				: srcURL.getFile();

		try {
			out.println("Connecting...");

			if (!localDir.exists()) {
				err.println("Destination directory does not exist.");
			}
			File file = new File(localDir, fileName);

			OutputStream os = new FileOutputStream(file);
			URLConnection conn = srcURL.openConnection();
			Util.setProxyAuth(conn);
			int total = conn.getContentLength();
			InputStream is = conn.getInputStream();

			if (total > 0) {
				out.println("Downloading " + fileName + " ( " + total + " bytes ).");
			} else {
				out.println("Downloading " + fileName + ".");
			}
			byte[] buffer = new byte[4096];
			for (int len = is.read(buffer); len > 0; len = is.read(buffer)) {
				os.write(buffer, 0, len);
			}

			os.close();
			is.close();

			if (extract) {
				is = new FileInputStream(file);
				JarInputStream jis = new JarInputStream(is);
				out.println("Extracting...");
				unjar(jis, localDir);
				jis.close();
				file.delete();
			}
		} catch (Exception ex) {
			err.println(ex);
		}
	}

	public static void unjar(JarInputStream jis, File dir) throws IOException {
		// Reusable buffer.
		byte[] buffer = new byte[4096];

		// Loop through JAR entries.
		for (JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry()) {
			if (je.getName()
				.startsWith("/")) {
				throw new IOException("JAR resource cannot contain absolute paths.");
			}

			File target = new File(dir, je.getName());

			// Check to see if the JAR entry is a directory.
			if (je.isDirectory()) {
				if (!target.exists()) {
					if (!target.mkdirs()) {
						throw new IOException("Unable to create target directory: " + target);
					}
				}
				// Just continue since directories do not have content to copy.
				continue;
			}

			int lastIndex = je.getName()
				.lastIndexOf('/');
			String name = (lastIndex >= 0) ? je.getName()
				.substring(lastIndex + 1) : je.getName();
			String destination = (lastIndex >= 0) ? je.getName()
				.substring(0, lastIndex) : "";

			// JAR files use '/', so convert it to platform separator.
			destination = destination.replace('/', File.separatorChar);
			copy(jis, dir, name, destination, buffer);
		}
	}

	public static void copy(InputStream is, File dir, String destName, String destDir, byte[] buffer)
		throws IOException {
		if (destDir == null) {
			destDir = "";
		}

		// Make sure the target directory exists and
		// that is actually a directory.
		File targetDir = new File(dir, destDir);
		if (!targetDir.exists()) {
			if (!targetDir.mkdirs()) {
				throw new IOException("Unable to create target directory: " + targetDir);
			}
		} else if (!targetDir.isDirectory()) {
			throw new IOException("Target is not a directory: " + targetDir);
		}

		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(targetDir, destName)));
		int count = 0;
		while ((count = is.read(buffer)) > 0) {
			bos.write(buffer, 0, count);
		}
		bos.close();
	}

	public static void setProxyAuth(URLConnection conn) throws IOException {
		// Support for http proxy authentication
		String auth = System.getProperty("http.proxyAuth");
		if ((auth != null) && (auth.length() > 0)) {
			if ("http".equals(conn.getURL()
				.getProtocol()) || "https".equals(
					conn.getURL()
						.getProtocol())) {
				String base64 = java.util.Base64.getEncoder()
					.encodeToString(auth.getBytes(StandardCharsets.UTF_8));
				conn.setRequestProperty("Proxy-Authorization", "Basic " + base64);
			}
		}
	}

	public static InputStream openURL(final URL url) throws IOException {
		// Do it the manual way to have a chance to
		// set request properties as proxy auth (EW).
		return openURL(url.openConnection());
	}

	public static InputStream openURL(final URLConnection conn) throws IOException {
		// Do it the manual way to have a chance to
		// set request properties as proxy auth (EW).
		setProxyAuth(conn);
		return conn.getInputStream();
	}

	public static List<String> parseSubstring(String value) {
		List<String> pieces = new ArrayList<>();
		StringBuilder ss = new StringBuilder();
		// int kind = SIMPLE; // assume until proven otherwise
		boolean wasStar = false; // indicates last piece was a star
		boolean leftstar = false; // track if the initial piece is a star
		boolean rightstar = false; // track if the final piece is a star

		int idx = 0;

		// We assume (sub)strings can contain leading and trailing blanks
		boolean escaped = false;
		loop: for (;;) {
			if (idx >= value.length()) {
				if (wasStar) {
					// insert last piece as "" to handle trailing star
					rightstar = true;
				} else {
					pieces.add(ss.toString());
					// accumulate the last piece
					// note that in the case of
					// (cn=); this might be
					// the string "" (!=null)
				}
				ss.setLength(0);
				break loop;
			}

			// Read the next character and account for escapes.
			char c = value.charAt(idx++);
			if (!escaped && ((c == '(') || (c == ')'))) {
				throw new IllegalArgumentException("Illegal value: " + value);
			} else if (!escaped && (c == '*')) {
				if (wasStar) {
					// encountered two successive stars;
					// I assume this is illegal
					throw new IllegalArgumentException("Invalid filter string: " + value);
				}
				if (ss.length() > 0) {
					pieces.add(ss.toString()); // accumulate the pieces
					// between '*' occurrences
				}
				ss.setLength(0);
				// if this is a leading star, then track it
				if (pieces.isEmpty()) {
					leftstar = true;
				}
				wasStar = true;
			} else if (!escaped && (c == '\\')) {
				escaped = true;
			} else {
				escaped = false;
				wasStar = false;
				ss.append(c);
			}
		}
		if (leftstar || rightstar || pieces.size() > 1) {
			// insert leading and/or trailing "" to anchor ends
			if (rightstar) {
				pieces.add("");
			}
			if (leftstar) {
				pieces.add(0, "");
			}
		}
		return pieces;
	}

	public static String unparseSubstring(List<String> pieces) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < pieces.size(); i++) {
			if (i > 0) {
				sb.append("*");
			}
			sb.append(pieces.get(i));
		}
		return sb.toString();
	}

	public static boolean compareSubstring(List<String> pieces, String s) {
		// Walk the pieces to match the string
		// There are implicit stars between each piece,
		// and the first and last pieces might be "" to anchor the match.
		// assert (pieces.length > 1)
		// minimal case is <string>*<string>

		boolean result = true;
		int len = pieces.size();

		// Special case, if there is only one piece, then
		// we must perform an equality test.
		if (len == 1) {
			return s.equals(pieces.get(0));
		}

		// Otherwise, check whether the pieces match
		// the specified string.

		int index = 0;

		loop: for (int i = 0; i < len; i++) {
			String piece = pieces.get(i);

			// If this is the first piece, then make sure the
			// string starts with it.
			if (i == 0) {
				if (!s.startsWith(piece)) {
					result = false;
					break loop;
				}
			}

			// If this is the last piece, then make sure the
			// string ends with it.
			if (i == len - 1) {
				result = s.endsWith(piece);
				break loop;
			}

			// If this is neither the first or last piece, then
			// make sure the string contains it.
			if ((i > 0) && (i < (len - 1))) {
				index = s.indexOf(piece, index);
				if (index < 0) {
					result = false;
					break loop;
				}
			}

			// Move string index beyond the matching piece.
			index += piece.length();
		}

		return result;
	}

	/**
	 * Intepret a string as a URI relative to the current working directory.
	 * 
	 * @param session the session (where the CWD is stored)
	 * @param relativeUri the input URI
	 * @return the resulting URI as a string
	 * @throws IOException
	 */
	public static String resolveUri(CommandSession session, String relativeUri) throws IOException {
		File cwd = (File) session.get(CWD);
		if (cwd == null) {
			cwd = new File("").getCanonicalFile();
			session.put(CWD, cwd);
		}
		if ((relativeUri == null) || (relativeUri.length() == 0)) {
			return relativeUri;
		}

		URI curUri = cwd.toURI();
		URI newUri = curUri.resolve(relativeUri);
		return newUri.toString();
	}
}
