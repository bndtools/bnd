package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import aQute.lib.io.IO;
import aQute.lib.io.NonClosingInputStream;
public class EmbeddedResource implements Resource {
	private final ByteBuffer	buffer;
	private final long			lastModified;
	private String				extra;

	public EmbeddedResource(byte data[], long lastModified) {
		this.buffer = ByteBuffer.wrap(data);
		this.lastModified = lastModified;
	}

	public EmbeddedResource(String pc, int lastModified) {
		this(pc.getBytes(UTF_8), lastModified);
	}

	@Override
	public ByteBuffer buffer() {
		return buffer.duplicate();
	}

	public InputStream openInputStream() {
		return IO.stream(buffer());
	}

	public void write(OutputStream out) throws Exception {
		IO.copy(buffer(), out);
	}

	@Override
	public String toString() {
		return ":" + size() + ":";
	}

	public static void build(Jar jar, InputStream in, long lastModified) throws IOException {
		try (ZipInputStream jin = new ZipInputStream(in)) {
			InputStream read = new NonClosingInputStream(jin);
			ZipEntry entry = jin.getNextEntry();
			while (entry != null) {
				if (!entry.isDirectory()) {
					byte data[] = IO.read(read);
					jar.putResource(entry.getName(), new EmbeddedResource(data, lastModified), true);
				}
				entry = jin.getNextEntry();
			}
			IO.drain(in);
		}
	}

	public long lastModified() {
		return lastModified;
	}

	public static void build(Jar sub, Resource resource) throws Exception {
		try (InputStream in = resource.openInputStream()) {
			build(sub, in, resource.lastModified());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public long size() {
		return buffer.limit();
	}

	public void close() throws IOException {}
}
