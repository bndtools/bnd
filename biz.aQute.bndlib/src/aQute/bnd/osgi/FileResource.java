package aQute.bnd.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import aQute.lib.io.IO;

public class FileResource implements Resource {
	private static final ByteBuffer	CLOSED	= ByteBuffer.allocate(0);
	private ByteBuffer	buffer;
	private final File	file;
	private String		extra;
	private boolean		deleteOnClose;

	public FileResource(File file) {
		this.file = file;
	}

	/**
	 * Turn a resource into a file so that anything in the conversion is
	 * properly caught
	 * 
	 * @param r
	 * @throws Exception
	 */
	public FileResource(Resource r) throws Exception {
		this.file = File.createTempFile("fileresource", ".resource");
		deleteOnClose(true);
		this.file.deleteOnExit();
		IO.copy(r.openInputStream(), this.file);
	}

	@Override
	public ByteBuffer buffer() throws Exception {
		return getBuffer().duplicate();
	}

	private ByteBuffer getBuffer() throws Exception {
		if (buffer != null) {
			return buffer;
		}
		return buffer = IO.read(file.toPath());
	}

	public InputStream openInputStream() throws Exception {
		return IO.stream(buffer());
	}

	public static void build(Jar jar, File directory, Pattern doNotCopy) {
		traverse(jar, directory.getAbsolutePath().length(), directory, doNotCopy);
	}

	@Override
	public String toString() {
		return file.getAbsolutePath();
	}

	public void write(OutputStream out) throws Exception {
		IO.copy(buffer(), out);
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
		return file.lastModified();
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public long size() {
		return (int) file.length();
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
