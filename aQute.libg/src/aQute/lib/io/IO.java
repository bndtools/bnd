package aQute.lib.io;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.util.*;

public class IO {
	static final public File	work	= new File(System.getProperty("user.dir"));
	static final public File	home	= new File(System.getProperty("user.home"));

	public static void copy(Reader r, Writer w) throws IOException {
		try {
			char buffer[] = new char[8000];
			int size = r.read(buffer);
			while (size > 0) {
				w.write(buffer, 0, size);
				size = r.read(buffer);
			}
		}
		finally {
			r.close();
			w.flush();
		}
	}

	public static void copy(InputStream r, Writer w) throws IOException {
		copy(r, w, "UTF-8");
	}

	public static void copy(byte[] r, Writer w) throws IOException {
		copy(new ByteArrayInputStream(r), w, "UTF-8");
	}

	public static void copy(byte[] r, OutputStream w) throws IOException {
		copy(new ByteArrayInputStream(r), w);
	}

	public static void copy(InputStream r, Writer w, String charset) throws IOException {
		try {
			InputStreamReader isr = new InputStreamReader(r, charset);
			copy(isr, w);
		}
		finally {
			r.close();
		}
	}

	public static void copy(Reader r, OutputStream o) throws IOException {
		copy(r, o, "UTF-8");
	}

	public static void copy(Reader r, OutputStream o, String charset) throws IOException {
		try {
			OutputStreamWriter osw = new OutputStreamWriter(o, charset);
			copy(r, osw);
		}
		finally {
			r.close();
		}
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		copy(in, (DataOutput) dos);
		out.flush();
	}

	public static void copy(InputStream in, DataOutput out) throws IOException {
		byte[] buffer = new byte[10000];
		try {
			int size = in.read(buffer);
			while (size > 0) {
				out.write(buffer, 0, size);
				size = in.read(buffer);
			}
		}
		finally {
			in.close();
		}
	}

	public static void copy(InputStream in, ByteBuffer bb) throws IOException {
		byte[] buffer = new byte[10000];
		try {
			int size = in.read(buffer);
			while (size > 0) {
				bb.put(buffer, 0, size);
				size = in.read(buffer);
			}
		}
		finally {
			in.close();
		}
	}

	public static void copy(URL in, MessageDigest md) throws IOException {
		copy(in.openStream(), md);
	}

	public static void copy(File in, MessageDigest md) throws IOException {
		copy(new FileInputStream(in), md);
	}

	public static void copy(URLConnection in, MessageDigest md) throws IOException {
		copy(in.getInputStream(), md);
	}

	public static void copy(InputStream in, MessageDigest md) throws IOException {
		byte[] buffer = new byte[10000];
		try {
			int size = in.read(buffer);
			while (size > 0) {
				md.update(buffer, 0, size);
				size = in.read(buffer);
			}
		}
		finally {
			in.close();
		}
	}

	public static void copy(URL url, File file) throws IOException {
		URLConnection c = url.openConnection();
		copy(c, file);
	}

	public static void copy(URLConnection c, File file) throws IOException {
		copy(c.getInputStream(), file);
	}

	public static void copy(InputStream in, URL out) throws IOException {
		copy(in, out, null);
	}

	public static void copy(InputStream in, URL out, String method) throws IOException {
		URLConnection c = out.openConnection();
		if (c instanceof HttpURLConnection && method != null) {
			HttpURLConnection http = (HttpURLConnection) c;
			http.setRequestMethod(method);
		}
		c.setDoOutput(true);
		copy(in, c.getOutputStream());
	}

	public static void copy(File a, File b) throws IOException {
		if (a.isFile()) {
			FileOutputStream out = new FileOutputStream(b);
			try {
				copy(new FileInputStream(a), out);
			}
			finally {
				out.close();
			}
		} else if (a.isDirectory()) {
			if (!b.exists() && !b.mkdirs()) {
				throw new IOException("Could not create directory " + b);
			}
			if (!b.isDirectory())
				throw new IllegalArgumentException("target directory for a directory must be a directory: " + b);
			File subs[] = a.listFiles();
			for (File sub : subs) {
				copy(sub, new File(b, sub.getName()));
			}
		} else
			throw new FileNotFoundException("During copy: " + a.toString());
	}

	public static void copy(InputStream a, File b) throws IOException {
		FileOutputStream out = new FileOutputStream(b);
		try {
			copy(a, out);
		}
		finally {
			out.close();
		}
	}

	public static void copy(File a, OutputStream b) throws IOException {
		copy(new FileInputStream(a), b);
	}

	public static String collect(File a, String encoding) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(a, out);
		return new String(out.toByteArray(), encoding);
	}

	public static String collect(URL a, String encoding) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(a.openStream(), out);
		return new String(out.toByteArray(), encoding);
	}

	public static String collect(URL a) throws IOException {
		return collect(a, "UTF-8");
	}

	public static String collect(File a) throws IOException {
		return collect(a, "UTF-8");
	}

	public static String collect(String a) throws IOException {
		return collect(new File(a), "UTF-8");
	}

	public static String collect(InputStream a, String encoding) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(a, out);
		return new String(out.toByteArray(), encoding);
	}

	public static String collect(InputStream a) throws IOException {
		return collect(a, "UTF-8");
	}

	public static String collect(Reader a) throws IOException {
		StringWriter sw = new StringWriter();
		char[] buffer = new char[10000];
		int size = a.read(buffer);
		while (size > 0) {
			sw.write(buffer, 0, size);
			size = a.read(buffer);
		}
		return sw.toString();
	}

	/**
	 * Create a temporary file.
	 * 
	 * @param directory
	 *            the directory in which to create the file. Can be null, in
	 *            which case the system TMP directory is used
	 * @param pattern
	 *            the filename prefix pattern. Must be at least 3 characters
	 *            long
	 * @param suffix
	 *            the filename suffix. Can be null, in which case (system)
	 *            default suffix is used
	 * @return
	 * @throws IllegalArgumentException
	 *             when pattern is null or too short
	 * @throws IOException
	 *             when the specified (non-null) directory is not a directory
	 */
	public static File createTempFile(File directory, String pattern, String suffix) throws IllegalArgumentException,
			IOException {
		if ((pattern == null) || (pattern.length() < 3)) {
			throw new IllegalArgumentException("Pattern must be at least 3 characters long, got "
					+ ((pattern == null) ? "null" : pattern.length()));
		}

		if ((directory != null) && !directory.isDirectory()) {
			throw new FileNotFoundException("Directory " + directory + " is not a directory");
		}

		return File.createTempFile(pattern, suffix, directory);
	}

	public static File getFile(String filename) {
		return getFile(work, filename);
	}

	public static File getFile(File base, String file) {

		if (file.startsWith("~/")) {
			file = file.substring(2);
			if (!file.startsWith("~/")) {
				return getFile(home, file);
			}
		}
		if (file.startsWith("~")) {
			file = file.substring(1);
			return getFile(home.getParentFile(), file);
		}

		File f = new File(file);
		if (f.isAbsolute())
			return f;
		int n;

		if (base == null)
			base = work;

		f = base.getAbsoluteFile();

		while ((n = file.indexOf('/')) > 0) {
			String first = file.substring(0, n);
			file = file.substring(n + 1);
			if (first.equals(".."))
				f = f.getParentFile();
			else
				f = new File(f, first);
		}
		if (file.equals(".."))
			return f.getParentFile();
		return new File(f, file).getAbsoluteFile();
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
		}
		catch (IOException e) {
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
		if (!f.exists())
			return;
		if (f.getParentFile() == null)
			throw new IllegalArgumentException("Cannot recursively delete root for safety reasons");

		boolean wasDeleted = true;
		if (f.isDirectory()) {
			File[] subs = f.listFiles();
			for (File sub : subs) {
				try {
					deleteWithException(sub);
				}
				catch (IOException e) {
					wasDeleted = false;
				}
			}
		}

		boolean fDeleted = f.delete();
		if (!fDeleted || !wasDeleted) {
			throw new IOException("Failed to delete " + f.getAbsoluteFile());
		}
	}

	/**
	 * Deletes <code>to</code> file if it exists, and renames <code>from</code>
	 * file to <code>to</code>.<br>
	 * Throws exception the rename operation fails.
	 * 
	 * @param from
	 *            source file
	 * @param to
	 *            destination file
	 * @throws IOException
	 *             if the rename operation fails
	 */
	public static void rename(File from, File to) throws IOException {
		IO.deleteWithException(to);

		boolean renamed = from.renameTo(to);
		if (!renamed)
			throw new IOException("Could not rename " + from.getAbsoluteFile() + " to " + to.getAbsoluteFile());
	}

	public static long drain(InputStream in) throws IOException {
		long result = 0;
		byte[] buffer = new byte[10000];
		try {
			int size = in.read(buffer);
			while (size >= 0) {
				result += size;
				size = in.read(buffer);
			}
		}
		finally {
			in.close();
		}
		return result;
	}

	public void copy(Collection< ? > c, OutputStream out) throws IOException {
		Writer w = new OutputStreamWriter(out, "UTF-8");
		PrintWriter ps = new PrintWriter(w);
		for (Object o : c) {
			ps.println(o);
		}
		ps.flush();
		w.flush();
	}

	public static Throwable close(Closeable in) {
		if (in == null)
			return null;

		try {
			in.close();
			return null;
		}
		catch (Throwable e) {
			return e;
		}
	}

	public static URL toURL(String s, File base) throws MalformedURLException {
		int n = s.indexOf(':');
		if (n > 0 && n < 10) {
			// is url
			return new URL(s);
		}
		return getFile(base, s).toURI().toURL();
	}

	public static void store(Object o, File out) throws IOException {
		store(o, out, "UTF-8");
	}

	public static void store(Object o, File out, String encoding) throws IOException {
		FileOutputStream fout = new FileOutputStream(out);
		try {
			store(o, fout, encoding);
		}
		finally {
			fout.close();
		}
	}

	public static void store(Object o, OutputStream fout) throws UnsupportedEncodingException, IOException {
		store(o, fout, "UTF-8");
	}

	public static void store(Object o, OutputStream fout, String encoding) throws UnsupportedEncodingException,
			IOException {
		String s;

		if (o == null)
			s = "";
		else
			s = o.toString();

		try {
			fout.write(s.getBytes(encoding));
		}
		finally {
			fout.close();
		}
	}

	public static InputStream stream(String s) {
		try {
			return new ByteArrayInputStream(s.getBytes("UTF-8"));
		}
		catch (Exception e) {
			// Ignore
			return null;
		}
	}

	public static InputStream stream(String s, String encoding) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(s.getBytes(encoding));
	}

	public static InputStream stream(File s) throws FileNotFoundException {
		return new FileInputStream(s);
	}

	public static InputStream stream(URL s) throws IOException {
		return s.openStream();
	}

	public static Reader reader(String s) {
		return new StringReader(s);
	}

	public static BufferedReader reader(File f, String encoding) throws IOException {
		return reader(new FileInputStream(f), encoding);
	}

	public static BufferedReader reader(File f) throws IOException {
		return reader(f, "UTF-8");
	}

	public static PrintWriter writer(File f, String encoding) throws IOException {
		return writer(new FileOutputStream(f), encoding);
	}

	public static PrintWriter writer(File f) throws IOException {
		return writer(f, "UTF-8");
	}

	public static PrintWriter writer(OutputStream out, String encoding) throws IOException {
		return new PrintWriter(new OutputStreamWriter(out, encoding));
	}

	public static BufferedReader reader(InputStream in, String encoding) throws IOException {
		return new BufferedReader(new InputStreamReader(in, encoding));
	}

	public static BufferedReader reader(InputStream in) throws IOException {
		return reader(in, "UTF-8");
	}

	public static PrintWriter writer(OutputStream out) throws IOException {
		return writer(out, "UTF-8");
	}
}
