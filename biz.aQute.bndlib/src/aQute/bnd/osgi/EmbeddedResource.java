package aQute.bnd.osgi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import aQute.lib.io.IO;

public class EmbeddedResource implements Resource {
	byte	data[];
	long	lastModified;
	String	extra;

	public EmbeddedResource(byte data[], long lastModified) {
		this.data = data;
		this.lastModified = lastModified;
	}

	public EmbeddedResource(String pc, int lastModified) throws UnsupportedEncodingException {
		this(pc.getBytes("UTF-8"), lastModified);
	}

	public InputStream openInputStream() throws FileNotFoundException {
		return new ByteArrayInputStream(data);
	}

	public void write(OutputStream out) throws IOException {
		out.write(data);
	}

	@Override
	public String toString() {
		return ":" + data.length + ":";
	}

	public static void build(Jar jar, InputStream in, long lastModified) throws IOException {
		ZipInputStream jin = new ZipInputStream(in);
		ZipEntry entry = jin.getNextEntry();
		while (entry != null) {
			if (!entry.isDirectory()) {
				byte data[] = collect(jin);
				jar.putResource(entry.getName(), new EmbeddedResource(data, lastModified), true);
			}
			entry = jin.getNextEntry();
		}
		IO.drain(in);
		jin.close();
	}

	/**
	 * Convenience method to turn an inputstream into a byte array. The method
	 * uses a recursive algorithm to minimize memory usage.
	 * 
	 * @param in stream with data
	 * @param offset where we are in the stream
	 * @returns byte array filled with data
	 */
	static byte[] collect(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(in, out);
		return out.toByteArray();
	}

	static void copy(InputStream in, OutputStream out) throws IOException {
		int available = in.available();
		if (available <= 10000)
			available = 64000;
		byte[] buffer = new byte[available];
		int size;
		while ((size = in.read(buffer)) > 0)
			out.write(buffer, 0, size);
	}

	public long lastModified() {
		return lastModified;
	}

	public static void build(Jar sub, Resource resource) throws Exception {
		InputStream in = resource.openInputStream();
		try {
			build(sub, in, resource.lastModified());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			in.close();
		}
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public long size() {
		return data.length;
	}

}
