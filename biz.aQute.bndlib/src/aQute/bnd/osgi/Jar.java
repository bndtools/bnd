package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.Version;
import aQute.lib.base64.Base64;
import aQute.lib.collections.Iterables;
import aQute.lib.date.Dates;
import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.lib.manifest.ManifestUtil;
import aQute.lib.zip.ZipUtil;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.SHA256;
import aQute.libg.glob.PathSet;

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
	 * timezone and locale. The date is 1980-02-01T00:00:00Z.
	 */
	private static final long	ZIP_ENTRY_CONSTANT_TIME	= 318211200000L;

	public enum Compression {
		DEFLATE,
		STORE
	}

	private static final Pattern								DEFAULT_DO_NOT_COPY		= Pattern
		.compile(Constants.DEFAULT_DO_NOT_COPY);

	public static final Object[]								EMPTY_ARRAY				= new Jar[0];
	private final NavigableMap<String, Resource>				resources				= new TreeMap<>();
	private final NavigableMap<String, Map<String, Resource>>	directories				= new TreeMap<>();
	private Optional<Manifest>									manifest;
	private boolean												manifestFirst;
	private String												manifestName			= JarFile.MANIFEST_NAME;
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
	private SHA256												sha256;
	private boolean												calculateFileDigest;
	private int													fileLength				= -1;
	private long												zipEntryConstantTime	= ZIP_ENTRY_CONSTANT_TIME;
	private boolean												closeResources			= true;

	public static final Pattern									METAINF_SIGNING_P		= Pattern
		.compile("META-INF/([^/]+\\.(?:DSA|RSA|EC|SF)|SIG-[^/]+)", Pattern.CASE_INSENSITIVE);

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
		this(name, in);
	}

	@SuppressWarnings("resource")
	public static Jar fromResource(String name, Resource resource) throws Exception {
		if (resource instanceof JarResource jarResource) {
			return jarResource.getJar();
		} else if (resource instanceof FileResource fileResource) {
			return new Jar(name, fileResource.getFile());
		}
		return new Jar(name).buildFromResource(resource);
	}

	public static Stream<Resource> getResources(Resource resource, Predicate<String> filter) throws Exception {
		requireNonNull(resource);
		requireNonNull(filter);
		if (resource instanceof JarResource jarResource) {
			Jar jar = jarResource.getJar();
			return jar.getResources(filter);
		}
		ZipResourceSpliterator spliterator = new ZipResourceSpliterator(resource, filter);
		return StreamSupport.stream(spliterator, false)
			.onClose(spliterator);
	}

	static final class ZipResourceSpliterator extends AbstractSpliterator<Resource> implements Runnable {
		private final ZipInputStream			jin;
		private final Predicate<String>			filter;
		private final ByteBufferOutputStream	bbos	= new ByteBufferOutputStream(BUFFER_SIZE);

		ZipResourceSpliterator(Resource resource, Predicate<String> filter) throws Exception {
			super(Long.MAX_VALUE, Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.NONNULL);
			this.jin = new ZipInputStream(resource.openInputStream());
			this.filter = requireNonNull(filter);
		}

		@Override
		public boolean tryAdvance(Consumer<? super Resource> action) {
			requireNonNull(action);
			try {
				for (ZipEntry entry; (entry = jin.getNextEntry()) != null;) {
					if (entry.isDirectory()) {
						continue;
					}
					String path = ZipUtil.cleanPath(entry.getName());
					if (filter.test(path)) {
						bbos.clear()
							.write(jin);
						Resource resource = new EmbeddedResource(bbos.toByteBuffer(), ZipUtil.getModifiedTime(entry));
						action.accept(resource);
						return true;
					}
				}
				return false;
			} catch (IOException e) {
				return false;
			}
		}

		@Override
		public void forEachRemaining(Consumer<? super Resource> action) {
			requireNonNull(action);
			try {
				for (ZipEntry entry; (entry = jin.getNextEntry()) != null;) {
					if (entry.isDirectory()) {
						continue;
					}
					String path = ZipUtil.cleanPath(entry.getName());
					if (filter.test(path)) {
						bbos.clear()
							.write(jin);
						Resource resource = new EmbeddedResource(bbos.toByteBuffer(), ZipUtil.getModifiedTime(entry));
						action.accept(resource);
					}
				}
			} catch (IOException e) {
				return;
			}
		}

		@Override
		public void run() {
			IO.close(jin);
		}
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

	public Jar(String name, InputStream in) throws IOException {
		this(name);
		buildFromInputStream(in);
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
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (doNotCopy != null) {
						String name = file.getFileName()
							.toString();
						if (doNotCopy.matcher(name)
							.matches()) {
							return FileVisitResult.CONTINUE;
						}
					}
					String relativePath = IO.normalizePath(baseDir.relativize(file));
					putResource(relativePath, new FileResource(file, attrs), true);
					return FileVisitResult.CONTINUE;
				}
			});
		return this;
	}

	private Jar buildFromZip(File file) throws IOException {
		try {
			zipFile = new ZipFile(file);
			for (ZipEntry entry : Iterables.iterable(zipFile.entries())) {
				if (entry.isDirectory()) {
					continue;
				}
				putResource(entry.getName(), new ZipResource(zipFile, entry), true);
			}
			return this;
		} catch (ZipException e) {
			IO.close(zipFile);
			ZipException ze = new ZipException(
				"The JAR/ZIP file (" + file.getAbsolutePath() + ") seems corrupted, error: " + e.getMessage());
			ze.initCause(e);
			throw ze;
		} catch (FileNotFoundException e) {
			IO.close(zipFile);
			throw new IllegalArgumentException("Problem opening JAR: " + file.getAbsolutePath(), e);
		} catch (IOException e) {
			IO.close(zipFile);
			throw e;
		}
	}

	private Jar buildFromResource(Resource resource) throws Exception {
		return buildFromInputStream(resource.openInputStream());
	}

	private Jar buildFromInputStream(InputStream in) throws IOException {
		try (ZipInputStream jin = new ZipInputStream(in);
			ByteBufferOutputStream bbos = new ByteBufferOutputStream(BUFFER_SIZE)) {
			for (ZipEntry entry; (entry = jin.getNextEntry()) != null;) {
				if (entry.isDirectory()) {
					continue;
				}
				bbos.clear()
					.write(jin);
				Resource resource = new EmbeddedResource(bbos.toByteBuffer(), ZipUtil.getModifiedTime(entry));
				byte[] extra = entry.getExtra();
				if (extra != null) {
					resource.setExtra(Resource.encodeExtra(extra));
				}
				putResource(entry.getName(), resource, true);
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
		return putResource(path, resource, true);
	}

	public boolean putResource(String path, Resource resource, boolean overwrite) {
		check();
		path = ZipUtil.cleanPath(path);

		if (path.equals(manifestName)) {
			manifest = null;
			if (resources.isEmpty())
				manifestFirst = true;
		}
		String dir = getParent(path);
		Map<String, Resource> s = directories.get(dir);
		if (s == null) {
			s = new TreeMap<>();
			directories.put(dir, s);
			// make ancestor directories
			for (int n; (n = dir.lastIndexOf('/')) > 0;) {
				dir = dir.substring(0, n);
				if (directories.containsKey(dir))
					break;
				directories.put(dir, null);
			}
		}
		boolean duplicate = s.containsKey(path);
		if (!duplicate || overwrite) {
			resources.put(path, resource);
			s.put(path, resource);
			updateModified(resource.lastModified(), path);
		}
		return duplicate;
	}

	public Resource getResource(String path) {
		check();
		path = ZipUtil.cleanPath(path);
		return resources.get(path);
	}

	public Stream<String> getResourceNames(Predicate<String> matches) {
		return getResources().keySet()
			.stream()
			.filter(matches);
	}

	public Stream<Resource> getResources(Predicate<String> matches) {
		return getResourceNames(matches).map(resources::get);
	}

	private String getParent(String path) {
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

	public Map<String, Resource> getDirectory(String path) {
		check();
		path = ZipUtil.cleanPath(path);
		return directories.get(path);
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
		return manifest().orElse(null);
	}

	Optional<Manifest> manifest() {
		check();
		Optional<Manifest> optional = manifest;
		if (optional != null) {
			return optional;
		}
		try {
			Resource manifestResource = getResource(manifestName);
			if (manifestResource == null) {
				return manifest = Optional.empty();
			}
			try (InputStream in = manifestResource.openInputStream()) {
				return manifest = Optional.of(new Manifest(in));
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * This method is deprecated because the JAR file tries to be ignorant of
	 * modules. The JPMSModule provides a facade or a Jar and understand
	 * multirelease and other JPMS shit.
	 */
	public String getModuleName() throws Exception {
		return new JPMSModule(this).getModuleName()
			.orElse(null);
	}

	/**
	 * This method is deprecated because the JAR file tries to be ignorant of
	 * modules. The JPMSModule provides a facade or a Jar and understand
	 * multirelease and other JPMS shit.
	 */
	public String getModuleVersion() throws Exception {
		return new JPMSModule(this).getModuleVersion()
			.orElse(null);
	}

	public boolean exists(String path) {
		check();
		path = ZipUtil.cleanPath(path);
		return resources.containsKey(path);
	}

	public boolean isEmpty() {
		check();
		return resources.isEmpty();
	}

	public void setManifest(Manifest manifest) {
		check();
		manifestFirst = true;
		this.manifest = Optional.ofNullable(manifest);
	}

	public void setManifest(File file) throws IOException {
		check();
		try (InputStream fin = IO.stream(file)) {
			Manifest m = new Manifest(fin);
			setManifest(m);
		}
	}

	public String getManifestName() {
		return manifestName;
	}

	public void setManifestName(String manifestName) {
		check();
		manifestName = ZipUtil.cleanPath(manifestName);
		if (manifestName.isEmpty())
			throw new IllegalArgumentException("Manifest name must not be empty");
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
		file.setLastModified(lastModified());
	}

	public void write(String file) throws Exception {
		check();
		write(new File(file));
	}

	public void write(OutputStream to) throws Exception {
		check();

		if (!doNotTouchManifest && !nomanifest && algorithms != null) {
			doChecksums(to);
			return;
		}

		OutputStream out = to;
		Digester<SHA256> digester = null;
		sha256 = null;
		fileLength = -1;

		if (calculateFileDigest) {
			out = digester = SHA256.getDigester(out);
		}

		ZipOutputStream jout = nomanifest || doNotTouchManifest ? new ZipOutputStream(out) : new JarOutputStream(out);

		switch (compression) {
			case STORE :
				jout.setMethod(ZipOutputStream.STORED);
				break;

			default :
				// default is DEFLATED
		}

		Set<String> done = new HashSet<>();

		Set<String> directories = new HashSet<>();

		// Write manifest first
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

		// Then write any signature info next since JarInputStream really cares!
		Map<String, Resource> metainf = getDirectory("META-INF");
		if (metainf != null) {
			List<String> signing = metainf.keySet()
				.stream()
				.filter(path -> METAINF_SIGNING_P.matcher(path)
					.matches())
				.collect(toList());
			for (String path : signing) {
				if (done.add(path)) {
					writeResource(jout, directories, path, metainf.get(path));
				}
			}
		}

		// Write all remaining entries
		for (Map.Entry<String, Resource> entry : getResources().entrySet()) {
			// Skip metainf contents
			if (!done.contains(entry.getKey()))
				writeResource(jout, directories, entry.getKey(), entry.getValue());
		}
		jout.finish();

		if (digester != null) {
			this.sha256 = digester.digest();
			this.fileLength = digester.getLength();
		}
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
			File file = IO.getBasedFile(dir, manifestName);
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
		File to = IO.getBasedFile(dir, path);
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
				tmp.setCompression(compression);
				tmp.calcChecksums(algs);
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
		ZipUtil.setModifiedTime(ze, isReproducible() ? zipEntryConstantTime : lastModified());
		Resource r = new WriteResource() {

			@Override
			public void write(OutputStream out) throws Exception {
				writeManifest(out);
			}

			@Override
			public long lastModified() {
				return 0; // a manifest should not change the date
			}
		};
		putEntry(jout, ze, r);
	}

	private void putEntry(ZipOutputStream jout, ZipEntry entry, Resource r) throws Exception {

		if (compression == Compression.STORE) {
			ByteBuffer buffer = r.buffer();
			if (buffer == null) {
				buffer = IO.copy(r.openInputStream(), new ByteBufferOutputStream())
					.toByteBuffer();
			}
			entry.setMethod(ZipOutputStream.STORED);
			CRC32 crc = new CRC32();
			buffer.mark();
			crc.update(buffer);
			buffer.reset();
			entry.setCrc(crc.getValue());
			entry.setSize(buffer.remaining());
			entry.setCompressedSize(buffer.remaining());
			jout.putNextEntry(entry);
			IO.copy(buffer, jout);
		} else {
			jout.putNextEntry(entry);
			r.write(jout);
		}
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
	 * Main function to output a manifest properly in UTF-8.
	 *
	 * @param manifest The manifest to output
	 * @param out The output stream
	 * @throws IOException when something fails
	 */
	public static void outputManifest(Manifest manifest, OutputStream out) throws IOException {
		ManifestUtil.write(manifest, out);
	}

	private static Manifest clean(Manifest org) {
		Manifest result = new Manifest();
		Attributes mainAttributes = result.getMainAttributes();
		for (Map.Entry<?, ?> entry : org.getMainAttributes()
			.entrySet()) {
			String nice = clean((String) entry.getValue());
			mainAttributes.put(entry.getKey(), nice);
		}
		mainAttributes.putIfAbsent(Attributes.Name.MANIFEST_VERSION, "1.0");
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
				ZipUtil.setModifiedTime(ze, zipEntryConstantTime);
			} else {
				long lastModified = resource.lastModified();
				if (lastModified == 0L) {
					lastModified = System.currentTimeMillis();
				}
				ZipUtil.setModifiedTime(ze, lastModified);
			}
			String extra = resource.getExtra();
			if (extra != null) {
				ze.setExtra(Resource.decodeExtra(extra));
			}
			putEntry(jout, ze, resource);
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
			ZipUtil.setModifiedTime(ze, isReproducible() ? zipEntryConstantTime : lastModified());
			if (compression == Compression.STORE) {
				ze.setMethod(ZipOutputStream.STORED);
				ze.setCrc(0L);
				ze.setSize(0L);
				ze.setCompressedSize(0L);
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

			if (filter == null || filter.matches(name) ^ filter.isNegated())
				dupl |= putResource(Processor.appendPath(destination, name), sub.getResource(name), true);
		}
		return dupl;
	}

	@Override
	public void close() {
		this.closed = true;
		if (closeResources) {
			IO.close(zipFile);
			resources.values()
				.forEach(IO::close);
		}
		zipFile = null;
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
		path = ZipUtil.cleanPath(path);
		return directories.containsKey(path);
	}

	public List<String> getPackages() {
		check();
		return MapStream.of(directories)
			.filterValue(mdir -> Objects.nonNull(mdir) && !mdir.isEmpty())
			.keys()
			.map(k -> k.replace('/', '.'))
			.collect(toList());
	}

	public File getSource() {
		check();
		return source;
	}

	public void setSource(File source) {
		this.source = source;
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
		path = ZipUtil.cleanPath(path);
		Resource resource = resources.remove(path);
		if (resource != null) {
			String dir = getParent(path);
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
				"SHA1", "MD5"
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

	private final static Pattern BSN = Pattern.compile("\\s*([-.\\w]+)\\s*;?.*");

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
		return manifest().map(m -> m.getMainAttributes()
			.getValue(Constants.BUNDLE_SYMBOLICNAME))
			.map(s -> {
				Matcher matcher = BSN.matcher(s);
				return matcher.matches() ? matcher.group(1) : null;
			})
			.orElse(null);
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
		return manifest().map(m -> m.getMainAttributes()
			.getValue(Constants.BUNDLE_VERSION))
			.map(String::trim)
			.orElse(null);
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
		if (!manifest().isPresent()) {
			manifest = Optional.of(new Manifest());
		}
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

	public void setReproducible(String outputTimestamp) {
		reproducible = Processor.isTrue(outputTimestamp);
		if (!reproducible || Boolean.parseBoolean(outputTimestamp = outputTimestamp.trim())) {
			zipEntryConstantTime = ZIP_ENTRY_CONSTANT_TIME;
			return; // false or just plain "true"
		}
		// is it epoch seconds?
		// https://reproducible-builds.org/docs/source-date-epoch/
		try {
			zipEntryConstantTime = Long.parseUnsignedLong(outputTimestamp) * 1000L;
			return;
		} catch (NumberFormatException e) {
			// ignore
		}
		// is it a date?
		try {
			ZonedDateTime dateTime = Dates.toZonedDateTime(DateTimeFormatter.ISO_DATE_TIME.parse(outputTimestamp));
			zipEntryConstantTime = dateTime.toInstant()
				.toEpochMilli();
			return;
		} catch (DateTimeParseException e) {
			// ignore
		}
		zipEntryConstantTime = ZIP_ENTRY_CONSTANT_TIME;
	}

	public void copy(Jar srce, String path, boolean overwrite) {
		check();
		addDirectory(srce.getDirectory(path), overwrite);
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

	public void stripSignatures() {
		Map<String, Resource> metainf = getDirectory("META-INF");
		if (metainf != null) {
			List<String> signing = metainf.keySet()
				.stream()
				.filter(path -> METAINF_SIGNING_P.matcher(path)
					.matches())
				.collect(toList());
			for (String path : signing) {
				remove(path);
			}
		}
	}

	public void removePrefix(String prefixLow) {
		prefixLow = ZipUtil.cleanPath(prefixLow);
		String prefixHigh = prefixLow.concat("\uFFFF");
		resources.subMap(prefixLow, prefixHigh)
			.clear();
		if (prefixLow.endsWith("/")) {
			prefixLow = prefixLow.substring(0, prefixLow.length() - 1);
			prefixHigh = prefixLow.concat("\uFFFF");
		}
		directories.subMap(prefixLow, prefixHigh)
			.clear();
	}

	public void removeSubDirs(String dir) {
		dir = ZipUtil.cleanPath(dir);
		if (!dir.endsWith("/")) {
			dir = dir.concat("/");
		}
		List<String> subDirs = new ArrayList<>(directories.subMap(dir, dir.concat("\uFFFF"))
			.keySet());
		subDirs.forEach(subDir -> removePrefix(subDir.concat("/")));
	}

	private static final Predicate<String> pomXmlFilter = new PathSet("META-INF/maven/*/*/pom.xml").matches();

	public Stream<Resource> getPomXmlResources() {
		return getResources(pomXmlFilter);
	}

	/**
	 * Make this jar calculate the SHA256 when it is saved as a file. When this
	 * JAR is written, the digest is always cleared. If this flag is on, it will
	 * be calculated and set when the file is successfully saved.
	 *
	 * @param onOrOff state of calculating the digest when writing this jar.
	 *            true is on, otherwise off
	 */

	public Jar setCalculateFileDigest(boolean onOrOff) {
		this.calculateFileDigest = onOrOff;
		return this;
	}

	/**
	 * Get the SHA256 digest of the last write operation when
	 * {@link #setCalculateFileDigest(boolean)} was on.
	 *
	 * @return the SHA 256 digest or empty
	 */

	public Optional<byte[]> getSHA256() {
		return Optional.ofNullable(sha256)
			.map(SHA256::digest);
	}

	/**
	 * Get the length of the last written file or -1 if unavailable. The length
	 * is only calculated when the checksum calculation was on during the write.
	 *
	 * @return the length of the last written file or -1 if unavailable.
	 */
	public int getLength() {
		return fileLength;
	}

	/**
	 * Sometimes a Jar is derived from another Jar. In that case this jar's
	 * close should not close the contained resources or file.
	 */

	public void setDerived() {
		this.closeResources = false;
	}
}
