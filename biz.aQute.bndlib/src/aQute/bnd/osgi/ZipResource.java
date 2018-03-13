package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import aQute.lib.io.IO;
import aQute.lib.zip.ZipUtil;

public class ZipResource implements Resource {
	private ByteBuffer		buffer;
	private final ZipFile	zip;
	private final ZipEntry	entry;
	private final boolean	closeZipFile;
	private long			lastModified;
	private long			size;
	private String			extra;

	ZipResource(Path path, String entryName) throws IOException {
		this(new ZipFile(path.toFile()), entryName);
	}

	private ZipResource(ZipFile zip, String entryName) throws IOException {
		this(zip, zip.getEntry(entryName), true);
		if (entry == null) {
			close();
			throw new FileNotFoundException("Entry " + entryName + " not found in " + zip.getName());
		}
	}

	ZipResource(ZipFile zip, ZipEntry entry) {
		this(zip, entry, false);
	}

	private ZipResource(ZipFile zip, ZipEntry entry, boolean closeZipFile) {
		this.zip = zip;
		this.entry = entry;
		this.closeZipFile = closeZipFile;
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
		ByteBuffer bb = IO.copy(zip.getInputStream(entry), ByteBuffer.allocate((int) size));
		bb.flip();
		return buffer = bb;
	}

	@Override
	public InputStream openInputStream() throws Exception {
		return IO.stream(buffer());
	}

	@Override
	public String toString() {
		return ":" + zip.getName() + "(" + entry.getName() + "):";
	}

	@Override
	public void write(OutputStream out) throws Exception {
		if (buffer != null) {
			IO.copy(buffer(), out);
		} else {
			IO.copy(zip.getInputStream(entry), out);
		}
	}

	@Override
	public long lastModified() {
		if (lastModified != -11L) {
			return lastModified;
		}
		return lastModified = ZipUtil.getModifiedTime(entry);
	}

	@Override
	public String getExtra() {
		return extra;
	}

	@Override
	public void setExtra(String extra) {
		this.extra = extra;
	}

	@Override
	public long size() throws Exception {
		if (size >= 0) {
			return size;
		}
		return size = getBuffer().limit();
	}

	@Override
	public void close() throws IOException {
		if (closeZipFile) {
			zip.close();
		}
	}
}
