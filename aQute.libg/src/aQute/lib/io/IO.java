package aQute.lib.io;

import java.io.*;
import java.net.*;
import java.nio.*;

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

	public static void copy( InputStream in, ByteBuffer bb ) throws IOException {
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
		FileOutputStream out = new FileOutputStream(b);
		try {
			copy(new FileInputStream(a), out);
		} finally {
			out.close();
		}
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
		char [] buffer = new char[10000];
		int size = a.read(buffer);
		while ( size > 0) {
			sw.write(buffer, 0, size);
			size = a.read(buffer);
		}
		return sw.toString();
	}
	
	public static File getFile( File dir, String path) {
		int n = path.indexOf('/');
		if ( n == 0 ) {
			return getFile( File.listRoots()[0], path.substring(1));
		}
		if ( n < 0 ) {
			return new File(dir,path);
		}

		String part = path.substring(0, n);
		String remainder = path.substring(n+1);
		dir = new File( dir, part);
		return getFile(dir, remainder);
	}
	
	public static void delete(File f) {
		f = f.getAbsoluteFile();
		if ( f.getParentFile() == null )
			throw new IllegalArgumentException("Cannot recursively delete root for safety reasons");
		

		if ( f.isDirectory()) {
			File [] subs = f.listFiles();
			for ( File sub : subs) 
				delete(sub);
		}
		
		delete(f);
	}
}
