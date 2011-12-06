package aQute.lib.io;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class IO {
	public static void copy(InputStream in, OutputStream out) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		copy(in, (DataOutput) dos);
	}

	public static void copy(InputStream in, DataOutput out) throws IOException {
		byte[] buffer = new byte[10000];
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
		byte[] buffer = new byte[10000];
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

	public static void copy(File a, File b) throws IOException {
		if (a.isFile()) {
			FileOutputStream out = new FileOutputStream(b);
			try {
				copy(new FileInputStream(a), out);
			} finally {
				out.close();
			}
		} else if (a.isDirectory()) {
			b.mkdirs();
			if (!b.isDirectory())
				throw new IllegalArgumentException(
						"target directory for a directory must be a directory: " + b);
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

	public static String collect(File a) throws IOException {
		return collect(a, "UTF-8");
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

	public static File getFile(File base, String file) {
		File f = new File(file);
		if (f.isAbsolute())
			return f;
		int n;

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
		else
			return new File(f, file).getAbsoluteFile();
	}

	public static void delete(File f) {
		f = f.getAbsoluteFile();
		if (f.getParentFile() == null)
			throw new IllegalArgumentException("Cannot recursively delete root for safety reasons");

		if (f.isDirectory()) {
			File[] subs = f.listFiles();
			for (File sub : subs)
				delete(sub);
		}

		f.delete();
	}

	public static long drain(InputStream in) throws IOException {
		long result = 0;
		byte[] buffer = new byte[10000];
		try {
			int size = in.read(buffer);
			while (size > 0) {
				result+=size;
				size = in.read(buffer);
			}
		} finally {
			in.close();
		}
		return result;
	}

	public void copy(Collection<?> c, OutputStream out) {
		PrintStream ps = new PrintStream(out);
		for (Object o : c) {
			ps.println(o);
		}
		ps.flush();
	}

	public static Throwable close(Closeable in) {
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
		FileOutputStream fout = new FileOutputStream(out);
		try {
			store(o, fout, encoding);
		} finally {
			fout.close();
		}
	}

	public static void store(Object o, OutputStream fout) throws UnsupportedEncodingException, IOException {
		store(o, fout, "UTF-8");
	}

	public static void store(Object o, OutputStream fout, String encoding)
			throws UnsupportedEncodingException, IOException {
		String s;

		if (o == null)
			s = "";
		else
			s = o.toString();

		try {
			fout.write(s.getBytes(encoding));
		} finally {
			fout.close();
		}
	}

}
