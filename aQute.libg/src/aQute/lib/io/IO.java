package aQute.lib.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import aQute.libg.glob.Glob;

public class IO {
	static final int			BUFFER_SIZE	= IOConstants.PAGE_SIZE * 16;

	static final public File	work		= new File(System.getProperty("user.dir"));
	static final public File	home;

	static {
		File tmp = null;
		try {
			tmp = new File(System.getenv("HOME"));
		} catch (Exception e) {}
		if (tmp == null) {
			tmp = new File(System.getProperty("user.home"));
		}
		home = tmp;
	}

	public static String getExtension(String fileName, String deflt) {
		int n = fileName.lastIndexOf('.');
		if (n < 0)
			return deflt;

		return fileName.substring(n + 1);
	}

	public static Collection<File> tree(File current) {
		Set<File> files = new LinkedHashSet<File>();
		traverse(files, current, null);
		return files;
	}

	public static Collection<File> tree(File current, String glob) {
		Set<File> files = new LinkedHashSet<File>();
		traverse(files, current, glob == null ? null : new Glob(glob));
		return files;
	}

	private static void traverse(Collection<File> files, File current, Glob glob) {
		if (current.isFile() && (glob == null || glob.matcher(current.getName()).matches())) {
			files.add(current);
		} else if (current.isDirectory()) {
			for (File sub : current.listFiles()) {
				traverse(files, sub, glob);
			}
		}
	}

	public static void copy(Reader r, Writer w) throws IOException {
		try {
			char buffer[] = new char[BUFFER_SIZE];
			int size = r.read(buffer);
			while (size > 0) {
				w.write(buffer, 0, size);
				size = r.read(buffer);
			}
		} finally {
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

	public static void copy(byte[] data, File file) throws FileNotFoundException, IOException {
		FileOutputStream out = new FileOutputStream(file);
		try {
			copy(data, out);
		} finally {
			out.close();
		}
	}

	public static void copy(byte[] r, OutputStream w) throws IOException {
		copy(new ByteArrayInputStream(r), w);
	}

	public static void copy(InputStream r, Writer w, String charset) throws IOException {
		try {
			InputStreamReader isr = new InputStreamReader(r, charset);
			copy(isr, w);
		} finally {
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
		} finally {
			r.close();
		}
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		copy(in, (DataOutput) dos);
		out.flush();
	}

	public static void copy(InputStream in, DataOutput out) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			int size = in.read(buffer);
			while (size > 0) {
				out.write(buffer, 0, size);
				size = in.read(buffer);
			}
		} finally {
			in.close();
		}
	}

	public static void copy(InputStream in, ByteBuffer bb) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			int size = in.read(buffer);
			while (size > 0) {
				bb.put(buffer, 0, size);
				size = in.read(buffer);
			}
		} finally {
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
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			int size = in.read(buffer);
			while (size > 0) {
				md.update(buffer, 0, size);
				size = in.read(buffer);
			}
		} finally {
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
		OutputStream os = c.getOutputStream();
		try {
			copy(in, os);
		} finally {
			os.close();
		}
	}

	public static void copy(File a, File b) throws IOException {
		if (a.isFile()) {
			FileOutputStream out = new FileOutputStream(b);
			try {
				copy(new FileInputStream(a), out);
			} finally {
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
		} finally {
			out.close();
		}
	}

	public static void copy(File a, OutputStream b) throws IOException {
		copy(new FileInputStream(a), b);
	}

	public static byte[] read(File f) throws IOException {
		byte[] data = new byte[(int) f.length()];
		DataInputStream in = new DataInputStream(new FileInputStream(f));
		try {
			in.readFully(data);
			return data;
		} finally {
			in.close();
		}
	}

	public static byte[] read(URL u) throws IOException {
		return read(u.openStream());
	}

	public static byte[] read(InputStream in) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		copy(in, bout);
		return bout.toByteArray();
	}

	public static void write(byte[] data, OutputStream out) throws Exception {
		copy(new ByteArrayInputStream(data), out);
	}

	public static void write(byte[] data, File out) throws Exception {
		copy(new ByteArrayInputStream(data), out);
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
		char[] buffer = new char[BUFFER_SIZE];
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
	 * @param directory the directory in which to create the file. Can be null,
	 *            in which case the system TMP directory is used
	 * @param pattern the filename prefix pattern. Must be at least 3 characters
	 *            long
	 * @param suffix the filename suffix. Can be null, in which case (system)
	 *            default suffix is used
	 * @return temp file
	 * @throws IllegalArgumentException when pattern is null or too short
	 * @throws IOException when the specified (non-null) directory is not a
	 *             directory
	 */
	public static File createTempFile(File directory, String pattern, String suffix)
			throws IllegalArgumentException, IOException {
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
	 * @param f file to be deleted
	 */
	public static void delete(File f) {
		try {
			deleteWithException(f);
		} catch (IOException e) {
			// Ignore a failed delete
		}
	}

	/**
	 * Deletes and creates directories
	 */
	public static void initialize(File f) {
		try {
			deleteWithException(f);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		f.mkdirs();
	}

	/**
	 * Deletes the specified file. Folders are recursively deleted.<br>
	 * Throws exception if any of the files could not be deleted.
	 * 
	 * @param f file to be deleted
	 * @throws IOException if the file (or contents of a folder) could not be
	 *             deleted
	 */
	public static void deleteWithException(File f) throws IOException {
		f = f.getAbsoluteFile();

		if (!f.exists()) {
			if (isSymbolicLink(f)) {
				f.delete();
			}
			return;
		}
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

	/**
	 * Deletes <code>to</code> file if it exists, and renames <code>from</code>
	 * file to <code>to</code>.<br>
	 * Throws exception the rename operation fails.
	 * 
	 * @param from source file
	 * @param to destination file
	 * @throws IOException if the rename operation fails
	 */
	public static void rename(File from, File to) throws IOException {
		IO.deleteWithException(to);

		boolean renamed = from.renameTo(to);
		if (!renamed)
			throw new IOException("Could not rename " + from.getAbsoluteFile() + " to " + to.getAbsoluteFile());
	}

	public static long drain(InputStream in) throws IOException {
		long result = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			int size = in.read(buffer);
			while (size >= 0) {
				result += size;
				size = in.read(buffer);
			}
		} finally {
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
		} catch (Throwable e) {
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
		store(o, new FileOutputStream(out), encoding);
	}

	public static void store(Object o, OutputStream fout) throws IOException {
		store(o, fout, "UTF-8");
	}

	public static void store(Object o, OutputStream fout, String encoding) throws IOException {
		try {
			String s = (o == null) ? "" : o.toString();
			fout.write(s.getBytes(encoding));
		} finally {
			fout.close();
		}
	}

	public static InputStream stream(String s) {
		try {
			return new ByteArrayInputStream(s.getBytes("UTF-8"));
		} catch (Exception e) {
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

	/**
	 * Reflective way to create a link. This assumes Java 7+
	 */
	public static boolean createSymbolicLink(File link, File target) throws Exception {
		try {
			Method toPath = link.getClass().getMethod("toPath");
			Class< ? > Files = Class.forName("java.nio.file.Files");
			for (Method m : Files.getMethods()) {
				if (m.getName().equals("createSymbolicLink") && m.getParameterTypes().length == 3) {
					Object attrs = Array.newInstance(m.getParameterTypes()[2].getComponentType(), 0);
					m.invoke(null, toPath.invoke(link), toPath.invoke(target), attrs);
					return true;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	public static boolean isSymbolicLink(File link) {
		try {
			Method toPath = link.getClass().getMethod("toPath");
			Class< ? > Files = Class.forName("java.nio.file.Files");
			Method method = Files.getMethod("isSymbolicLink", toPath.getReturnType());
			return (Boolean) method.invoke(null, toPath.invoke(link));
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	static public OutputStream	nullStream	= new OutputStream() {

												@Override
												public void write(int var0) throws IOException {}

												@Override
												public void write(byte[] var0) throws IOException {}

												@Override
												public void write(byte[] var0, int from, int l) throws IOException {}
											};
	static public Writer		nullWriter	= new Writer() {
												public java.io.Writer append(char var0) throws java.io.IOException {
													return null;
												}

												public java.io.Writer append(java.lang.CharSequence var0)
														throws java.io.IOException {
													return null;
												}

												public java.io.Writer append(java.lang.CharSequence var0, int var1,
														int var2) throws java.io.IOException {
													return null;
												}

												public void write(int var0) throws java.io.IOException {}

												public void write(java.lang.String var0) throws java.io.IOException {}

												public void write(java.lang.String var0, int var1, int var2)
														throws java.io.IOException {}

												public void write(char[] var0) throws java.io.IOException {}

												public void write(char[] var0, int var1, int var2)
														throws java.io.IOException {}

												public void close() throws IOException {}

												public void flush() throws IOException {}
											};

	public static String toSafeFileName(String string) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c < ' ')
				continue;

			if (isWindows()) {
				switch (c) {
					case '<' :
					case '>' :
					case '"' :
					case '/' :
					case '\\' :
					case '|' :
					case '*' :
					case ':' :
						sb.append('%');
						break;
					default :
						sb.append(c);
				}
			} else {
				switch (c) {
					case '/' :
						sb.append('%');
						break;
					default :
						sb.append(c);
				}
			}
		}
		if (sb.length() == 0 || (isWindows() && RESERVED_WINDOWS_P.matcher(sb).matches()))
			sb.append("_");

		return sb.toString();
	}

	final static Pattern RESERVED_WINDOWS_P = Pattern.compile(
			"CON|PRN|AUX|NUL|COM1|COM2|COM3|COM4|COM5|COM6|COM7|COM8|COM9|LPT1|LPT2|LPT3|LPT4|LPT5|LPT6|LPT7|LPT8|LPT9");

	static boolean isWindows() {
		return File.separatorChar == '\\';
	}
}
