package aQute.lib.io;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.util.*;

public class IO {

	public static void copy(Reader r, Writer w) throws IOException {
		try {
			char buffer[] = new char[8000];
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

	public static void copy(byte []r, Writer w) throws IOException {
		copy( new ByteArrayInputStream(r), w, "UTF-8");
	}

	public static void copy(byte []r, OutputStream w) throws IOException {
		copy( new ByteArrayInputStream(r), w);
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
		copy(in, c.getOutputStream());
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
			while (size >= 0) {
				result += size;
				size = in.read(buffer);
			}
		} finally {
			in.close();
		}
		return result;
	}

	public void copy(Collection<?> c, OutputStream out) throws IOException {
		Writer w = new OutputStreamWriter(out,"UTF-8");
		PrintWriter ps = new PrintWriter(w);
		for (Object o : c) {
			ps.println(o);
		}
		ps.flush();
		w.flush();
	}

	public static Throwable close(Closeable in) {
		if ( in == null)
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
		FileOutputStream fout = new FileOutputStream(out);
		try {
			store(o, fout, encoding);
		} finally {
			fout.close();
		}
	}

	public static void store(Object o, OutputStream fout) throws UnsupportedEncodingException,
			IOException {
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
		return reader( new FileInputStream(f), encoding);
	}
	
	public static BufferedReader reader(File f) throws IOException {
		return reader(f,"UTF-8");
	}
	
	public static PrintWriter writer(File f, String encoding) throws IOException {
		return writer( new FileOutputStream(f),encoding);
	}
	
	public static PrintWriter writer(File f) throws IOException {
		return writer(f, "UTF-8");
	}
	
	public static PrintWriter writer(OutputStream out, String encoding) throws IOException {
		return new PrintWriter( new OutputStreamWriter( out,encoding));
	}
	public static BufferedReader reader(InputStream in, String encoding) throws IOException {
		return new BufferedReader( new InputStreamReader(in, encoding));
	}
	
	public static BufferedReader reader(InputStream in) throws IOException {
		return reader(in, "UTF-8");
	}
	public static PrintWriter writer(OutputStream out) throws IOException {
		return writer(out, "UTF-8");
	}
}
