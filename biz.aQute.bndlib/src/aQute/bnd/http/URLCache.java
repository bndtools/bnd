package aQute.bnd.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;

public class URLCache {
	private final static Logger			logger	= LoggerFactory.getLogger(URLCache.class);
	private final static JSONCodec		codec	= new JSONCodec();

	private final File					root;

	private ConcurrentMap<File, Info>	infos	= new ConcurrentHashMap<>();

	public static class InfoDTO {
		public String	etag;
		public String	sha_1;
		public long		modified;
		public URI		uri;
		public String	sha_256;
	}

	public class Info implements Closeable {
		File			file;
		File			jsonFile;
		InfoDTO			dto;
		URI				url;
		ReentrantLock	lock	= new ReentrantLock();

		@Deprecated
		public Info(URI url) throws Exception {
			this(getCacheFileFor(url), url);
		}

		public Info(File content, URI url) throws Exception {
			this.file = content;
			this.url = url;
			this.jsonFile = new File(content.getParentFile(), content.getName() + ".json");
			if (this.jsonFile.isFile()) {
				try {
					this.dto = codec.dec()
						.from(jsonFile)
						.get(InfoDTO.class);
				} catch (Exception e) {
					this.dto = new InfoDTO();
					logger.error("URLCache Failed to load data for {} from {}", content, jsonFile);
				}
			} else {
				this.dto = new InfoDTO();
			}
			this.dto.uri = url;
		}

		@Override
		public void close() throws IOException {
			logger.debug("Unlocking url cache {}", url);
			lock.unlock();
		}

		public void update(InputStream inputStream, String etag, long modified) throws Exception {
			IO.mkdirs(this.file.getParentFile());
			IO.copy(inputStream, this.file);
			if (modified > 0) {
				this.file.setLastModified(modified);
			}
			update(etag);
		}

		public void update(String etag) throws Exception {
			this.dto.sha_1 = SHA1.digest(file)
				.asHex();
			this.dto.sha_256 = SHA256.digest(file)
				.asHex();
			this.dto.etag = etag;
			this.dto.modified = file.lastModified();
			codec.enc()
				.to(jsonFile)
				.put(this.dto);
		}

		public boolean isPresent() {
			boolean f = file.isFile();
			boolean j = jsonFile.isFile();
			return f && j;
		}

		public void delete() {
			IO.delete(file);
			IO.delete(jsonFile);
		}

		public String getETag() {
			return dto.etag;
		}

		public long getModified() {
			return dto.modified;
		}

		@Override
		public String toString() {
			return "Info [file=" + file + ", etag=" + dto.etag + ", modified=" + Instant.ofEpochMilli(dto.modified)
				+ ", url=" + url + ", lock=" + lock + "]";
		}

	}

	public URLCache(File root) {
		this.root = new File(root, "shas");
		try {
			IO.mkdirs(this.root);
		} catch (IOException e) {
			throw Exceptions.duck(e);
		}
	}

	public Info get(URI uri) throws Exception {
		return get(null, uri);
	}

	public Map<File, Info> infos() {
		return Collections.unmodifiableMap(infos);
	}

	public Info get(File file, URI uri) throws Exception {
		if (file == null)
			file = getCacheFileFor(uri);

		// Unfortunately, the Info constructor throws Exception, making
		// ConcurrentMap.computeIfAbsent() impossible. So we do it the
		// hard(er) way
		Info info = infos.get(file);
		if (info == null) {
			Info candidate = new Info(file, uri);
			info = infos.putIfAbsent(file, candidate);
			if (info == null) {
				info = candidate;
			}
		}

		// FIXME: 5 minutes is a bit excessive, no?
		if (!info.lock.tryLock(5, TimeUnit.MINUTES)) {
			logger.debug("Could not lock URL cache for {} - {}", uri, info);
		}

		return info;
	}

	public static String toName(URI uri) throws Exception {
		return SHA1.digest(uri.toASCIIString()
			.getBytes(StandardCharsets.UTF_8))
			.asHex();
	}

	public static void update(File file, String tag) {
		throw new UnsupportedOperationException();
	}

	public File getCacheFileFor(URI url) throws Exception {
		return new File(root, toName(url) + ".content");
	}

	public File getCacheInfoFileFor(URI url) throws Exception {
		return new File(root, toName(url) + ".content.json");
	}

	public boolean clear(URI uri) throws Exception {
		File f = getCacheFileFor(uri);
		boolean exists = f.isFile();
		if (f != null && exists) {
			IO.deleteWithException(f);
		}
		f = getCacheInfoFileFor(uri);
		if (f != null && f.isFile()) {
			IO.deleteWithException(f);
		}
		infos.remove(f);
		return exists;
	}

	public boolean isCached(URI url) throws Exception {
		return getCacheFileFor(url).isFile();
	}

}
