package aQute.lib.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UTFDataFormatException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.exceptions.ConsumerWithException;
import aQute.lib.stringrover.StringRover;
import aQute.libg.glob.Glob;

public class IO {
	private static final Pattern						WINDOWS_MACROS			= Pattern.compile("%([^%]+)%");
	private static final int							BUFFER_SIZE				= IOConstants.PAGE_SIZE * 16;
	private static final int							DIRECT_MAP_THRESHOLD	= BUFFER_SIZE;
	private static final boolean						isWindows				= File.separatorChar == '\\';
	static final public File							work					= new File(
		System.getProperty("user.dir"));
	static final public File							home;
	static final public File							JAVA_HOME;
	private static final EnumSet<StandardOpenOption>	writeOptions			= EnumSet.of(StandardOpenOption.WRITE,
		StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	private static final EnumSet<StandardOpenOption>	readOptions				= EnumSet.of(StandardOpenOption.READ);

	static {
		EnvironmentCalculator hc = new EnvironmentCalculator(isWindows);
		home = hc.getHome();
		JAVA_HOME = hc.getJavaHome();
	}

	public static String getExtension(String fileName, String deflt) {
		int n = fileName.lastIndexOf('.');
		if (n < 0)
			return deflt;

		return fileName.substring(n + 1);
	}

	public static Collection<File> tree(File current) {
		Set<File> files = new LinkedHashSet<>();
		traverse(files, current, null);
		return files;
	}

	public static Collection<File> tree(File current, String glob) {
		Set<File> files = new LinkedHashSet<>();
		traverse(files, current, glob == null ? null : new Glob(glob));
		return files;
	}

	private static void traverse(Collection<File> files, File current, Glob glob) {
		if (current.isFile() && (glob == null || glob.matcher(current.getName())
			.matches())) {
			files.add(current);
		} else if (current.isDirectory()) {
			for (File sub : current.listFiles()) {
				traverse(files, sub, glob);
			}
		}
	}

	public static File copy(byte[] data, File file) throws IOException {
		copy(data, file.toPath());
		return file;
	}

	public static Path copy(byte[] data, Path path) throws IOException {
		try (FileChannel out = writeChannel(path)) {
			ByteBuffer bb = ByteBuffer.wrap(data);
			while (bb.hasRemaining()) {
				out.write(bb);
			}
		}
		return path;
	}

	public static Writer copy(byte[] data, Writer w) throws IOException {
		w.write(new String(data, 0, data.length, UTF_8));
		return w;
	}

	public static OutputStream copy(byte[] data, OutputStream out) throws IOException {
		out.write(data, 0, data.length);
		return out;
	}

	public static Writer copy(Reader r, Writer w) throws IOException {
		try {
			char[] buffer = new char[BUFFER_SIZE];
			for (int size; (size = r.read(buffer, 0, buffer.length)) > 0;) {
				w.write(buffer, 0, size);
			}
			return w;
		} finally {
			r.close();
		}
	}

	public static OutputStream copy(Reader r, OutputStream out) throws IOException {
		return copy(r, out, UTF_8);
	}

	public static OutputStream copy(Reader r, OutputStream out, String charset) throws IOException {
		return copy(r, out, Charset.forName(charset));
	}

	public static OutputStream copy(Reader r, OutputStream out, Charset charset) throws IOException {
		Writer w = writer(out, charset);
		try {
			copy(r, w);
			return out;
		} finally {
			w.flush();
		}
	}

	public static Writer copy(InputStream in, Writer w) throws IOException {
		return copy(in, w, UTF_8);
	}

	public static Writer copy(InputStream in, Writer w, String charset) throws IOException {
		return copy(in, w, Charset.forName(charset));
	}

	public static Writer copy(InputStream in, Writer w, Charset charset) throws IOException {
		return copy(reader(in, charset), w);
	}

	public static OutputStream copy(InputStream in, OutputStream out) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
				out.write(buffer, 0, size);
			}
			return out;
		} finally {
			in.close();
		}
	}

	public static ByteBufferOutputStream copy(InputStream in, ByteBufferOutputStream out) throws IOException {
		try {
			out.write(in);
			return out;
		} finally {
			in.close();
		}
	}

	public static DataOutput copy(InputStream in, DataOutput out) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
				out.write(buffer, 0, size);
			}
			return out;
		} finally {
			in.close();
		}
	}

	public static WritableByteChannel copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
		try {
			ByteBuffer bb = ByteBuffer.allocateDirect(BUFFER_SIZE);
			while (in.read(bb) > 0) {
				bb.flip();
				out.write(bb);
				bb.compact();
			}
			for (bb.flip(); bb.hasRemaining();) {
				out.write(bb);
			}
			return out;
		} finally {
			in.close();
		}
	}

	public static ByteBuffer copy(InputStream in, ByteBuffer bb) throws IOException {
		try {
			if (bb.hasArray()) {
				byte[] buffer = bb.array();
				int offset = bb.arrayOffset();
				for (int size, position; bb.hasRemaining()
					&& (size = in.read(buffer, offset + (position = bb.position()), bb.remaining())) > 0;) {
					bb.position(position + size);
				}
			} else {
				int length = Math.min(bb.remaining(), BUFFER_SIZE);
				byte[] buffer = new byte[length];
				for (int size; length > 0 && (size = in.read(buffer, 0, length)) > 0;) {
					bb.put(buffer, 0, size);
					length = Math.min(bb.remaining(), buffer.length);
				}
			}
			return bb;
		} finally {
			in.close();
		}
	}

	public static byte[] copy(InputStream in, byte[] data) throws IOException {
		return copy(in, data, 0, data.length);
	}

	public static byte[] copy(InputStream in, byte[] data, int off, int len) throws IOException {
		try {
			for (int remaining, size; (remaining = len - off) > 0 && (size = in.read(data, off, remaining)) > 0;) {
				off += size;
			}
			return data;
		} finally {
			in.close();
		}
	}

	public static OutputStream copy(ByteBuffer bb, OutputStream out) throws IOException {
		if (out instanceof ByteBufferOutputStream) {
			ByteBufferOutputStream bbout = (ByteBufferOutputStream) out;
			bbout.write(bb);
		} else if (bb.hasArray()) {
			out.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
			bb.position(bb.limit());
		} else {
			int length = Math.min(bb.remaining(), BUFFER_SIZE);
			byte[] buffer = new byte[length];
			while (length > 0) {
				bb.get(buffer, 0, length);
				out.write(buffer, 0, length);
				length = Math.min(bb.remaining(), buffer.length);
			}
		}
		return out;
	}

	public static DataOutput copy(ByteBuffer bb, DataOutput out) throws IOException {
		if (out instanceof ByteBufferDataOutput) {
			ByteBufferDataOutput bbout = (ByteBufferDataOutput) out;
			bbout.write(bb);
		} else if (bb.hasArray()) {
			out.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
			bb.position(bb.limit());
		} else {
			int length = Math.min(bb.remaining(), BUFFER_SIZE);
			byte[] buffer = new byte[length];
			while (length > 0) {
				bb.get(buffer, 0, length);
				out.write(buffer, 0, length);
				length = Math.min(bb.remaining(), buffer.length);
			}
		}
		return out;
	}

	public static MessageDigest copy(URL url, MessageDigest md) throws IOException {
		return copy(stream(url), md);
	}

	public static MessageDigest copy(File file, MessageDigest md) throws IOException {
		return copy(file.toPath(), md);
	}

	public static MessageDigest copy(Path path, MessageDigest md) throws IOException {
		return copy(readChannel(path), md);
	}

	public static MessageDigest copy(URLConnection conn, MessageDigest md) throws IOException {
		return copy(conn.getInputStream(), md);
	}

	public static MessageDigest copy(InputStream in, MessageDigest md) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
				md.update(buffer, 0, size);
			}
			return md;
		} finally {
			in.close();
		}
	}

	public static MessageDigest copy(ReadableByteChannel in, MessageDigest md) throws IOException {
		try {
			ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
			while (in.read(bb) > 0) {
				bb.flip();
				md.update(bb);
				bb.compact();
			}
			for (bb.flip(); bb.hasRemaining();) {
				md.update(bb);
			}
			return md;
		} finally {
			in.close();
		}
	}

	public static File copy(URL url, File file) throws IOException {
		return copy(stream(url), file);
	}

	public static File copy(URLConnection conn, File file) throws IOException {
		return copy(conn.getInputStream(), file);
	}

	public static URL copy(InputStream in, URL url) throws IOException {
		return copy(in, url, null);
	}

	public static URL copy(InputStream in, URL url, String method) throws IOException {
		URLConnection c = url.openConnection();
		HttpURLConnection http = (c instanceof HttpURLConnection) ? (HttpURLConnection) c : null;
		if (http != null && method != null) {
			http.setRequestMethod(method);
		}
		c.setDoOutput(true);
		try (OutputStream out = c.getOutputStream()) {
			copy(in, out);
			return url;
		} finally {
			if (http != null) {
				http.disconnect();
			}
		}
	}

	public static File copy(File src, File tgt) throws IOException {
		copy(src.toPath(), tgt.toPath());
		return tgt;
	}

	public static Path copy(Path src, Path tgt) throws IOException {
		final Path source = src.toAbsolutePath();
		final Path target = tgt.toAbsolutePath();
		if (Files.isRegularFile(source)) {
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			return tgt;
		}
		if (Files.isDirectory(source)) {
			if (Files.notExists(target)) {
				mkdirs(target);
			}
			if (!Files.isDirectory(target))
				throw new IllegalArgumentException("target directory for a directory must be a directory: " + target);
			if (target.startsWith(source))
				throw new IllegalArgumentException("target directory can not be child of source directory.");

			Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
				new FileVisitor<Path>() {
					final FileTime now = FileTime.fromMillis(System.currentTimeMillis());

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						Path targetdir = target.resolve(source.relativize(dir));
						try {
							Files.copy(dir, targetdir);
						} catch (FileAlreadyExistsException e) {
							if (!Files.isDirectory(targetdir))
								throw e;
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Path targetFile = target.resolve(source.relativize(file));
						Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
						Files.setLastModifiedTime(targetFile, now);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						if (exc != null) { // directory iteration failed
							throw exc;
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						if (exc != null) {
							throw exc;
						}
						return FileVisitResult.CONTINUE;
					}
				});
			return tgt;
		}
		throw new FileNotFoundException("During copy: " + source.toString());
	}

	public static File copy(InputStream in, File file) throws IOException {
		copy(in, file.toPath());
		return file;
	}

	public static Path copy(InputStream in, Path path) throws IOException {
		try (FileChannel out = writeChannel(path)) {
			copy(in, out);
		}
		return path;
	}

	public static OutputStream copy(File file, OutputStream out) throws IOException {
		return copy(file.toPath(), out);
	}

	public static OutputStream copy(Path path, OutputStream out) throws IOException {
		return copy(readChannel(path), out);
	}

	public static WritableByteChannel copy(InputStream in, WritableByteChannel out) throws IOException {
		try {
			ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
			byte[] buffer = bb.array();
			for (int size, position; (size = in.read(buffer, position = bb.position(), bb.remaining())) > 0;) {
				bb.position(position + size);
				bb.flip();
				out.write(bb);
				bb.compact();
			}
			for (bb.flip(); bb.hasRemaining();) {
				out.write(bb);
			}
			return out;
		} finally {
			in.close();
		}
	}

	public static OutputStream copy(ReadableByteChannel in, OutputStream out) throws IOException {
		try {
			ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
			byte[] buffer = bb.array();
			for (; in.read(bb) > 0; bb.clear()) {
				out.write(buffer, 0, bb.position());
			}
			return out;
		} finally {
			in.close();
		}
	}

	public static byte[] read(File file) throws IOException {
		try (FileChannel in = readChannel(file.toPath())) {
			ByteBuffer bb = ByteBuffer.allocate((int) in.size());
			while (in.read(bb) > 0) {}
			return bb.array();
		}
	}

	public static ByteBuffer read(Path path) throws IOException {
		try (FileChannel in = readChannel(path)) {
			long size = in.size();
			if (!isWindows && (size > DIRECT_MAP_THRESHOLD)) {
				return in.map(MapMode.READ_ONLY, 0, size);
			}
			ByteBuffer bb = ByteBuffer.allocate((int) size);
			while (in.read(bb) > 0) {}
			bb.flip();
			return bb;
		}
	}

	public static byte[] read(ByteBuffer bb) {
		byte[] data = new byte[bb.remaining()];
		bb.get(data, 0, data.length);
		return data;
	}

	public static byte[] read(URL url) throws IOException {
		URLConnection conn = url.openConnection();
		conn.connect();
		int length = conn.getContentLength();
		if (length == -1) {
			return read(conn.getInputStream());
		}
		return copy(conn.getInputStream(), new byte[length]);
	}

	public static byte[] read(InputStream in) throws IOException {
		return copy(in, new ByteBufferOutputStream()).toByteArray();
	}

	public static void write(byte[] data, OutputStream out) throws IOException {
		copy(data, out);
	}

	public static void write(byte[] data, File file) throws IOException {
		copy(data, file);
	}

	public static String collect(File file) throws IOException {
		return collect(file.toPath(), UTF_8);
	}

	public static String collect(File file, String encoding) throws IOException {
		return collect(file.toPath(), Charset.forName(encoding));
	}

	public static String collect(File file, Charset encoding) throws IOException {
		return collect(file.toPath(), encoding);
	}

	public static String collect(Path path) throws IOException {
		return collect(path, UTF_8);
	}

	public static String collect(Path path, Charset encoding) throws IOException {
		return collect(reader(path, encoding));
	}

	public static String collect(ByteBuffer bb, Charset encoding) {
		return decode(bb, encoding).toString();
	}

	public static String collect(URL url, String encoding) throws IOException {
		return collect(stream(url), Charset.forName(encoding));
	}

	public static String collect(URL url, Charset encoding) throws IOException {
		return collect(stream(url), encoding);
	}

	public static String collect(URL url) throws IOException {
		return collect(url, UTF_8);
	}

	public static String collect(String path) throws IOException {
		return collect(Paths.get(path), UTF_8);
	}

	public static String collect(InputStream in) throws IOException {
		return collect(in, UTF_8);
	}

	public static String collect(InputStream in, String encoding) throws IOException {
		return collect(in, Charset.forName(encoding));
	}

	public static String collect(InputStream in, Charset encoding) throws IOException {
		return collect(reader(in, encoding));
	}

	public static String collect(Reader r) throws IOException {
		StringWriter w = new StringWriter();
		copy(r, w);
		return w.toString();
	}

	/**
	 * Create a temporary file.
	 *
	 * @param directory the directory in which to create the file. Can be null,
	 *            in which case the system TMP directory is used
	 * @param pattern the filename prefix pattern. Must be at least 3 characters
	 *            long
	 * @param suffix the filename suffix. Can be null, in which case (system)
	 *            default suffix is used
	 * @return temp file
	 * @throws IllegalArgumentException when pattern is null or too short
	 * @throws IOException when the specified (non-null) directory is not a
	 *             directory
	 */
	public static File createTempFile(File directory, String pattern, String suffix)
		throws IllegalArgumentException, IOException {
		if ((pattern == null) || (pattern.length() < 3)) {
			throw new IllegalArgumentException(
				"Pattern must be at least 3 characters long, got " + ((pattern == null) ? "null" : pattern.length()));
		}

		if ((directory != null) && !directory.isDirectory()) {
			throw new FileNotFoundException("Directory " + directory + " is not a directory");
		}

		return File.createTempFile(pattern, suffix, directory);
	}

	public static String absolutePath(File file) {
		return normalizePath(file.getAbsolutePath());
	}

	public static String absolutePath(Path path) {
		return normalizePath(path.toAbsolutePath());
	}

	public static String normalizePath(Path path) {
		return normalizePath(path.toString());
	}

	public static String normalizePath(File file) {
		return normalizePath(file.getPath());
	}

	public static String normalizePath(String path) {
		return path.replace(File.separatorChar, '/');
	}

	public static File getFile(String file) {
		return getFile(work, file);
	}

	public static File getFile(File base, String file) {
		StringRover rover = new StringRover(file);
		if (rover.startsWith("~/")) {
			rover.increment(2);
			if (!rover.startsWith("~/")) {
				return getFile(home, rover.substring(0));
			}
		}
		if (rover.startsWith("~")) {
			return getFile(home.getParentFile(), rover.substring(1));
		}

		File f = new File(rover.substring(0));
		if (f.isAbsolute()) {
			return f;
		}

		if (base == null) {
			base = work;
		}

		for (f = base.getAbsoluteFile(); !rover.isEmpty();) {
			int n = rover.indexOf('/');
			if (n < 0) {
				n = rover.length();
			}
			if ((n == 0) || ((n == 1) && (rover.charAt(0) == '.'))) {
				// case "" or "."
			} else if ((n == 2) && (rover.charAt(0) == '.') && (rover.charAt(1) == '.')) {
				// case ".."
				File parent = f.getParentFile();
				if (parent != null) {
					f = parent;
				}
			} else {
				String segment = rover.substring(0, n);
				f = new File(f, segment);
			}
			rover.increment(n + 1);
		}

		return f.getAbsoluteFile();
	}

	public static File getBasedFile(File base, String file) throws IOException {
		base = base.getCanonicalFile();
		File child = getFile(base, file);
		if (child.getCanonicalPath()
			.startsWith(base.getCanonicalPath())) {
			return child;
		}
		throw new IOException("The file " + child + " is outside of the base " + base);
	}

	public static Path getPath(String file) {
		return getPath(work.toPath(), file);
	}

	public static Path getPath(Path base, String file) {
		StringRover rover = new StringRover(file);
		if (rover.startsWith("~/")) {
			rover.increment(2);
			if (!rover.startsWith("~/")) {
				return getPath(home.toPath(), rover.substring(0));
			}
		}
		if (rover.startsWith("~")) {
			return getPath(home.toPath()
				.getParent(), rover.substring(1));
		}

		Path f = new File(rover.substring(0)).toPath();
		if (f.isAbsolute()) {
			return f;
		}

		if (base == null) {
			base = work.toPath();
		}

		for (f = base.normalize()
			.toAbsolutePath(); !rover.isEmpty();) {
			int n = rover.indexOf('/');
			if (n < 0) {
				n = rover.length();
			}
			String segment = rover.substring(0, n);
			f = f.resolve(segment)
				.normalize();
			rover.increment(n + 1);
		}

		return f.toAbsolutePath();
	}

	public static Path getBasedPath(Path base, String file) throws IOException {
		base = base.normalize()
			.toAbsolutePath();
		Path child = getPath(base, file);
		if (child.startsWith(base)) {
			return child;
		}
		throw new IOException("The file " + child + " is outside of the base " + base);
	}

	/**
	 * Deletes the specified file. Folders are recursively deleted.<br>
	 * If file(s) cannot be deleted, no feedback is provided (fail silently).
	 *
	 * @param file file to be deleted
	 */
	public static void delete(File file) {
		delete(file.toPath());
	}

	/**
	 * Deletes the specified path. Folders are recursively deleted.<br>
	 * If file(s) cannot be deleted, no feedback is provided (fail silently).
	 *
	 * @param path path to be deleted
	 */
	public static void delete(Path path) {
		try {
			deleteWithException(path);
		} catch (IOException e) {
			// Ignore a failed delete
		}
	}

	/**
	 * Deletes and creates directories
	 */
	public static void initialize(File dir) {
		try {
			deleteWithException(dir);
			mkdirs(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Deletes the specified file. Folders are recursively deleted.<br>
	 * Throws exception if any of the files could not be deleted.
	 *
	 * @param file file to be deleted
	 * @throws IOException if the file (or contents of a folder) could not be
	 *             deleted
	 */
	public static void deleteWithException(File file) throws IOException {
		deleteWithException(file.toPath());
	}

	/**
	 * Deletes the specified path. Folders are recursively deleted.<br>
	 * Throws exception if any of the files could not be deleted.
	 *
	 * @param path path to be deleted
	 * @throws IOException if the path (or contents of a folder) could not be
	 *             deleted
	 */
	public static void deleteWithException(Path path) throws IOException {
		path = path.toAbsolutePath();
		if (Files.notExists(path) && !isSymbolicLink(path)) {
			return;
		}
		if (path.equals(path.getRoot()))
			throw new IllegalArgumentException("Cannot recursively delete root for safety reasons");

		Files.walkFileTree(path, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				try {
					Files.delete(file);
				} catch (IOException e) {
					throw exc;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) { // directory iteration failed
					throw exc;
				}
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Renames <code>from</code> to <code>to</code> replacing the target file if
	 * necessary.
	 *
	 * @param from source file
	 * @param to destination file
	 * @throws IOException if the rename operation fails
	 */
	public static File rename(File from, File to) throws IOException {
		return rename(from.toPath(), to.toPath()).toFile();
	}

	/**
	 * Renames <code>from</code> to <code>to</code> replacing the target file if
	 * necessary.
	 *
	 * @param from source path
	 * @param to destination path
	 * @throws IOException if the rename operation fails
	 */
	public static Path rename(Path from, Path to) throws IOException {
		try {
			return Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			return Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static File mkdirs(File dir) throws IOException {
		return mkdirs(dir.toPath()).toFile();
	}

	public static Path mkdirs(Path dir) throws IOException {
		if (Files.isSymbolicLink(dir)) {
			Path target = Files.readSymbolicLink(dir);
			boolean recreateSymlink = isWindows() && !Files.exists(target, LinkOption.NOFOLLOW_LINKS);
			Path result = mkdirs(target);
			if (recreateSymlink) { // recreate symlink on windows
				delete(dir);
				createSymbolicLink(dir, target);
			}
			return result;
		}
		return Files.createDirectories(dir);
	}

	public static long drain(InputStream in) throws IOException {
		try {
			long result = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
				result += size;
			}
			return result;
		} finally {
			in.close();
		}
	}

	public static OutputStream copy(Collection<?> c, OutputStream out) throws IOException {
		PrintWriter pw = writer(out);
		try {
			for (Object o : c) {
				pw.println(o);
			}
			return out;
		} finally {
			pw.flush();
		}
	}

	public static Throwable close(AutoCloseable in) {
		try {
			if (in != null)
				in.close();
		} catch (Throwable e) {
			return e;
		}
		return null;
	}

	// This method is required for binary backwards compatibility.
	public static Throwable close(Closeable in) {
		return close((AutoCloseable) in);
	}

	public static URL toURL(String s, File base) throws MalformedURLException {
		int n = s.indexOf(':');
		if (n > 0 && n < 10) {
			// is url
			return new URL(s);
		}
		return getFile(base, s).toURI()
			.toURL();
	}

	public static void store(Object o, File file) throws IOException {
		store(o, file.toPath(), UTF_8);
	}

	public static void store(Object o, File file, String encoding) throws IOException {
		store(o, file.toPath(), Charset.forName(encoding));
	}

	public static void store(Object o, Path path, Charset encoding) throws IOException {
		try (FileChannel ch = writeChannel(path)) {
			if (o != null) {
				try (Writer w = Channels.newWriter(ch, encoding.newEncoder(), -1)) {
					w.write(o.toString());
				}
			}
		}
	}

	public static void store(Object o, OutputStream out) throws IOException {
		store(o, out, UTF_8);
	}

	public static void store(Object o, OutputStream out, String encoding) throws IOException {
		store(o, out, Charset.forName(encoding));
	}

	public static void store(Object o, OutputStream out, Charset encoding) throws IOException {
		Writer w = writer(out, encoding);
		try {
			store(o, w);
		} finally {
			w.flush();
		}
	}

	public static void store(Object o, Writer w) throws IOException {
		if (o != null) {
			w.write(o.toString());
		}
	}

	/**
	 * Store output in a file but ensure that the content is updated atomically.
	 * To ensure this, the file is first copied to a temporary file in the same
	 * directory as the target. It is then renamed which will first attempt an
	 * atomic move but will always replace.
	 *
	 * @param store the function provide the output
	 * @param target the file to store it, parent directories will be created if
	 *            necessary
	 */
	public static void store(ConsumerWithException<OutputStream> store, File target) throws Exception {

		target.getParentFile()
			.mkdirs();

		File tmp = createTempFile(target.getParentFile(), target.getName(), ".tmp");
		try {

			try (OutputStream outputStream = outputStream(tmp)) {
				store.accept(outputStream);
			}
			rename(tmp, target);

			assert target.isFile();

		} finally {
			if (tmp.exists())
				tmp.delete();
		}
	}

	public static InputStream stream(byte[] data) {
		return stream(ByteBuffer.wrap(data));
	}

	public static InputStream stream(ByteBuffer bb) {
		return new ByteBufferInputStream(bb);
	}

	public static InputStream stream(String s) {
		return stream(s, UTF_8);
	}

	public static InputStream stream(String s, String encoding) {
		return stream(s, Charset.forName(encoding));
	}

	public static InputStream stream(String s, Charset encoding) {
		return stream(s.getBytes(encoding));
	}

	public static InputStream stream(File file) throws IOException {
		return stream(file.toPath());
	}

	public static InputStream stream(Path path) throws IOException {
		return Files.newInputStream(path);
	}

	public static InputStream stream(URL url) throws IOException {
		return url.openStream();
	}

	public static FileChannel readChannel(Path path) throws IOException {
		return FileChannel.open(path, readOptions);
	}

	public static OutputStream outputStream(File file) throws IOException {
		return outputStream(file.toPath());
	}

	public static OutputStream outputStream(Path path) throws IOException {
		return Files.newOutputStream(path);
	}

	public static FileChannel writeChannel(Path path) throws IOException {
		return FileChannel.open(path, writeOptions);
	}

	public static CharBuffer decode(ByteBuffer bb, Charset encoding) {
		return encoding.decode(bb);
	}

	public static ByteBuffer encode(CharBuffer cb, Charset encoding) {
		return encoding.encode(cb);
	}

	public static BufferedReader reader(String s) {
		return new BufferedReader(new StringReader(s));
	}

	public static BufferedReader reader(File file) throws IOException {
		return reader(file.toPath(), UTF_8);
	}

	public static BufferedReader reader(File file, String encoding) throws IOException {
		return reader(file.toPath(), Charset.forName(encoding));
	}

	public static BufferedReader reader(File file, Charset encoding) throws IOException {
		return reader(file.toPath(), encoding);
	}

	public static BufferedReader reader(Path path, Charset encoding) throws IOException {
		return reader(readChannel(path), encoding);
	}

	public static BufferedReader reader(ByteBuffer bb, Charset encoding) {
		return reader(new ByteBufferInputStream(bb), encoding);
	}

	public static BufferedReader reader(CharBuffer cb) {
		return new BufferedReader(new CharBufferReader(cb));
	}

	public static BufferedReader reader(ReadableByteChannel in, Charset encoding) {
		return new BufferedReader(Channels.newReader(in, encoding.newDecoder(), -1));
	}

	public static BufferedReader reader(InputStream in) {
		return reader(in, UTF_8);
	}

	public static BufferedReader reader(InputStream in, String encoding) {
		return reader(in, Charset.forName(encoding));
	}

	public static BufferedReader reader(InputStream in, Charset encoding) {
		return new BufferedReader(new InputStreamReader(in, encoding));
	}

	public static PrintWriter writer(File file) throws IOException {
		return writer(file.toPath(), UTF_8);
	}

	public static PrintWriter writer(File file, String encoding) throws IOException {
		return writer(file.toPath(), Charset.forName(encoding));
	}

	public static PrintWriter writer(File file, Charset encoding) throws IOException {
		return writer(file.toPath(), encoding);
	}

	public static PrintWriter writer(Path path) throws IOException {
		return writer(path, UTF_8);
	}

	public static PrintWriter writer(Path path, Charset encoding) throws IOException {
		return writer(writeChannel(path), encoding);
	}

	public static PrintWriter writer(WritableByteChannel out, Charset encoding) {
		return new PrintWriter(Channels.newWriter(out, encoding.newEncoder(), -1));
	}

	public static PrintWriter writer(OutputStream out) {
		return writer(out, UTF_8);
	}

	public static PrintWriter writer(OutputStream out, String encoding) {
		return writer(out, Charset.forName(encoding));
	}

	public static PrintWriter writer(OutputStream out, Charset encoding) {
		return new PrintWriter(new OutputStreamWriter(out, encoding));
	}

	public static boolean createSymbolicLink(File link, File target) throws IOException {
		return createSymbolicLink(link.toPath(), target.toPath());
	}

	public static boolean createSymbolicLink(Path link, Path target) throws IOException {
		if (isSymbolicLink(link)) {
			Path linkTarget = Files.readSymbolicLink(link);

			if (target.equals(linkTarget)) {
				return true;
			} else {
				Files.delete(link);
			}
		}

		try {
			Files.createSymbolicLink(link, target);
			return true;
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	public static boolean isSymbolicLink(File link) {
		return isSymbolicLink(link.toPath());
	}

	public static boolean isSymbolicLink(Path link) {
		return Files.isSymbolicLink(link);
	}

	/**
	 * Creates a symbolic link from {@code link} to the {@code target}, or
	 * copies {@code target} to {@code link} if running on Windows.
	 * <p>
	 * Creating symbolic links on Windows requires administrator permissions, so
	 * copying is a safer fallback. Copy only happens if timestamp and and file
	 * length are different than target
	 *
	 * @param link the location of the symbolic link, or destination of the
	 *            copy.
	 * @param target the source of the symbolic link, or source of the copy.
	 * @return {@code true} if the operation succeeds, {@code false} otherwise.
	 */
	public static boolean createSymbolicLinkOrCopy(File link, File target) {
		return createSymbolicLinkOrCopy(link.toPath(), target.toPath());
	}

	/**
	 * Creates a symbolic link from {@code link} to the {@code target}, or
	 * copies {@code target} to {@code link} if running on Windows.
	 * <p>
	 * Creating symbolic links on Windows requires administrator permissions, so
	 * copying is a safer fallback. Copy only happens if timestamp and and file
	 * length are different than target
	 *
	 * @param link the location of the symbolic link, or destination of the
	 *            copy.
	 * @param target the source of the symbolic link, or source of the copy.
	 * @return {@code true} if the operation succeeds, {@code false} otherwise.
	 */
	public static boolean createSymbolicLinkOrCopy(Path link, Path target) {
		try {
			if (isWindows || !createSymbolicLink(link, target)) {
				// only copy if target length and timestamp differ
				BasicFileAttributes targetAttrs = Files.readAttributes(target, BasicFileAttributes.class);
				try {
					BasicFileAttributes linkAttrs = Files.readAttributes(link, BasicFileAttributes.class);
					if (targetAttrs.lastModifiedTime()
						.equals(linkAttrs.lastModifiedTime()) && targetAttrs.size() == linkAttrs.size()) {
						return true;
					}
				} catch (IOException e) {
					// link does not exist
				}
				copy(target, link);
				Files.setLastModifiedTime(link, targetAttrs.lastModifiedTime());
			}
			return true;
		} catch (Exception ignore) {
			// ignore
		}
		return false;
	}

	static final public OutputStream	nullStream	=						//
		new OutputStream() {
			@Override
			public void write(int var0) {}

			@Override
			public void write(byte[] var0) {}

			@Override
			public void write(byte[] var0, int from, int l) {}

			@Override
			public void close() {}

			@Override
			public void flush() {}
		};

	static final public Writer			nullWriter	=						//
		new Writer() {
			@Override
			public Writer append(char var0) {
				return this;
			}

			@Override
			public Writer append(CharSequence var0) {
				return this;
			}

			@Override
			public Writer append(CharSequence var0, int var1, int var2) {
				return this;
			}

			@Override
			public void write(int var0) {}

			@Override
			public void write(String var0) {}

			@Override
			public void write(String var0, int var1, int var2) {}

			@Override
			public void write(char[] var0) {}

			@Override
			public void write(char[] var0, int var1, int var2) {}

			@Override
			public void close() {}

			@Override
			public void flush() {}
		};

	public static String toSafeFileName(String string) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c < ' ')
				continue;

			if (isWindows) {
				switch (c) {
					case '<' :
					case '>' :
					case '"' :
					case '/' :
					case '\\' :
					case '|' :
					case '*' :
					case ':' :
						sb.append('%');
						break;
					default :
						sb.append(c);
				}
			} else {
				switch (c) {
					case '/' :
						sb.append('%');
						break;
					default :
						sb.append(c);
				}
			}
		}
		if (sb.length() == 0 || (isWindows && RESERVED_WINDOWS_P.matcher(sb)
			.matches()))
			sb.append("_");

		return sb.toString();
	}

	private final static Pattern RESERVED_WINDOWS_P = Pattern.compile("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]");

	public static boolean isWindows() {
		return isWindows;
	}

	/*
	 * This class calculates the home path. This class uses environment
	 * variables so that makes it hard to test. For this reason tests can
	 * override the #getenv method.
	 */
	static class EnvironmentCalculator {
		private boolean iswindows;

		public EnvironmentCalculator(boolean iswindows) {
			this.iswindows = iswindows;
		}

		/**
		 * Get the value of a system environment variable. Expand any macros
		 * (%...%) if run on windows. Generally, on Linux et. al. environment
		 * variables are already expanded.
		 *
		 * @param key the environment variable name
		 * @return the value with expanded macros if on windows.
		 */
		String getSystemEnv(String key) {
			return getSystemEnv(key, null);
		}

		private String getSystemEnv(String key, Set<String> visited) {
			String value = getenv(key);
			if (value == null || !iswindows) {
				return value;
			}
			if (visited == null) {
				visited = new HashSet<>();
			}
			if (!visited.add(key)) {
				return key;
			}

			StringBuilder sb = new StringBuilder();
			Matcher matcher = WINDOWS_MACROS.matcher(value);
			int start = 0;
			for (; matcher.find(); start = matcher.end()) {
				String name = matcher.group(1);
				String replacement = getSystemEnv(name, visited);
				sb.append(value, start, matcher.start())
					.append(replacement);
			}
			return (start == 0) ? value
				: sb.append(value, start, value.length())
					.toString();
		}

		String getenv(String key) {
			return System.getenv(key);
		}

		File getHome() {
			File home = testFile(getSystemEnv("HOME"));
			if ((home == null) || !home.isDirectory()) {
				home = testFile(System.getProperty("user.home"));
			}
			assert home != null;
			return home;
		}

		File getJavaHome() {
			File javaHome = testFile(getSystemEnv("JAVA_HOME"));
			if ((javaHome == null) || !javaHome.isDirectory()) {
				javaHome = testFile(System.getProperty("java.home"));
			}
			assert javaHome != null;
			return javaHome;
		}

		private File testFile(String path) {
			if (path == null)
				return null;

			return new File(path);
		}
	}

	public static String readUTF(DataInput in) throws IOException {
		int size = in.readUnsignedShort();
		char[] string = new char[size];
		int len = 0;
		for (int i = 0; i < size; i++, len++) {
			int b = in.readUnsignedByte();
			if ((b > 0x00) && (b < 0x80)) {
				string[len] = (char) b;
			} else {
				switch (b >> 4) {
					// 2 byte encoding
					case 0b1100 :
					case 0b1101 : {
						i++;
						if (i >= size) {
							throw new UTFDataFormatException("partial multi byte charater at end");
						}
						int b2 = in.readUnsignedByte();
						if ((b2 & 0b1100_0000) != 0b1000_0000) {
							throw new UTFDataFormatException("bad encoding at byte: " + (i - 1));
						}
						string[len] = (char) (((b & 0x1F) << 6) | (b2 & 0x3F));
						break;
					}
					// 3 byte encoding
					case 0b1110 : {
						i += 2;
						if (i >= size) {
							throw new UTFDataFormatException("partial multi byte charater at end");
						}
						int b2 = in.readUnsignedByte();
						int b3 = in.readUnsignedByte();
						if (((b2 & 0b1100_0000) != 0b1000_0000) || ((b3 & 0b1100_0000) != 0b1000_0000)) {
							throw new UTFDataFormatException("bad encoding at byte: " + (i - 2));
						}
						string[len] = (char) (((b & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F));
						break;
					}
					// invalid encoding
					default : {
						throw new UTFDataFormatException("bad encoding at byte: " + i);
					}
				}
			}
		}
		return new String(string, 0, len);
	}

}
