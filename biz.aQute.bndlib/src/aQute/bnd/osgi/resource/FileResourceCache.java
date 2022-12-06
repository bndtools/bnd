package aQute.bnd.osgi.resource;

import static aQute.bnd.exceptions.SupplierWithException.asSupplierOrElse;
import static aQute.bnd.osgi.Constants.MIME_TYPE_BUNDLE;
import static aQute.bnd.osgi.Constants.MIME_TYPE_JAR;

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

import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Domain;
import aQute.libg.cryptography.SHA256;

class FileResourceCache {
	private final static Logger				logger					= LoggerFactory.getLogger(FileResourceCache.class);
	private final static long				EXPIRED_DURATION_NANOS	= TimeUnit.NANOSECONDS.convert(30L,
		TimeUnit.MINUTES);
	private static final FileResourceCache	INSTANCE				= new FileResourceCache();
	private final Map<CacheKey, Resource>	cache;
	private long							time;

	private FileResourceCache() {
		cache = new ConcurrentHashMap<>();
		time = System.nanoTime();
	}

	static FileResourceCache getInstance() {
		return INSTANCE;
	}

	Resource getResource(File file, URI uri) {
		if (!file.isFile()) {
			return null;
		}
		// Make sure we don't grow infinitely
		final long now = System.nanoTime();
		if ((now - time) > EXPIRED_DURATION_NANOS) {
			time = now;
			cache.keySet()
				.removeIf(key -> (now - key.time) > EXPIRED_DURATION_NANOS);
		}
		CacheKey cacheKey = new CacheKey(file);
		Resource resource = cache.computeIfAbsent(cacheKey, key -> {
			logger.debug("parsing {}", file);
			ResourceBuilder rb = new ResourceBuilder();
			try {
				Domain manifest = Domain.domain(file);
				boolean hasIdentity = false;
				if (manifest != null) {
					hasIdentity = rb.addManifest(manifest);
				}
				String mime = hasIdentity ? MIME_TYPE_BUNDLE : MIME_TYPE_JAR;
				DeferredValue<String> sha256 = new DeferredComparableValue<>(String.class,
					asSupplierOrElse(() -> SHA256.digest(file)
						.asHex(), null),
					key.hashCode());
				rb.addContentCapability(uri, sha256, file.length(), mime);

				if (hasIdentity) {
					rb.addHashes(file);
				}
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
			return rb.build();
		});
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
