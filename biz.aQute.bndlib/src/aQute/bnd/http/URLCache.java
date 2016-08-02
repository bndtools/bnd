package aQute.bnd.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

public class URLCache {
	private final static JSONCodec		codec		= new JSONCodec();

	private final File					root;
	private Reporter					reporter	= new Slf4jReporter(URLCache.class);

	private ConcurrentMap<File,Info>	infos		= new ConcurrentHashMap<>();

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
					this.dto = codec.dec().from(jsonFile).get(InfoDTO.class);
				} catch (Exception e) {
					this.dto = new InfoDTO();
					reporter.error("URLCache Failed to load data for %s from %s", content, jsonFile);
				}
			} else {
				this.dto = new InfoDTO();
			}
			this.dto.uri = url;
		}

		@Override
		public void close() throws IOException {
			reporter.trace("Unlocking url cache %s", url);
			lock.unlock();
		}

		public void update(InputStream inputStream, String etag, long modified) throws Exception {
			this.file.getParentFile().mkdirs();
			IO.copy(inputStream, this.file);
			codec.enc().to(jsonFile).put(this.dto);
			if (modified > 0) {
				this.file.setLastModified(modified);
				this.jsonFile.setLastModified(modified);
			}
			update(etag);
		}

		public void update(String etag) throws Exception {
			this.dto.sha_1 = SHA1.digest(file).asHex();
			this.dto.sha_256 = SHA256.digest(file).asHex();
			this.dto.etag = etag;
			this.dto.modified = file.lastModified();
			codec.enc().to(jsonFile).put(this.dto);
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
	}

	public URLCache(File root) {
		this.root = new File(root, "shas");
		this.root.mkdirs();
	}

	public Info get(URI uri) throws Exception {
		return get(null, uri);
	}

	public Info get(File file, URI uri) throws Exception {
		synchronized (this) {
			if (file == null)
				file = getCacheFileFor(uri);
			Info info = infos.get(file);
			if (info == null) {
				info = new Info(file, uri);
				infos.put(file, info);
			}

			reporter.trace("Locking url cache %s - %s", uri, info);
			if (info.lock.tryLock(5, TimeUnit.MINUTES)) {
				reporter.trace("Locked url cache %s - %s", uri, info);
			} else {
				reporter.trace("Could not locked url cache %s - %s", uri, info);
			}

			return info;
		}
	}

	public static String toName(URI uri) throws Exception {
		return SHA1.digest(uri.toASCIIString().getBytes(StandardCharsets.UTF_8)).asHex();
	}

	public static void update(File file, String tag) {
		throw new UnsupportedOperationException();
	}

	public File getCacheFileFor(URI url) throws Exception {
		return new File(root, toName(url) + ".content");
	}

}
