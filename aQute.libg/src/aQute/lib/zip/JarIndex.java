package aQute.lib.zip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.exceptions.SupplierWithException;
import aQute.lib.hierarchy.Hierarchy;
import aQute.lib.io.IO;

/**
 * Creates a Hierarchy on a ZipFile, a directory, or a ZipStream.
 */
public class JarIndex extends Hierarchy {

	public interface NodeInfo {

		InputStream open() throws IOException, Exception;

		Optional<File> file();

		long size();

		long lastModified();

	}

	public JarIndex(InputStream in) throws IOException {
		super(buildFromInputStream(in, null));
	}

	public JarIndex(File in) throws IOException {
		super(build(in, null, null));
	}

	public JarIndex(File in, Pattern doNotCopy) throws IOException {
		super(build(in, doNotCopy, null));
	}

	private static Map<String, Object> build(File file, Pattern doNotCopy, Function<NodeInfo, ?> f) throws IOException {
		if (file.isDirectory())
			return buildFromDirectory(file.toPath(), doNotCopy, f);
		if (file.isFile()) {
			return buildFromZip(file, f);
		}
		return null;
	}

	private static Map<String, Object> buildFromDirectory(final Path baseDir, final Pattern doNotCopy,
		Function<NodeInfo, ?> f) throws IOException {
		Map<String, Object> map = new HashMap<>();
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
					Object payload = f == null ? null : f.apply(getNodeInfo(file.toFile()));
					addFile(map, relativePath, payload);
					return FileVisitResult.CONTINUE;
				}
			});
		return map;
	}

	private static Map<String, Object> buildFromZip(File file, Function<NodeInfo, ?> f) throws IOException {
		Map<String, Object> map = new HashMap<>();

		try (ZipFile zipFile = new ZipFile(file)) {

			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry element = entries.nextElement();
				if (!element.isDirectory()) {
					Object payload = f == null ? null
						: getPayload(f, getNodeInfo(element, () -> zipFile.getInputStream(element)));
					addFile(map, ZipUtil.cleanPath(element.getName()), payload);
				}
			}
			return map;
		} catch (ZipException e) {
			ZipException ze = new ZipException(
				"The JAR/ZIP file (" + file.getAbsolutePath() + ") seems corrupted, error: " + e.getMessage());
			ze.initCause(e);
			throw ze;
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Problem opening JAR: " + file.getAbsolutePath(), e);
		} catch (IOException e) {
			throw e;
		}
	}

	private static Map<String, Object> buildFromInputStream(InputStream in, Function<NodeInfo, ?> f)
		throws IOException {
		Map<String, Object> map = new HashMap<>();
		try (ZipInputStream jin = new ZipInputStream(in)) {
			for (ZipEntry entry; (entry = jin.getNextEntry()) != null;) {
				if (!entry.isDirectory()) {
					Object payload = f == null ? null : f.apply(getNodeInfo(entry, () -> jin));
					addFile(map, ZipUtil.cleanPath(entry.getName()), payload);
				}
			}
		}
		return map;
	}

	private static Object getPayload(Function<NodeInfo, ?> f, NodeInfo nodeInfo) {
		Object o = f.apply(nodeInfo);
		assert !(o instanceof Map);
		return o;
	}

	private static final Pattern PATH_SPLITTER = Pattern.compile("/");

	private static void addFile(Map<String, Object> map, String path, Object payload) {
		if (path.isEmpty())
			return;

		String parts[] = PATH_SPLITTER.split(path);

		addFile(map, parts, 0, path, payload);
	}

	@SuppressWarnings("unchecked")
	private static void addFile(Map<String, Object> map, String[] parts, int i, String path, Object payload) {
		assert i < parts.length;
		if (i == parts.length - 1) {
			map.put(parts[i], payload);
		} else {
			Map<String, Object> folder = (Map<String, Object>) map.computeIfAbsent(parts[i],
				k -> new HashMap<String, Object>());

			addFile(folder, parts, i + 1, null, payload);
		}
	}

	private static NodeInfo getNodeInfo(File in) {
		return new NodeInfo() {
			@Override
			public InputStream open() throws IOException {
				return IO.stream(in);
			}

			@Override
			public long lastModified() {
				return in.lastModified();
			}

			@Override
			public long size() {
				return in.length();
			}

			@Override
			public Optional<File> file() {
				return Optional.of(in);
			}
		};
	}

	private static NodeInfo getNodeInfo(ZipEntry entry, SupplierWithException<InputStream> open) {
		return new NodeInfo() {
			@Override
			public InputStream open() throws IOException {
				try {
					return open.get();
				} catch (IOException e) {
					throw e;
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

			@Override
			public long lastModified() {
				return ZipUtil.getModifiedTime(entry);
			}

			@Override
			public long size() {
				return entry.getSize();
			}

			@Override
			public Optional<File> file() {
				return Optional.empty();
			}
		};
	}
}
