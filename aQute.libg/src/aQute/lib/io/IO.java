package aQute.lib.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
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
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import aQute.libg.glob.Glob;

public class IO {
	static final int			BUFFER_SIZE	= IOConstants.PAGE_SIZE * 16;

	static final public File	work		= new File(System.getProperty("user.dir"));
	static final public File	home;
	private static final EnumSet<StandardOpenOption>	writeOptions	= EnumSet.of(StandardOpenOption.WRITE,
			StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	private static final EnumSet<StandardOpenOption>	readOptions		= EnumSet.of(StandardOpenOption.READ);
	static {
		File tmp = null;
		try {
			tmp = new File(System.getenv("HOME"));
		} catch (Exception e) {}
		if (tmp == null) {
			tmp = new File(System.getProperty("user.home"));
		}
		home = tmp;
	}

	public static String getExtension(String fileName, String deflt) {
		int n = fileName.lastIndexOf('.');
		if (n < 0)
			return deflt;

		return fileName.substring(n + 1);
	}

	public static Collection<File> tree(File current) {
		Set<File> files = new LinkedHashSet<File>();
		traverse(files, current, null);
		return files;
	}

	public static Collection<File> tree(File current, String glob) {
		Set<File> files = new LinkedHashSet<File>();
		traverse(files, current, glob == null ? null : new Glob(glob));
		return files;
	}

	private static void traverse(Collection<File> files, File current, Glob glob) {
		if (current.isFile() && (glob == null || glob.matcher(current.getName()).matches())) {
			files.add(current);
		} else if (current.isDirectory()) {
			for (File sub : current.listFiles()) {
				traverse(files, sub, glob);
			}
		}
	}

	public static void copy(byte[] data, File file) throws IOException {
		copy(data, file.toPath());
	}

	public static void copy(byte[] data, Path path) throws IOException {
		try (FileChannel out = writeChannel(path)) {
			ByteBuffer buffer = ByteBuffer.wrap(data);
			while (buffer.hasRemaining()) {
				out.write(buffer);
			}
		}
	}

	public static void copy(byte[] r, Writer w) throws IOException {
		w.write(new String(r, UTF_8));
		w.flush();
	}

	public static void copy(byte[] r, OutputStream w) throws IOException {
		w.write(r);
	}

	public static void copy(Reader r, Writer w) throws IOException {
		try {
			char buffer[] = new char[BUFFER_SIZE];
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

	public static void copy(Reader r, OutputStream o) throws IOException {
		copy(r, o, "UTF-8");
	}

	public static void copy(Reader r, OutputStream o, String charset) throws IOException {
		try {
			copy(r, new OutputStreamWriter(o, charset));
		} finally {
			r.close();
		}
	}

	public static void copy(InputStream r, Writer w) throws IOException {
		copy(r, w, "UTF-8");
	}

	public static void copy(InputStream r, Writer w, String charset) throws IOException {
		copy(new InputStreamReader(r, charset), w);
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int size = in.read(buffer);
			while (size > 0) {
				out.write(buffer, 0, size);
				size = in.read(buffer);
			}
		} finally {
			in.close();
		}
	}

	public static void copy(InputStream in, DataOutput out) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int size = in.read(buffer);
			while (size > 0) {
				out.write(buffer, 0, size);
				size = in.read(buffer);
			}
		} finally {
			in.close();
		}
	}

	public static void copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
			while (in.read(buffer) != -1) {
				buffer.flip();
				out.write(buffer);
				buffer.compact();
			}
			buffer.flip();
			while (buffer.hasRemaining()) {
				out.write(buffer);
			}
		} finally {
			in.close();
		}
	}

	public static void copy(InputStream in, ByteBuffer bb) throws IOException {
		try {
			boolean hasArray = bb.hasArray();
			byte[] buffer = hasArray ? bb.array() : new byte[bb.remaining()];
			while (bb.hasRemaining()) {
				int pos = hasArray ? bb.position() : 0;
				int size = in.read(buffer, pos, bb.remaining());
				if (size <= 0) {
					break;
				}
				if (hasArray) {
					bb.position(pos + size);
				} else {
					bb.put(buffer, pos, size);
				}
			}
		} finally {
			in.close();
		}
	}

	public static void copy(URL in, MessageDigest md) throws IOException {
		copy(stream(in), md);
	}

	public static void copy(File in, MessageDigest md) throws IOException {
		copy(in.toPath(), md);
	}

	public static void copy(Path path, MessageDigest md) throws IOException {
		copy(readChannel(path), md);
	}

	public static void copy(URLConnection in, MessageDigest md) throws IOException {
		copy(in.getInputStream(), md);
	}

	public static void copy(InputStream in, MessageDigest md) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int size = in.read(buffer);
			while (size > 0) {
				md.update(buffer, 0, size);
				size = in.read(buffer);
			}
		} finally {
			in.close();
		}
	}

	public static void copy(ReadableByteChannel in, MessageDigest md) throws IOException {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			while (in.read(buffer) != -1) {
				buffer.flip();
				md.update(buffer);
				buffer.compact();
			}
			buffer.flip();
			while (buffer.hasRemaining()) {
				md.update(buffer);
			}
		} finally {
			in.close();
		}
	}

	public static void copy(URL url, File file) throws IOException {
		copy(stream(url), file);
	}

	public static void copy(URLConnection c, File file) throws IOException {
		copy(c.getInputStream(), file);
	}

	public static void copy(InputStream in, URL out) throws IOException {
		copy(in, out, null);
	}

	public static void copy(InputStream in, URL out, String method) throws IOException {
		URLConnection c = out.openConnection();
		HttpURLConnection http = (c instanceof HttpURLConnection) ? (HttpURLConnection) c : null;
		if (http != null && method != null) {
			http.setRequestMethod(method);
		}
		c.setDoOutput(true);
		try (OutputStream os = c.getOutputStream()) {
			copy(in, os);
		} finally {
			if (http != null) {
				http.disconnect();
			}
		}
	}

	public static void copy(File a, File b) throws IOException {
		copy(a.toPath(), b.toPath());
	}

	public static void copy(Path a, Path b) throws IOException {
		final Path source = a.toAbsolutePath();
		final Path target = b.toAbsolutePath();
		if (Files.isRegularFile(source)) {
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			return;
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
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
								throws IOException {
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
			return;
		}
		throw new FileNotFoundException("During copy: " + source.toString());
	}

	public static void copy(InputStream a, File b) throws IOException {
		copy(a, b.toPath());
	}

	public static void copy(InputStream in, Path path) throws IOException {
		try (FileChannel out = writeChannel(path)) {
			copy(Channels.newChannel(in), out);
		}
	}

	public static void copy(File a, OutputStream b) throws IOException {
		copy(a.toPath(), b);
	}

	public static void copy(Path path, OutputStream out) throws IOException {
		copy(readChannel(path), Channels.newChannel(out));
	}

	public static byte[] read(File f) throws IOException {
		ByteBuffer buffer = read(f.toPath());
		return buffer.array();
	}

	public static ByteBuffer read(Path path) throws IOException {
		try (FileChannel in = readChannel(path)) {
			ByteBuffer buffer = ByteBuffer.allocate((int) in.size());
			while (in.read(buffer) > 0) {}
			buffer.flip();
			return buffer;
		}
	}

	public static byte[] read(URL u) throws IOException {
		return read(stream(u));
	}

	public static byte[] read(InputStream in) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		copy(in, bout);
		return bout.toByteArray();
	}

	public static void write(byte[] data, OutputStream out) throws Exception {
		copy(data, out);
	}

	public static void write(byte[] data, File out) throws Exception {
		copy(data, out);
	}

	public static String collect(File a) throws IOException {
		return collect(a.toPath(), UTF_8);
	}

	public static String collect(File a, String encoding) throws IOException {
		return collect(a.toPath(), Charset.forName(encoding));
	}

	public static String collect(Path path) throws IOException {
		return collect(path, UTF_8);
	}

	public static String collect(Path in, Charset encoding) throws IOException {
		return encoding.decode(read(in)).toString();
	}

	public static String collect(URL a, String encoding) throws IOException {
		return collect(stream(a), encoding);
	}

	public static String collect(URL a) throws IOException {
		return collect(a, "UTF-8");
	}

	public static String collect(String a) throws IOException {
		return collect(Paths.get(a), UTF_8);
	}

	public static String collect(InputStream a) throws IOException {
		return collect(a, "UTF-8");
	}

	public static String collect(InputStream a, String encoding) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(a, out);
		return out.toString(encoding);
	}

	public static String collect(Reader a) throws IOException {
		StringWriter sw = new StringWriter();
		copy(a, sw);
		return sw.toString();
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
			throw new IllegalArgumentException("Pattern must be at least 3 characters long, got "
					+ ((pattern == null) ? "null" : pattern.length()));
		}

		if ((directory != null) && !directory.isDirectory()) {
			throw new FileNotFoundException("Directory " + directory + " is not a directory");
		}

		return File.createTempFile(pattern, suffix, directory);
	}

	public static File getFile(String filename) {
		return getFile(work, filename);
	}

	public static File getFile(File base, String file) {

		if (file.startsWith("~/")) {
			file = file.substring(2);
			if (!file.startsWith("~/")) {
				return getFile(home, file);
			}
		}
		if (file.startsWith("~")) {
			file = file.substring(1);
			return getFile(home.getParentFile(), file);
		}

		File f = new File(file);
		if (f.isAbsolute())
			return f;
		int n;

		if (base == null)
			base = work;

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

	/**
	 * Deletes the specified file. Folders are recursively deleted.<br>
	 * If file(s) cannot be deleted, no feedback is provided (fail silently).
	 *
	 * @param f file to be deleted
	 */
	public static void delete(File f) {
		delete(f.toPath());
	}

	/**
	 * Deletes the specified path. Folders are recursively deleted.<br>
	 * If file(s) cannot be deleted, no feedback is provided (fail silently).
	 *
	 * @param p path to be deleted
	 */
	public static void delete(Path p) {
		try {
			deleteWithException(p);
		} catch (IOException e) {
			// Ignore a failed delete
		}
	}

	/**
	 * Deletes and creates directories
	 */
	public static void initialize(File f) {
		try {
			deleteWithException(f);
			mkdirs(f);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Deletes the specified file. Folders are recursively deleted.<br>
	 * Throws exception if any of the files could not be deleted.
	 *
	 * @param f file to be deleted
	 * @throws IOException if the file (or contents of a folder) could not be
	 *             deleted
	 */
	public static void deleteWithException(File f) throws IOException {
		deleteWithException(f.toPath());
	}

	/**
	 * Deletes the specified path. Folders are recursively deleted.<br>
	 * Throws exception if any of the files could not be deleted.
	 *
	 * @param p path to be deleted
	 * @throws IOException if the path (or contents of a folder) could not be
	 *             deleted
	 */
	public static void deleteWithException(Path p) throws IOException {
		p = p.toAbsolutePath();
		if (Files.notExists(p) && !isSymbolicLink(p)) {
			return;
		}
		if (p.equals(p.getRoot()))
			throw new IllegalArgumentException("Cannot recursively delete root for safety reasons");

		Files.walkFileTree(p, new FileVisitor<Path>() {
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
	public static void rename(File from, File to) throws IOException {
		rename(from.toPath(), to.toPath());
	}

	/**
	 * Renames <code>from</code> to <code>to</code> replacing the target file if
	 * necessary.
	 *
	 * @param from source path
	 * @param to destination path
	 * @throws IOException if the rename operation fails
	 */
	public static void rename(Path from, Path to) throws IOException {
		Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
	}

	public static void mkdirs(File dir) throws IOException {
		mkdirs(dir.toPath());
	}

	public static void mkdirs(Path dir) throws IOException {
		Files.createDirectories(dir);
	}

	public static long drain(InputStream in) throws IOException {
		try {
			long result = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			int size = in.read(buffer);
			while (size >= 0) {
				result += size;
				size = in.read(buffer);
			}
			return result;
		} finally {
			in.close();
		}
	}

	public static void copy(Collection< ? > c, OutputStream out) throws IOException {
		PrintWriter pw = writer(out);
		try {
			for (Object o : c) {
				pw.println(o);
			}
		} finally {
			pw.flush();
		}
	}

	public static Throwable close(Closeable in) {
		try {
			if (in != null)
				in.close();
		} catch (Throwable e) {
			return e;
		}
		return null;
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
		try (FileChannel ch = writeChannel(out.toPath())) {
			if (o != null) {
				try (Writer w = Channels.newWriter(ch, encoding)) {
					w.write(o.toString());
				}
			}
		}
	}

	public static void store(Object o, OutputStream fout) throws IOException {
		store(o, fout, "UTF-8");
	}

	public static void store(Object o, OutputStream fout, String encoding) throws IOException {
		try {
			if (o != null) {
				copy(o.toString().getBytes(encoding), fout);
			}
		} finally {
			fout.close();
		}
	}

	public static InputStream stream(String s) {
		try {
			return stream(s, "UTF-8");
		} catch (IOException e) {
			// Ignore
			return null;
		}
	}

	public static InputStream stream(String s, String encoding) throws IOException {
		return new ByteArrayInputStream(s.getBytes(encoding));
	}

	public static InputStream stream(File f) throws IOException {
		return stream(f.toPath());
	}

	public static InputStream stream(Path p) throws IOException {
		return Files.newInputStream(p);
	}

	public static InputStream stream(URL s) throws IOException {
		return s.openStream();
	}

	public static FileChannel readChannel(Path p) throws IOException {
		return FileChannel.open(p, readOptions);
	}

	public static OutputStream outputStream(File f) throws IOException {
		return outputStream(f.toPath());
	}

	public static OutputStream outputStream(Path p) throws IOException {
		return Files.newOutputStream(p);
	}

	public static FileChannel writeChannel(Path p) throws IOException {
		return FileChannel.open(p, writeOptions);
	}

	public static BufferedReader reader(String s) {
		return new BufferedReader(new StringReader(s));
	}

	public static BufferedReader reader(File f) throws IOException {
		return reader(f.toPath(), UTF_8);
	}

	public static BufferedReader reader(File f, String encoding) throws IOException {
		return reader(f.toPath(), Charset.forName(encoding));
	}

	public static BufferedReader reader(File f, Charset encoding) throws IOException {
		return reader(f.toPath(), encoding);
	}

	public static BufferedReader reader(Path path, Charset encoding) throws IOException {
		return reader(readChannel(path), encoding);
	}

	public static BufferedReader reader(ReadableByteChannel in, Charset encoding) throws IOException {
		return new BufferedReader(Channels.newReader(in, encoding.newDecoder(), -1));
	}

	public static BufferedReader reader(InputStream in) throws IOException {
		return reader(in, UTF_8);
	}

	public static BufferedReader reader(InputStream in, String encoding) throws IOException {
		return new BufferedReader(new InputStreamReader(in, encoding));
	}

	public static BufferedReader reader(InputStream in, Charset encoding) throws IOException {
		return new BufferedReader(new InputStreamReader(in, encoding));
	}

	public static PrintWriter writer(File f) throws IOException {
		return writer(f.toPath(), UTF_8);
	}

	public static PrintWriter writer(File f, String encoding) throws IOException {
		return writer(f.toPath(), Charset.forName(encoding));
	}

	public static PrintWriter writer(File f, Charset encoding) throws IOException {
		return writer(f.toPath(), encoding);
	}

	public static PrintWriter writer(Path path, Charset encoding) throws IOException {
		return writer(writeChannel(path), encoding);
	}

	public static PrintWriter writer(WritableByteChannel out, Charset encoding) throws IOException {
		return new PrintWriter(Channels.newWriter(out, encoding.newEncoder(), -1));
	}

	public static PrintWriter writer(OutputStream out) throws IOException {
		return writer(out, UTF_8);
	}

	public static PrintWriter writer(OutputStream out, String encoding) throws IOException {
		return new PrintWriter(new OutputStreamWriter(out, encoding));
	}

	public static PrintWriter writer(OutputStream out, Charset encoding) throws IOException {
		return new PrintWriter(new OutputStreamWriter(out, encoding));
	}

	public static boolean createSymbolicLink(File link, File target) throws Exception {
		return createSymbolicLink(link.toPath(), target.toPath());
	}

	public static boolean createSymbolicLink(Path link, Path target) throws Exception {
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
			if (isWindows() || !createSymbolicLink(link, target)) {
				// only copy if target length and timestamp differ
				BasicFileAttributes targetAttrs = Files.readAttributes(target, BasicFileAttributes.class);
				try {
					BasicFileAttributes linkAttrs = Files.readAttributes(link, BasicFileAttributes.class);
					if (targetAttrs.lastModifiedTime().equals(linkAttrs.lastModifiedTime())
							&& targetAttrs.size() == linkAttrs.size()) {
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

	static final public OutputStream	nullStream	= new OutputStream() {

												@Override
												public void write(int var0) throws IOException {}

												@Override
												public void write(byte[] var0) throws IOException {}

												@Override
												public void write(byte[] var0, int from, int l) throws IOException {}
											};
	static final public Writer			nullWriter	= new Writer() {
												public java.io.Writer append(char var0) throws java.io.IOException {
													return null;
												}

												public java.io.Writer append(java.lang.CharSequence var0)
														throws java.io.IOException {
													return null;
												}

												public java.io.Writer append(java.lang.CharSequence var0, int var1,
														int var2) throws java.io.IOException {
													return null;
												}

												public void write(int var0) throws java.io.IOException {}

												public void write(java.lang.String var0) throws java.io.IOException {}

												public void write(java.lang.String var0, int var1, int var2)
														throws java.io.IOException {}

												public void write(char[] var0) throws java.io.IOException {}

												public void write(char[] var0, int var1, int var2)
														throws java.io.IOException {}

												public void close() throws IOException {}

												public void flush() throws IOException {}
											};

	public static String toSafeFileName(String string) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c < ' ')
				continue;

			if (isWindows()) {
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
		if (sb.length() == 0 || (isWindows() && RESERVED_WINDOWS_P.matcher(sb).matches()))
			sb.append("_");

		return sb.toString();
	}

	final static Pattern RESERVED_WINDOWS_P = Pattern.compile(
			"CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]");

	static boolean isWindows() {
		return File.separatorChar == '\\';
	}
}
