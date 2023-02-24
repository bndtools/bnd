package aQute.bnd.osgi;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import aQute.bnd.osgi.Jar.Compression;
import aQute.lib.collections.Iterables;
import aQute.lib.date.Dates;
import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.lib.zip.ZipUtil;

public class Zip implements Closeable {

	private static final Pattern	DEFAULT_DO_NOT_COPY		= Pattern.compile(Constants.DEFAULT_DO_NOT_COPY);

	protected static final int		BUFFER_SIZE				= IOConstants.PAGE_SIZE * 16;
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

	protected int					fileLength				= -1;
	protected long					zipEntryConstantTime	= ZIP_ENTRY_CONSTANT_TIME;

	protected final NavigableMap<String, Resource>				resources	= new TreeMap<>();
	protected final NavigableMap<String, Map<String, Resource>>	directories	= new TreeMap<>();

	private String												name;
	private File												source;
	private ZipFile												zipFile;
	private long												lastModified;
	private String												lastModifiedReason;
	private boolean												reproducible;
	private boolean												closed;
	protected Compression										compression	= Compression.DEFLATE;
	public Zip(String name) {
		this.name = name;
	}

	public Zip(String name, File dirOrFile, Pattern doNotCopy) throws IOException {
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

	public Zip(String name, InputStream in, long lastModified) throws IOException {
		this(name, in);
	}

	public Zip(String name, String path) throws IOException {
		this(name, new File(path));
	}

	public Zip(String name, InputStream in) throws IOException {
		this(name);
		buildFromInputStream(in);
	}

	public Zip(File f) throws IOException {
		this(getName(f), f, null);
	}

	public Zip(String string, File file) throws IOException {
		this(string, file, DEFAULT_DO_NOT_COPY);
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

	protected void putEntry(ZipOutputStream jout, ZipEntry entry, Resource r) throws Exception {

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

	/**
	 * @deprecated Replaced by {@link #setReproducible(String)}.
	 */
	@Deprecated
	public void setReproducible(boolean reproducible) {
		this.reproducible = reproducible;
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

	/**
	 * Get the length of the last written file or -1 if unavailable. The length
	 * is only calculated when the checksum calculation was on during the write.
	 *
	 * @return the length of the last written file or -1 if unavailable.
	 */
	public int getLength() {
		return fileLength;
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

	public boolean putResource(String path, Resource resource) {
		return putResource(path, resource, true);
	}

	public boolean putResource(String path, Resource resource, boolean overwrite) {
		check();
		path = ZipUtil.cleanPath(path);

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

	public boolean exists(String path) {
		check();
		path = ZipUtil.cleanPath(path);
		return resources.containsKey(path);
	}

	public boolean isEmpty() {
		check();
		return resources.isEmpty();
	}

	private void buildFromDirectory(final Path baseDir, final Pattern doNotCopy) throws IOException {
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
	}

	private void buildFromZip(File file) throws IOException {
		try {
			zipFile = new ZipFile(file);
			for (ZipEntry entry : Iterables.iterable(zipFile.entries())) {
				if (entry.isDirectory()) {
					continue;
				}
				putResource(entry.getName(), new ZipResource(zipFile, entry), true);
			}
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

	protected void buildFromResource(Resource resource) throws Exception {
		buildFromInputStream(resource.openInputStream());
	}

	protected void buildFromInputStream(InputStream in) throws IOException {
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
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	protected static String clean(String s) {
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

	@Override
	public void close() {
		this.closed = true;
		IO.close(zipFile);
		resources.values()
			.forEach(IO::close);
		resources.clear();
		directories.clear();
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

	public File getSource() {
		check();
		return source;
	}

	public void setSource(File source) {
		this.source = source;
	}

	public boolean rename(String oldPath, String newPath) {
		check();
		Resource resource = remove(oldPath);
		if (resource == null)
			return false;

		return putResource(newPath, resource);
	}

	protected String padString(String s, int length, char pad) {
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

	protected void copyResource(File dir, String path, Resource resource) throws Exception {
		File to = IO.getBasedFile(dir, path);
		IO.mkdirs(to.getParentFile());
		IO.copy(resource.openInputStream(), to);
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

	protected void writeResource(ZipOutputStream jout, Set<String> directories, String path, Resource resource)
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

	@Override
	public String toString() {
		return "Zip:" + getName();
	}
}
