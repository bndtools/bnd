package org.bndtools.utils.jar;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import aQute.lib.io.IO;

/**
 * A utility for reading either JAR files or directories that have the same
 * layout as a JAR file.
 */
public class PseudoJar implements Closeable {

	private final File			base;

	private JarInputStream		jarStream		= null;
	private Iterator<String>	dirIndex		= null;

	private JarEntry			lastJarEntry	= null;
	private String				lastIndexEntry	= null;

	public PseudoJar(File file) {
		this.base = file;
	}

	private void initJarStream() throws IOException {
		if (jarStream != null)
			return;

		if (!base.isFile())
			throw new IOException("Cannot read as JAR, file does not exist or is not a plain file: " + base);

		jarStream = new JarInputStream(IO.stream(base));
	}

	private void initDirIndex() throws IOException {
		if (dirIndex != null)
			return;

		if (!base.isDirectory())
			throw new IOException("Cannot read as directory, does not exist or is not a plain directory: " + base);

		List<String> tmpIndex = new LinkedList<>();
		index("", base, tmpIndex);
		dirIndex = tmpIndex.iterator();
	}

	private static void index(String prefix, File dir, List<String> index) {
		File[] children = dir.listFiles();
		Arrays.sort(children, (f1, f2) -> f1.getName()
			.compareTo(f2.getName()));
		if (children != null)
			for (File child : children) {
				String path = prefix + child.getName();
				if (child.isDirectory())
					path += "/";

				index.add(path);

				if (child.isDirectory())
					index(path, child, index);
			}
	}

	public Manifest readManifest() throws IOException {
		if (base.isDirectory()) {
			File manifest = new File(base, "META-INF/MANIFEST.MF");
			if (!manifest.isFile()) {
				return null;
			}
			try (InputStream in = IO.stream(manifest)) {
				return new Manifest(in);
			}
		}
		initJarStream();
		return jarStream.getManifest();
	}

	public String nextEntry() throws IOException {
		String path;

		if (base.isDirectory()) {
			initDirIndex();
			path = dirIndex.hasNext() ? dirIndex.next() : null;
			lastIndexEntry = path;
		} else {
			initJarStream();

			lastJarEntry = jarStream.getNextJarEntry();
			path = lastJarEntry != null ? lastJarEntry.getName() : null;
		}

		return path;
	}

	public boolean isDirectoryEntry() {
		if (lastJarEntry != null)
			return lastJarEntry.isDirectory();

		if (lastIndexEntry != null)
			return lastIndexEntry.endsWith("/");

		throw new IllegalStateException("No entry is current");
	}

	public InputStream openEntry() throws IOException {
		final InputStream result;

		if (jarStream != null) {
			if (lastJarEntry == null)
				throw new IOException("No more entries available");
			result = new InputStream() {
				@Override
				public int read() throws IOException {
					return jarStream.read();
				}

				@Override
				public int read(byte[] b) throws IOException {
					return jarStream.read(b);
				}

				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					return jarStream.read(b, off, len);
				}

				@Override
				public void close() throws IOException {
					// swallow the close, which would otherwise close the whole
					// JAR stream
				}
			};
		} else {
			if (lastIndexEntry == null)
				throw new IOException("No more entries available");
			result = IO.stream(new File(base, lastIndexEntry));
		}

		return result;
	}

	@Override
	public void close() throws IOException {
		if (jarStream != null)
			jarStream.close();
	}

}
