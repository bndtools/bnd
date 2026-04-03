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
import aQute.bnd.service.resource.SupportingResource;

/**
 * A cache for {@link SupportingResource}s associated with {@link File}s. The
 * cache is implemented as a concurrent hash map. The cache key consists of the
 * file path, size, and last modification time. The resources are created on
 * demand via the provided {@link Supplier}.
 */
class FileResourceCache {
	private final static long						EXPIRED_DURATION_NANOS	= TimeUnit.NANOSECONDS.convert(30L,
		TimeUnit.MINUTES);
	private static final FileResourceCache			INSTANCE				= new FileResourceCache();
	private final Map<CacheKey, SupportingResource>	cache;
	private long									time;

	private FileResourceCache() {
		cache = new ConcurrentHashMap<>();
		time = System.nanoTime();
	}

	/**
	 * Get the singleton instance of the cache.
	 *
	 * @return The cache instance.
	 */
	static FileResourceCache getInstance() {
		return INSTANCE;
	}

	/**
	 * Get a resource for a file. If a resource for the file already exists in
	 * the cache, it is returned. Otherwise, a new resource is created using the
	 * provided {@link Supplier} and added to the cache before being returned.
	 *
	 * @param file The file.
	 * @param uri The URI associated with the file.
	 * @param create The function to create a new resource.
	 * @return The resource.
	 */
	SupportingResource getResource(File file, URI uri, Supplier<SupportingResource> create) {
		// Make sure we don't grow infinitely
		final long now = System.nanoTime();
		if ((now - time) > EXPIRED_DURATION_NANOS) {
			time = now;
			cache.keySet()
				.removeIf(key -> (now - key.time) > EXPIRED_DURATION_NANOS);
		}
		CacheKey cacheKey = new CacheKey(file.toPath(), uri);
		SupportingResource resource = cache.computeIfAbsent(cacheKey, key -> create.get());
		return resource;
	}

	/**
	 * A key used to identify a file in the cache. The key is based on the file
	 * path, size, and last modification time.
	 */
	static final class CacheKey {
		final Object	fileKey;
		final URI		uri;
		final long		lastModifiedTime;
		final long		size;
		final long		time;

		CacheKey(Path path, URI uri) {
			this.uri = uri;
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

		CacheKey(Path path) {
			this(path, null);
		}

		@Override
		public int hashCode() {
			return Objects.hash(fileKey, lastModifiedTime, size, uri);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			return Objects.equals(fileKey, other.fileKey) && lastModifiedTime == other.lastModifiedTime
				&& size == other.size && Objects.equals(uri, other.uri);
		}

		@Override
		public String toString() {
			return Objects.toString(fileKey) + "-" + Objects.toString(uri);
		}
	}
}
