package aQute.maven.provider;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.exceptions.Exceptions;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MetadataParser.ProgramMetadata;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.service.reporter.Reporter;

public class MavenRemoteRepository extends MavenBackingRepository {
	private final static Logger				logger				= LoggerFactory.getLogger(MavenRemoteRepository.class);
	final HttpClient						client;
	final Map<Revision, RevisionMetadata>	revisions			= new ConcurrentHashMap<>();
	final Map<Program, ProgramMetadata>		programs			= new ConcurrentHashMap<>();
	final String							base;
	final static long						DEFAULT_MAX_STALE	= TimeUnit.HOURS.toMillis(1);

	public MavenRemoteRepository(File root, HttpClient client, String base, Reporter reporter) throws Exception {
		super(root, base, reporter);
		this.client = client;
		this.base = base;
	}

	@Override
	public TaggedData fetch(String path, File file) throws Exception {
		Promise<TaggedData> promise = fetch(path, file, 3, 1000L);
		Throwable failure = promise.getFailure(); // wait for completion
		if (failure != null) {
			throw Exceptions.duck(failure);
		}
		return promise.getValue();
	}

	private Promise<TaggedData> fetch(String path, File file, int retries, long delay) throws Exception {
		logger.debug("Fetching {}", path);
		return client.build()
			.headers("User-Agent", "Bnd")
			.useCache(file, DEFAULT_MAX_STALE)
			.asTag()
			.async(new URL(base + path))
			.then(success -> success.flatMap(tag -> {
				logger.debug("Fetched {}", tag);
				if ((tag.getState() != State.UPDATED) || path.endsWith("/maven-metadata.xml")) {
					return success;
				}
				// https://issues.sonatype.org/browse/NEXUS-4900
				return client.build()
					.asString()
					.timeout(15000)
					.async(new URL(base + path + ".sha1"))
					.flatMap(sha -> {
						if (sha != null) {
							String fileSha = SHA1.digest(file)
								.asHex();
							checkDigest(fileSha, sha, file);
							return success;
						}
						return client.build()
							.asString()
							.timeout(15000)
							.async(new URL(base + path + ".md5"))
							.flatMap(md5 -> {
								if (md5 != null) {
									String fileMD5 = MD5.digest(file)
										.asHex();
									checkDigest(fileMD5, md5, file);
								}
								return success;
							});
					});
			})
				.recoverWith(failed -> {
					if (retries < 1) {
						return null; // no recovery
					}
					logger.info("Retrying invalid download: {}. delay={}, retries={}", failed.getFailure()
						.getMessage(), delay, retries);
					@SuppressWarnings("unchecked")
					Promise<TaggedData> delayed = (Promise<TaggedData>) failed.delay(delay);
					return delayed.recoverWith(
						f -> fetch(path, file, retries - 1, Math.min(delay * 2L, TimeUnit.MINUTES.toMillis(10))));
				}));
	}

	@Override
	public void store(File file, String path) throws Exception {

		int n = 0;
		URL url = new URL(base + path);
		SHA1 sha1 = SHA1.digest(file);
		MD5 md5 = MD5.digest(file);

		try (TaggedData go = client.build()
			.put()
			.upload(file)
			.updateTag()
			.asTag()
			.go(url);) {

			switch (go.getState()) {
				case NOT_FOUND :
				case OTHER :
					throw new IOException("Could not store " + path + " from " + file + " with " + go);

				case UNMODIFIED :
				case UPDATED :
				default :
					break;
			}
		}
		try (TaggedData tag = client.build()
			.put()
			.upload(sha1.asHex())
			.asTag()
			.go(new URL(base + path + ".sha1"))) {}
		try (TaggedData tag = client.build()
			.put()
			.upload(md5.asHex())
			.asTag()
			.go(new URL(base + path + ".md5"))) {}

	}

	@Override
	public boolean delete(String path) throws Exception {
		URL url = new URL(base + path);
		TaggedData go = client.build()
			.put()
			.delete()
			.get(TaggedData.class)
			.go(url);
		if (go == null)
			return false;

		if (go.getResponseCode() == HttpURLConnection.HTTP_OK
			|| go.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
			client.build()
				.delete()
				.async(new URL(base + path + ".sha1"));
			client.build()
				.delete()
				.async(new URL(base + path + ".md5"));
			return true;
		}

		throw new HttpRequestException(go);
	}

	@Override
	public void close() {

	}

	@Override
	public String toString() {
		return "RemoteRepo [base=" + base + ", id=" + id + "]";
	}

	@Override
	public String getUser() throws Exception {
		return client.getUserFor(base);
	}

	@Override
	public URI toURI(String remotePath) throws Exception {
		return new URI(base + remotePath);
	}

	@Override
	public boolean isFile() {
		return false;
	}
}
