package aQute.bnd.osgi.resource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.service.resource.CompositeResource;

class FileResourceCache {
	private final static long						EXPIRED_DURATION_NANOS	= TimeUnit.NANOSECONDS.convert(30L,
		TimeUnit.MINUTES);
	private static final FileResourceCache			INSTANCE				= new FileResourceCache();
	private final Map<CacheKey, CompositeResource>	cache;
	private long									time;

	private FileResourceCache() {
		cache = new ConcurrentHashMap<>();
		time = System.nanoTime();
	}

	static FileResourceCache getInstance() {
		return INSTANCE;
	}

	CompositeResource getResource(File file, URI uri, Supplier<CompositeResource> create) {
		// Make sure we don't grow infinitely
		final long now = System.nanoTime();
		if ((now - time) > EXPIRED_DURATION_NANOS) {
			time = now;
			cache.keySet()
				.removeIf(key -> (now - key.time) > EXPIRED_DURATION_NANOS);
		}
		CacheKey cacheKey = new CacheKey(file);
		CompositeResource resource = cache.computeIfAbsent(cacheKey, key -> create.get());
		return resource;
	}

	static final class CacheKey {
		private final Object	fileKey;
		private final long		lastModifiedTime;
		private final long		size;
		final long				time;

		CacheKey(File file) {
			this(file.toPath());
		}

		CacheKey(Path path) {
			BasicFileAttributes attributes;
			try {
				attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class)
					.readAttributes();
			} catch (IOException e) {
				throw Exceptions.duck(e);
			}
			if (!attributes.isRegularFile()) {
				throw new IllegalArgumentException("File must be a regular file: " + path);
			}
			Object fileKey = attributes.fileKey();
			this.fileKey = (fileKey != null) ? fileKey //
				: path.toAbsolutePath(); // Windows FS does not have fileKey
			this.lastModifiedTime = attributes.lastModifiedTime()
				.toMillis();
			this.size = attributes.size();
			this.time = System.nanoTime();
		}

		@Override
		public int hashCode() {
			return (Objects.hashCode(fileKey) * 31 + Long.hashCode(lastModifiedTime)) * 31 + Long.hashCode(size);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof CacheKey other)) {
				return false;
			}
			return Objects.equals(fileKey, other.fileKey) && (lastModifiedTime == other.lastModifiedTime)
				&& (size == other.size);
		}

		@Override
		public String toString() {
			return Objects.toString(fileKey);
		}
	}
}
