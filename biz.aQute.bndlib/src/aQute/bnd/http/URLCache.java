package aQute.bnd.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

public class URLCache {
	private static final long		TIMEOUT	= TimeUnit.MINUTES.toMillis(5);
	private final static JSONCodec	codec	= new JSONCodec();

	private final File	root;
	private Reporter	reporter	= new Slf4jReporter(URLCache.class);
	private static String	whom;

	public static class InfoDTO {
		public String	etag;
		public String	sha_1;
		public long		modified;
		public URI		uri;
		public String	sha_256;
	}

	public class Info implements Closeable {
		File	file;
		File	lockFile;
		File	jsonFile;
		InfoDTO	dto;
		URI		url;

		public Info(URI url) throws Exception {
			this(getCacheFileFor(url), url);
		}

		public Info(File content, URI url) throws Exception {
			this.file = content;
			this.url = url;
			this.lockFile = new File(content.getParentFile(), content.getName() + ".lock");
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
			lock();
		}

		@Override
		public synchronized void close() throws IOException {
			whom = null;
			IO.delete(lockFile);
		}

		public void update(InputStream inputStream, String etag, long modified) throws Exception {
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
			return file.isFile() && jsonFile.isFile() && dto.etag != null;
		}

		private synchronized void lock() throws InterruptedException {

			if (lockFile.isFile())
				IO.delete(lockFile);

			long deadline = lockFile.lastModified() + TIMEOUT;
			while (lockFile.mkdirs() == false) {
				if (System.currentTimeMillis() > deadline) {
					IO.delete(lockFile);
					reporter.error("Had to delete lockfile %s due to timeout for %s", lockFile, dto);
				}
				reporter.trace("Waiting on lock %s we=%s whom=%s", lockFile, Thread.currentThread().getName(), whom);
				Thread.sleep(5000);
			}
			whom = Thread.currentThread().getName();
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
	}

	public Info get(URI uri) throws Exception {
		return new Info(uri);
	}

	public Info get(File file, URI uri) throws Exception {
		if (file == null)
			return get(uri);

		return new Info(file, uri);
	}

	public static String toName(URI uri) throws Exception {
		return SHA1.digest(uri.toASCIIString().getBytes(StandardCharsets.UTF_8)).asHex();
	}

	public static void update(File file, String tag) {
		// TODO Auto-generated method stub

	}

	public File getCacheFileFor(URI url) throws Exception {
		return new File(root, toName(url) + ".content");
	}

}
