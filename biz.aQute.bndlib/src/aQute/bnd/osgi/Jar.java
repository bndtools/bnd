package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import aQute.bnd.version.Version;
import aQute.lib.base64.Base64;
import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.lib.zip.ZipUtil;

public class Jar implements Closeable {
	private static final int	BUFFER_SIZE				= IOConstants.PAGE_SIZE * 16;
	/**
	 * Note that setting the January 1st 1980 (or even worse, "0", as time)
	 * won't work due to Java 8 doing some interesting time processing: It
	 * checks if this date is before January 1st 1980 and if it is it starts
	 * setting some extra fields in the zip. Java 7 does not do that - but in
	 * the zip not the milliseconds are saved but values for each of the date
	 * fields - but no time zone. And 1980 is the first year which can be saved.
	 * If you use January 1st 1980 then it is treated as a special flag in Java
	 * 8. Moreover, only even seconds can be stored in the zip file. Java 8 uses
	 * the upper half of some other long to store the remaining millis while
	 * Java 7 doesn't do that. So make sure that your seconds are even.
	 * Moreover, parsing happens via `new Date(millis)` in
	 * {@link java.util.zip.ZipUtils}#javaToDosTime() so we must use default
	 * timezone and locale. The date is 1980 February 1st CET.
	 */
	private static final long	ZIP_ENTRY_CONSTANT_TIME	= new GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0)
		.getTimeInMillis();

	public enum Compression {
		DEFLATE,
		STORE
	}

	private static final String									DEFAULT_MANIFEST_NAME	= "META-INF/MANIFEST.MF";
	private static final Pattern								DEFAULT_DO_NOT_COPY		= Pattern
		.compile(Constants.DEFAULT_DO_NOT_COPY);

	public static final Object[]								EMPTY_ARRAY				= new Jar[0];
	private final NavigableMap<String, Resource>				resources				= new TreeMap<>();
	private final NavigableMap<String, Map<String, Resource>>	directories				= new TreeMap<>();
	private Manifest											manifest;
	private boolean												manifestFirst;
	private String												manifestName			= DEFAULT_MANIFEST_NAME;
	private String												name;
	private File												source;
	private ZipFile												zipFile;
	private long												lastModified;
	private String												lastModifiedReason;
	private boolean												doNotTouchManifest;
	private boolean												nomanifest;
	private boolean												reproducible;
	private Compression											compression				= Compression.DEFLATE;
	private boolean												closed;
	private String[]											algorithms;

	public Jar(String name) {
		this.name = name;
	}

	public Jar(String name, File dirOrFile, Pattern doNotCopy) throws IOException {
		this(name);
		source = dirOrFile;
		if (dirOrFile.isDirectory())
			buildFromDirectory(dirOrFile.toPath()
				.toAbsolutePath(), doNotCopy);
		else if (dirOrFile.isFile()) {
			buildFromZip(dirOrFile);
		} else {
			throw new IllegalArgumentException("A Jar can only accept a file or directory that exists: " + dirOrFile);
		}
	}

	public Jar(String name, InputStream in, long lastModified) throws IOException {
		this(name);
		buildFromInputStream(in, lastModified);
	}

	@SuppressWarnings("resource")
	public static Jar fromResource(String name, Resource resource) throws Exception {
		if (resource instanceof JarResource) {
			return ((JarResource) resource).getJar();
		} else if (resource instanceof FileResource) {
			return new Jar(name, ((FileResource) resource).getFile());
		}
		return new Jar(name).buildFromResource(resource);
	}

	public Jar(String name, String path) throws IOException {
		this(name, new File(path));
	}

	public Jar(File f) throws IOException {
		this(getName(f), f, null);
	}

	/**
	 * Make the JAR file name the project name if we get a src or bin directory.
	 * 
	 * @param f
	 */
	private static String getName(File f) {
		f = f.getAbsoluteFile();
		String name = f.getName();
		if (name.equals("bin") || name.equals("src"))
			return f.getParentFile()
				.getName();
		if (name.endsWith(".jar"))
			name = name.substring(0, name.length() - 4);
		return name;
	}

	public Jar(String string, InputStream resourceAsStream) throws IOException {
		this(string, resourceAsStream, 0);
	}

	public Jar(String string, File file) throws IOException {
		this(string, file, DEFAULT_DO_NOT_COPY);
	}

	private Jar buildFromDirectory(final Path baseDir, final Pattern doNotCopy) throws IOException {
		Files.walkFileTree(baseDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (doNotCopy != null) {
						String name = dir.getFileName()
							.toString();
						if (doNotCopy.matcher(name)
							.matches()) {
							return FileVisitResult.SKIP_SUBTREE;
						}
					}
					updateModified(attrs.lastModifiedTime()
						.toMillis(), "Dir change " + dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String relativePath = baseDir.relativize(file)
						.toString();
					if (File.separatorChar != '/') {
						relativePath = relativePath.replace(File.separatorChar, '/');
					}
					putResource(relativePath, new FileResource(file, attrs), true);
					return FileVisitResult.CONTINUE;
				}
			});
		return this;
	}

	private Jar buildFromZip(File file) throws IOException {
		try {
			zipFile = new ZipFile(file);
			for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				putResource(entry.getName(), new ZipResource(zipFile, entry), true);
			}
			return this;
		} catch (ZipException e) {
			ZipException ze = new ZipException(
				"The JAR/ZIP file (" + file.getAbsolutePath() + ") seems corrupted, error: " + e.getMessage());
			ze.initCause(e);
			throw ze;
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Problem opening JAR: " + file.getAbsolutePath());
		}
	}

	private Jar buildFromResource(Resource resource) throws Exception {
		return buildFromInputStream(resource.openInputStream(), resource.lastModified());
	}

	private Jar buildFromInputStream(InputStream in, long lastModified) throws IOException {
		try (ZipInputStream jin = new ZipInputStream(in)) {
			for (ZipEntry entry; (entry = jin.getNextEntry()) != null;) {
				if (entry.isDirectory()) {
					continue;
				}
				int size = (int) entry.getSize();
				try (ByteBufferOutputStream bbos = new ByteBufferOutputStream((size == -1) ? BUFFER_SIZE : size + 1)) {
					bbos.write(jin);
					putResource(entry.getName(), new EmbeddedResource(bbos.toByteBuffer(), lastModified), true);
				}
			}
		}
		return this;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Jar:" + name;
	}

	public boolean putResource(String path, Resource resource) {
		check();
		return putResource(path, resource, true);
	}

	public boolean putResource(String path, Resource resource, boolean overwrite) {
		check();
		updateModified(resource.lastModified(), path);
		while (path.startsWith("/"))
			path = path.substring(1);

		if (path.equals(manifestName)) {
			manifest = null;
			if (resources.isEmpty())
				manifestFirst = true;
		}
		Map<String, Resource> s = directories.computeIfAbsent(getDirectory(path), dir -> {
			// make ancestor directories
			for (int n = dir.lastIndexOf('/'); n > 0; n = dir.lastIndexOf('/')) {
				dir = dir.substring(0, n);
				if (directories.containsKey(dir))
					break;
				directories.put(dir, null);
			}
			return new TreeMap<>();
		});
		boolean duplicate = s.containsKey(path);
		if (!duplicate || overwrite) {
			resources.put(path, resource);
			s.put(path, resource);
		}
		return duplicate;
	}

	public Resource getResource(String path) {
		check();
		return resources.get(path);
	}

	private String getDirectory(String path) {
		check();
		int n = path.lastIndexOf('/');
		if (n < 0)
			return "";

		return path.substring(0, n);
	}

	public Map<String, Map<String, Resource>> getDirectories() {
		check();
		return directories;
	}

	public Map<String, Resource> getResources() {
		check();
		return resources;
	}

	public boolean addDirectory(Map<String, Resource> directory, boolean overwrite) {
		check();
		boolean duplicates = false;
		if (directory == null)
			return false;

		for (Map.Entry<String, Resource> entry : directory.entrySet()) {
			duplicates |= putResource(entry.getKey(), entry.getValue(), overwrite);
		}
		return duplicates;
	}

	public Manifest getManifest() throws Exception {
		check();
		if (manifest == null) {
			Resource manifestResource = getResource(manifestName);
			if (manifestResource != null) {
				try (InputStream in = manifestResource.openInputStream()) {
					manifest = new Manifest(in);
				}
			}
		}
		return manifest;
	}

	public boolean exists(String path) {
		check();
		return resources.containsKey(path);
	}

	public void setManifest(Manifest manifest) {
		check();
		manifestFirst = true;
		this.manifest = manifest;
	}

	public void setManifest(File file) throws IOException {
		check();
		try (InputStream fin = IO.stream(file)) {
			Manifest m = new Manifest(fin);
			setManifest(m);
		}
	}

	public void setManifestName(String manifestName) {
		check();
		if (manifestName == null || manifestName.length() == 0)
			throw new IllegalArgumentException("Manifest name cannot be null or empty!");
		this.manifestName = manifestName;
	}

	public void write(File file) throws Exception {
		check();
		try (OutputStream out = IO.outputStream(file)) {
			write(out);
		} catch (Exception t) {
			IO.delete(file);
			throw t;
		}
		file.setLastModified(lastModified);
	}

	public void write(String file) throws Exception {
		check();
		write(new File(file));
	}

	public void write(OutputStream out) throws Exception {
		check();

		if (!doNotTouchManifest && !nomanifest && algorithms != null) {
			doChecksums(out);
			return;
		}

		ZipOutputStream jout = nomanifest || doNotTouchManifest ? new ZipOutputStream(out) : new JarOutputStream(out);

		switch (compression) {
			case STORE :
				jout.setMethod(ZipOutputStream.DEFLATED);
				break;

			default :
				// default is DEFLATED
		}

		Set<String> done = new HashSet<>();

		Set<String> directories = new HashSet<>();
		if (doNotTouchManifest) {
			Resource r = getResource(manifestName);
			if (r != null) {
				writeResource(jout, directories, manifestName, r);
				done.add(manifestName);
			}
		} else if (!nomanifest) {
			doManifest(jout, directories, manifestName);
			done.add(manifestName);
		}

		for (Map.Entry<String, Resource> entry : getResources().entrySet()) {
			// Skip metainf contents
			if (!done.contains(entry.getKey()))
				writeResource(jout, directories, entry.getKey(), entry.getValue());
		}
		jout.finish();
	}

	public void writeFolder(File dir) throws Exception {
		IO.mkdirs(dir);

		if (!dir.exists())
			throw new IllegalArgumentException(
				"The directory " + dir + " to write the JAR " + this + " could not be created");

		if (!dir.isDirectory())
			throw new IllegalArgumentException(
				"The directory " + dir + " to write the JAR " + this + " to is not a directory");

		check();

		Set<String> done = new HashSet<>();

		if (doNotTouchManifest) {
			Resource r = getResource(manifestName);
			if (r != null) {
				copyResource(dir, manifestName, r);
				done.add(manifestName);
			}
		} else {
			File file = IO.getFile(dir, manifestName);
			IO.mkdirs(file.getParentFile());
			try (OutputStream fout = IO.outputStream(file)) {
				writeManifest(fout);
				done.add(manifestName);
			}
		}

		for (Map.Entry<String, Resource> entry : getResources().entrySet()) {
			String path = entry.getKey();
			if (done.contains(path))
				continue;

			Resource resource = entry.getValue();
			copyResource(dir, path, resource);
		}
	}

	private void copyResource(File dir, String path, Resource resource) throws Exception {
		File to = IO.getFile(dir, path);
		IO.mkdirs(to.getParentFile());
		IO.copy(resource.openInputStream(), to);
	}

	public void doChecksums(OutputStream out) throws Exception {
		// ok, we have a request to create digests
		// of the resources. Since we have to output
		// the manifest first, we have a slight problem.
		// We can also not make multiple passes over the resource
		// because some resources are not idempotent and/or can
		// take significant time. So we just copy the jar
		// to a temporary file, read it in again, calculate
		// the checksums and save.

		String[] algs = algorithms;
		algorithms = null;
		try {
			File f = File.createTempFile(padString(getName(), 3, '_'), ".jar");
			write(f);
			try (Jar tmp = new Jar(f)) {
				tmp.calcChecksums(algorithms);
				tmp.write(out);
			} finally {
				IO.delete(f);
			}
		} finally {
			algorithms = algs;
		}
	}

	private String padString(String s, int length, char pad) {
		if (s == null)
			s = "";
		if (s.length() >= length)
			return s;
		char[] cs = new char[length];
		Arrays.fill(cs, pad);

		char[] orig = s.toCharArray();
		System.arraycopy(orig, 0, cs, 0, orig.length);
		return new String(cs);
	}

	private void doManifest(ZipOutputStream jout, Set<String> directories, String manifestName) throws Exception {
		check();
		createDirectories(directories, jout, manifestName);
		JarEntry ze = new JarEntry(manifestName);
		if (isReproducible()) {
			ze.setTime(ZIP_ENTRY_CONSTANT_TIME);
		} else {
			ZipUtil.setModifiedTime(ze, lastModified);
		}
		jout.putNextEntry(ze);
		writeManifest(jout);
		jout.closeEntry();
	}

	/**
	 * Cleanup the manifest for writing. Cleaning up consists of adding a space
	 * after any \n to prevent the manifest to see this newline as a delimiter.
	 * 
	 * @param out Output
	 * @throws IOException
	 */

	public void writeManifest(OutputStream out) throws Exception {
		check();
		stripSignatures();
		writeManifest(getManifest(), out);
	}

	public static void writeManifest(Manifest manifest, OutputStream out) throws IOException {
		if (manifest == null)
			return;

		manifest = clean(manifest);
		outputManifest(manifest, out);
	}

	/**
	 * Unfortunately we have to write our own manifest :-( because of a stupid
	 * bug in the manifest code. It tries to handle UTF-8 but the way it does it
	 * it makes the bytes platform dependent. So the following code outputs the
	 * manifest. A Manifest consists of
	 * 
	 * <pre>
	 *  'Manifest-Version: 1.0\r\n'
	 * main-attributes * \r\n name-section main-attributes ::= attributes
	 * attributes ::= key ': ' value '\r\n' name-section ::= 'Name: ' name
	 * '\r\n' attributes
	 * </pre>
	 * 
	 * Lines in the manifest should not exceed 72 bytes (! this is where the
	 * manifest screwed up as well when 16 bit unicodes were used).
	 * <p>
	 * As a bonus, we can now sort the manifest!
	 */
	private final static byte[]	EOL			= new byte[] {
		'\r', '\n'
	};
	private final static byte[]	SEPARATOR	= new byte[] {
		':', ' '
	};

	/**
	 * Main function to output a manifest properly in UTF-8.
	 * 
	 * @param manifest The manifest to output
	 * @param out The output stream
	 * @throws IOException when something fails
	 */
	public static void outputManifest(Manifest manifest, OutputStream out) throws IOException {
		writeEntry(out, "Manifest-Version", "1.0");
		attributes(manifest.getMainAttributes(), out);

		TreeSet<String> keys = new TreeSet<>();
		for (Object o : manifest.getEntries()
			.keySet())
			keys.add(o.toString());

		for (String key : keys) {
			out.write(EOL);
			writeEntry(out, "Name", key);
			attributes(manifest.getAttributes(key), out);
		}
		out.flush();
	}

	/**
	 * Write out an entry, handling proper unicode and line length constraints
	 */
	private static void writeEntry(OutputStream out, String name, String value) throws IOException {
		int width = write(out, 0, name);
		width = write(out, width, SEPARATOR);
		write(out, width, value);
		out.write(EOL);
	}

	/**
	 * Convert a string to bytes with UTF-8 and then output in max 72 bytes
	 * 
	 * @param out the output string
	 * @param width the current width
	 * @param s the string to output
	 * @return the new width
	 * @throws IOException when something fails
	 */
	private static int write(OutputStream out, int width, String s) throws IOException {
		byte[] bytes = s.getBytes(UTF_8);
		return write(out, width, bytes);
	}

	/**
	 * Write the bytes but ensure that the line length does not exceed 72
	 * characters. If it is more than 70 characters, we just put a cr/lf +
	 * space.
	 * 
	 * @param out The output stream
	 * @param width The nr of characters output in a line before this method
	 *            started
	 * @param bytes the bytes to output
	 * @return the nr of characters in the last line
	 * @throws IOException if something fails
	 */
	private static int write(OutputStream out, int width, byte[] bytes) throws IOException {
		int w = width;
		for (int i = 0; i < bytes.length; i++) {
			if (w >= 72 - EOL.length) { // we need to add the EOL!
				out.write(EOL);
				out.write(' ');
				w = 1;
			}
			out.write(bytes[i]);
			w++;
		}
		return w;
	}

	/**
	 * Output an Attributes map. We will sort this map before outputing.
	 * 
	 * @param value the attrbutes
	 * @param out the output stream
	 * @throws IOException when something fails
	 */
	private static void attributes(Attributes value, OutputStream out) throws IOException {
		TreeMap<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Map.Entry<Object, Object> entry : value.entrySet()) {
			map.put(entry.getKey()
				.toString(),
				entry.getValue()
					.toString());
		}

		map.remove("Manifest-Version"); // get rid of manifest version
		for (Map.Entry<String, String> entry : map.entrySet()) {
			writeEntry(out, entry.getKey(), entry.getValue());
		}
	}

	private static Manifest clean(Manifest org) {

		Manifest result = new Manifest();
		for (Map.Entry<?, ?> entry : org.getMainAttributes()
			.entrySet()) {
			String nice = clean((String) entry.getValue());
			result.getMainAttributes()
				.put(entry.getKey(), nice);
		}
		for (String name : org.getEntries()
			.keySet()) {
			Attributes attrs = result.getAttributes(name);
			if (attrs == null) {
				attrs = new Attributes();
				result.getEntries()
					.put(name, attrs);
			}

			for (Map.Entry<?, ?> entry : org.getAttributes(name)
				.entrySet()) {
				String nice = clean((String) entry.getValue());
				attrs.put(entry.getKey(), nice);
			}
		}
		return result;
	}

	private static String clean(String s) {
		StringBuilder sb = new StringBuilder(s);
		boolean changed = false;
		boolean replacedPrev = false;
		for (int i = 0; i < sb.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case 0 :
				case '\n' :
				case '\r' :
					changed = true;
					if (!replacedPrev) {
						sb.replace(i, i + 1, " ");
						replacedPrev = true;
					} else
						sb.delete(i, i + 1);
					break;
				default :
					replacedPrev = false;
					break;
			}
		}
		if (changed)
			return sb.toString();
		else
			return s;
	}

	private void writeResource(ZipOutputStream jout, Set<String> directories, String path, Resource resource)
		throws Exception {
		if (resource == null)
			return;
		try {
			createDirectories(directories, jout, path);
			if (path.endsWith(Constants.EMPTY_HEADER))
				return;
			ZipEntry ze = new ZipEntry(path);
			ze.setMethod(ZipEntry.DEFLATED);
			if (isReproducible()) {
				ze.setTime(ZIP_ENTRY_CONSTANT_TIME);
			} else {
				long lastModified = resource.lastModified();
				if (lastModified == 0L) {
					lastModified = System.currentTimeMillis();
				}
				ZipUtil.setModifiedTime(ze, lastModified);
			}
			if (resource.getExtra() != null)
				ze.setExtra(resource.getExtra()
					.getBytes(UTF_8));
			jout.putNextEntry(ze);
			resource.write(jout);
			jout.closeEntry();
		} catch (Exception e) {
			throw new Exception("Problem writing resource " + path, e);
		}
	}

	void createDirectories(Set<String> directories, ZipOutputStream zip, String name) throws IOException {
		int index = name.lastIndexOf('/');
		if (index > 0) {
			String path = name.substring(0, index);
			if (directories.contains(path))
				return;
			createDirectories(directories, zip, path);
			ZipEntry ze = new ZipEntry(path + '/');
			if (isReproducible()) {
				ze.setTime(ZIP_ENTRY_CONSTANT_TIME);
			} else {
				ZipUtil.setModifiedTime(ze, lastModified);
			}
			zip.putNextEntry(ze);
			zip.closeEntry();
			directories.add(path);
		}
	}

	public String getName() {
		return name;
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 * 
	 * @param sub the jar
	 * @param filter a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar sub, Instruction filter) {
		return addAll(sub, filter, "");
	}

	/**
	 * Add all the resources in the given jar that match the given filter.
	 * 
	 * @param sub the jar
	 * @param filter a pattern that should match the resoures in sub to be added
	 */
	public boolean addAll(Jar sub, Instruction filter, String destination) {
		check();
		boolean dupl = false;
		for (String name : sub.getResources()
			.keySet()) {
			if (manifestName.equals(name))
				continue;

			if (filter == null || filter.matches(name) != filter.isNegated())
				dupl |= putResource(Processor.appendPath(destination, name), sub.getResource(name), true);
		}
		return dupl;
	}

	@Override
	public void close() {
		this.closed = true;
		IO.close(zipFile);
		for (Resource r : resources.values()) {
			IO.close(r);
		}
		resources.clear();
		directories.clear();
		manifest = null;
		source = null;
	}

	public long lastModified() {
		return lastModified;
	}

	String lastModifiedReason() {
		return lastModifiedReason;
	}

	public void updateModified(long time, String reason) {
		if (time > lastModified) {
			lastModified = time;
			lastModifiedReason = reason;
		}
	}

	public boolean hasDirectory(String path) {
		check();
		return directories.get(path) != null;
	}

	public List<String> getPackages() {
		check();
		return directories.entrySet()
			.stream()
			.filter(e -> e.getValue() != null)
			.map(e -> e.getKey()
				.replace('/', '.'))
			.collect(Collectors.toList());
	}

	public File getSource() {
		check();
		return source;
	}

	public boolean addAll(Jar src) {
		check();
		return addAll(src, null);
	}

	public boolean rename(String oldPath, String newPath) {
		check();
		Resource resource = remove(oldPath);
		if (resource == null)
			return false;

		return putResource(newPath, resource);
	}

	public Resource remove(String path) {
		check();
		Resource resource = resources.remove(path);
		if (resource != null) {
			String dir = getDirectory(path);
			Map<String, Resource> mdir = directories.get(dir);
			// must be != null
			mdir.remove(path);
		}
		return resource;
	}

	/**
	 * Make sure nobody touches the manifest! If the bundle is signed, we do not
	 * want anybody to touch the manifest after the digests have been
	 * calculated.
	 */
	public void setDoNotTouchManifest() {
		doNotTouchManifest = true;
	}

	/**
	 * Calculate the checksums and set them in the manifest.
	 */

	public void calcChecksums(String[] algorithms) throws Exception {
		check();
		if (algorithms == null)
			algorithms = new String[] {
				"SHA", "MD5"
			};

		Manifest m = getManifest();
		if (m == null) {
			m = new Manifest();
			setManifest(m);
		}

		MessageDigest[] digests = new MessageDigest[algorithms.length];
		int n = 0;
		for (String algorithm : algorithms)
			digests[n++] = MessageDigest.getInstance(algorithm);

		byte[] buffer = new byte[BUFFER_SIZE];

		for (Map.Entry<String, Resource> entry : resources.entrySet()) {
			String path = entry.getKey();
			// Skip the manifest
			if (path.equals(manifestName))
				continue;

			Attributes attributes = m.getAttributes(path);
			if (attributes == null) {
				attributes = new Attributes();
				getManifest().getEntries()
					.put(path, attributes);
			}
			Resource r = entry.getValue();
			ByteBuffer bb = r.buffer();
			if ((bb != null) && bb.hasArray()) {
				for (MessageDigest d : digests) {
					d.update(bb);
					bb.flip();
				}
			} else {
				try (InputStream in = r.openInputStream()) {
					for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
						for (MessageDigest d : digests) {
							d.update(buffer, 0, size);
						}
					}
				}
			}
			for (MessageDigest d : digests) {
				attributes.putValue(d.getAlgorithm() + "-Digest", Base64.encodeBase64(d.digest()));
				d.reset();
			}
		}
	}

	private final static Pattern BSN = Pattern.compile("\\s*([-\\w\\d\\._]+)\\s*;?.*");

	/**
	 * Get the jar bsn from the {@link Constants#BUNDLE_SYMBOLICNAME} manifest
	 * header.
	 * 
	 * @return null when the jar has no manifest, when the manifest has no
	 *         {@link Constants#BUNDLE_SYMBOLICNAME} header, or when the value
	 *         of the header is not a valid bsn according to {@link #BSN}.
	 * @throws Exception when the jar is closed or when the manifest could not
	 *             be retrieved.
	 */
	public String getBsn() throws Exception {
		check();
		Manifest m = getManifest();
		if (m == null)
			return null;

		String s = m.getMainAttributes()
			.getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (s == null)
			return null;

		Matcher matcher = BSN.matcher(s);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}

	/**
	 * Get the jar version from the {@link Constants#BUNDLE_VERSION} manifest
	 * header.
	 * 
	 * @return null when the jar has no manifest or when the manifest has no
	 *         {@link Constants#BUNDLE_VERSION} header
	 * @throws Exception when the jar is closed or when the manifest could not
	 *             be retrieved.
	 */
	public String getVersion() throws Exception {
		check();
		Manifest m = getManifest();
		if (m == null)
			return null;

		String s = m.getMainAttributes()
			.getValue(Constants.BUNDLE_VERSION);
		if (s == null)
			return null;

		return s.trim();
	}

	/**
	 * Expand the JAR file to a directory.
	 * 
	 * @param dir the dst directory, is not required to exist
	 * @throws Exception if anything does not work as expected.
	 */
	public void expand(File dir) throws Exception {
		writeFolder(dir);
	}

	/**
	 * Make sure we have a manifest
	 * 
	 * @throws Exception
	 */
	public void ensureManifest() throws Exception {
		if (getManifest() != null)
			return;
		manifest = new Manifest();
	}

	/**
	 * Answer if the manifest was the first entry
	 */

	public boolean isManifestFirst() {
		return manifestFirst;
	}

	public boolean isReproducible() {
		return reproducible;
	}

	public void setReproducible(boolean reproducible) {
		this.reproducible = reproducible;
	}

	public void copy(Jar srce, String path, boolean overwrite) {
		check();
		addDirectory(srce.getDirectories()
			.get(path), overwrite);
	}

	public void setCompression(Compression compression) {
		this.compression = compression;
	}

	public Compression hasCompression() {
		return this.compression;
	}

	void check() {
		if (closed)
			throw new RuntimeException("Already closed " + name);
	}

	/**
	 * Return a data uri from the JAR. The data must be less than 32k
	 * 
	 * @param path the path in the jar
	 * @param mime the mime type
	 * @return a URI or null if conversion could not take place
	 */

	public URI getDataURI(String path, String mime, int max) throws Exception {
		Resource r = getResource(path);

		if (r.size() >= max || r.size() <= 0)
			return null;

		byte[] data = new byte[(int) r.size()];
		try (DataInputStream din = new DataInputStream(r.openInputStream())) {
			din.readFully(data);
			String encoded = Base64.encodeBase64(data);
			return new URI("data:" + mime + ";base64," + encoded);
		}
	}

	public void setDigestAlgorithms(String[] algorithms) {
		this.algorithms = algorithms;
	}

	public byte[] getTimelessDigest() throws Exception {
		check();

		MessageDigest md = MessageDigest.getInstance("SHA1");
		OutputStream dout = new DigestOutputStream(IO.nullStream, md);
		// dout = System.out;

		Manifest m = getManifest();

		if (m != null) {
			Manifest m2 = new Manifest(m);
			Attributes main = m2.getMainAttributes();
			String lastmodified = (String) main.remove(new Attributes.Name(Constants.BND_LASTMODIFIED));
			String version = main.getValue(new Attributes.Name(Constants.BUNDLE_VERSION));
			if (version != null && Verifier.isVersion(version)) {
				Version v = new Version(version);
				main.putValue(Constants.BUNDLE_VERSION, v.toStringWithoutQualifier());
			}
			writeManifest(m2, dout);

			for (Map.Entry<String, Resource> entry : getResources().entrySet()) {
				String path = entry.getKey();
				if (path.equals(manifestName))
					continue;
				Resource resource = entry.getValue();
				dout.write(path.getBytes(UTF_8));
				resource.write(dout);
			}
		}
		return md.digest();
	}

	static Pattern SIGNER_FILES_P = Pattern.compile("(.+\\.(SF|DSA|RSA))|(.*/SIG-.*)", Pattern.CASE_INSENSITIVE);

	public void stripSignatures() {
		Map<String, Resource> map = getDirectories().get("META-INF");
		if (map != null) {
			for (String file : new HashSet<>(map.keySet())) {
				if (SIGNER_FILES_P.matcher(file)
					.matches())
					remove(file);
			}
		}
	}

	public void removePrefix(String prefixLow) {
		String prefixHigh = prefixLow + "\uFFFF";
		resources.subMap(prefixLow, prefixHigh)
			.clear();
		if (prefixLow.endsWith("/")) {
			prefixLow = prefixLow.substring(0, prefixLow.length() - 1);
			prefixHigh = prefixLow + "\uFFFF";
		}
		directories.subMap(prefixLow, prefixHigh)
			.clear();
	}
}
