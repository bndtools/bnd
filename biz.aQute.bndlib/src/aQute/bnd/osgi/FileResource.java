package aQute.bnd.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;

public class FileResource implements Resource {
	private static final int		THRESHOLD	= IOConstants.PAGE_SIZE * 16;
	private static final ByteBuffer	CLOSED	= ByteBuffer.allocate(0);
	private ByteBuffer	buffer;
	private final File	file;
	private String		extra;
	private boolean		deleteOnClose;
	private final long				lastModified;
	private final long				size;

	public FileResource(File file) {
		this.file = file;
		lastModified = file.lastModified();
		size = file.length();
	}

	/**
	 * Turn a resource into a file so that anything in the conversion is
	 * properly caught
	 * 
	 * @param r
	 * @throws Exception
	 */
	public FileResource(Resource r) throws Exception {
		file = File.createTempFile("fileresource", ".resource");
		deleteOnClose(true);
		file.deleteOnExit();
		try (OutputStream out = IO.outputStream(file)) {
			r.write(out);
		}
		lastModified = r.lastModified();
		size = file.length();
	}

	@Override
	public ByteBuffer buffer() throws Exception {
		if (buffer != null) {
			return buffer.duplicate();
		}
		if (IO.isWindows() && (size > THRESHOLD)) {
			return null;
		}
		return (buffer = IO.read(file.toPath())).duplicate();
	}

	public InputStream openInputStream() throws Exception {
		if (buffer != null) {
			return IO.stream(buffer());
		} else {
			return IO.stream(file);
		}
	}

	public static void build(Jar jar, File directory, Pattern doNotCopy) {
		traverse(jar, directory.getAbsolutePath().length(), directory, doNotCopy);
	}

	@Override
	public String toString() {
		return file.getAbsolutePath();
	}

	public void write(OutputStream out) throws Exception {
		if (buffer != null) {
			IO.copy(buffer(), out);
		} else {
			IO.copy(file, out);
		}
	}

	static void traverse(Jar jar, int rootlength, File directory, Pattern doNotCopy) {
		if (doNotCopy != null && doNotCopy.matcher(directory.getName()).matches())
			return;
		jar.updateModified(directory.lastModified(), "Dir change " + directory);

		File files[] = directory.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				traverse(jar, rootlength, files[i], doNotCopy);
			else {
				String path = files[i].getAbsolutePath().substring(rootlength + 1);
				if (File.separatorChar != '/')
					path = path.replace(File.separatorChar, '/');
				jar.putResource(path, new FileResource(files[i]), true);
			}
		}
	}

	public long lastModified() {
		return lastModified;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public long size() {
		return size;
	}

	public void close() throws IOException {
		/*
		 * Allow original buffer to be garbage collected and prevent it being
		 * remapped for this FileResouce.
		 */
		buffer = CLOSED;
		if (deleteOnClose)
			IO.delete(file);
	}

	public void deleteOnClose(boolean b) {
		deleteOnClose = b;
	}

	public File getFile() {
		return file;
	}
}
