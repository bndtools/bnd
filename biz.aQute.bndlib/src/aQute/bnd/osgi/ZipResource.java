package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import aQute.lib.io.IO;
import aQute.lib.zip.ZipUtil;

public class ZipResource implements Resource {
	private ByteBuffer	buffer;
	private final ZipFile	zip;
	private final ZipEntry	entry;
	private long			lastModified;
	private long			size;
	private String			extra;

	ZipResource(ZipFile zip, ZipEntry entry) {
		this.zip = zip;
		this.entry = entry;
		lastModified = -11L;
		size = entry.getSize();
		byte[] data = entry.getExtra();
		if (data != null) {
			extra = new String(data, UTF_8);
		}
	}

	@Override
	public ByteBuffer buffer() throws Exception {
		return getBuffer().duplicate();
	}

	private ByteBuffer getBuffer() throws Exception {
		if (buffer != null) {
			return buffer;
		}
		if (size == -1) {
			return buffer = ByteBuffer.wrap(IO.read(zip.getInputStream(entry)));
		}
		ByteBuffer bb = ByteBuffer.allocate((int) size);
		IO.copy(zip.getInputStream(entry), bb);
		bb.flip();
		return buffer = bb;
	}

	public InputStream openInputStream() throws Exception {
		return IO.stream(buffer());
	}

	@Override
	public String toString() {
		return ":" + zip.getName() + "(" + entry.getName() + "):";
	}

	public static ZipFile build(Jar jar, File file) throws ZipException, IOException {
		return build(jar, file, null);
	}

	public static ZipFile build(Jar jar, File file, Pattern pattern) throws ZipException, IOException {
		try {
			ZipFile zip = new ZipFile(file);
			nextEntry: for (Enumeration< ? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				if (pattern != null) {
					Matcher m = pattern.matcher(entry.getName());
					if (!m.matches())
						continue nextEntry;
				}
				if (!entry.isDirectory()) {
					jar.putResource(entry.getName(), new ZipResource(zip, entry), true);
				}
			}
			return zip;
		} catch (ZipException e) {
			ZipException ze = new ZipException(
					"The JAR/ZIP file (" + file.getAbsolutePath() + ") seems corrupted, error: " + e.getMessage());
			ze.initCause(e);
			throw ze;
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Problem opening JAR: " + file.getAbsolutePath());
		}
	}

	public void write(OutputStream out) throws Exception {
		IO.copy(buffer(), out);
	}

	public long lastModified() {
		if (lastModified != -11L) {
			return lastModified;
		}
		return lastModified = ZipUtil.getModifiedTime(entry);
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public long size() throws Exception {
		if (size >= 0) {
			return size;
		}
		return size = getBuffer().limit();
	}

	public void close() throws IOException {}
}
