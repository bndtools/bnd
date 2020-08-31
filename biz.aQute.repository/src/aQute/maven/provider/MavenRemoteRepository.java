package aQute.maven.provider;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.exceptions.SupplierWithException;
import aQute.lib.io.IO;
import aQute.libg.cryptography.Digest;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.libg.uri.URIUtil;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MetadataParser.ProgramMetadata;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.service.reporter.Reporter;

public class MavenRemoteRepository extends MavenBackingRepository {
	private static final String				NO_DIGEST			= "No digest";

	private final static Logger				logger				= LoggerFactory.getLogger(MavenRemoteRepository.class);

	// MD5(xercesImpl-2.9.0.jar)= 33ec8d237cbaceeffb2c2a7f52afd79a
	// SHA1(xercesImpl-2.9.0.jar)= 868c0792233fc78d8c9bac29ac79ade988301318

	final static Pattern					DIGEST_POLLUTED		= Pattern.compile("([0-9A-F][0-9A-F]){16,100}",
		Pattern.CASE_INSENSITIVE);

	final HttpClient						client;
	final Map<Revision, RevisionMetadata>	revisions			= new ConcurrentHashMap<>();
	final Map<Program, ProgramMetadata>		programs			= new ConcurrentHashMap<>();
	final String							base;
	final static long						DEFAULT_MAX_STALE	= TimeUnit.HOURS.toMillis(1);
	final boolean							remote;

	public MavenRemoteRepository(File root, HttpClient client, String base, Reporter reporter) throws Exception {
		super(root, base, reporter);
		this.client = client;
		this.base = base;

		URI uri = new URI(base);
		remote = URIUtil.isRemote(uri);
	}

	@Override
	public TaggedData fetch(String path, File file) throws Exception {
		return fetch(path, file, 3, 1000L);
	}

	private TaggedData fetch(String path, File file, int retries, long delay) throws Exception {
		logger.debug("Fetching {}", path);
		String cause;

		if (client.isOffline()) {
			if (file == null || !file.isFile())
				throw new IllegalStateException("We're offline and we do not have a file for: " + path);

			return new TaggedData(new URI(base + path), 304, file);
		}

		while (true) {
			TaggedData tag = null;
			try {
				tag = client.build()
					.headers("User-Agent", "Bnd")
					.useCache(file, 0)
					.retries(retries)
					.asTag()
					.go(new URI(base + path));

				switch (tag.getState()) {
					case UNMODIFIED :
					case NOT_FOUND :
						return tag;

					case UPDATED :
						cause = digest(path + ".sha1", () -> SHA1.digest(file));
						if (cause == null)
							return tag;

						String cause2 = digest(path + ".md5", () -> MD5.digest(file));
						if (cause2 == null)
							return tag;

						if (cause.equals(NO_DIGEST) && cause2.equals(NO_DIGEST))
							return tag;

						cause = "sh1 " + cause + "\n" + "md5 " + cause2;
						IO.delete(file);
						break;

					default :
					case OTHER :
						cause = tag.toString();
						break;

				}
			} catch (Exception e) {
				cause = Exceptions.causes(e);
				logger.info("Something failed in downloading {}: {}", path, cause);
			}

			if (retries-- <= 0)
				throw new IllegalStateException(String
					.format("Cannot download %s into %s after %s tries, last cause %s", path, file, retries, cause));

			long minDelay = Math.min(delay * 2L, TimeUnit.MINUTES.toMillis(10));
			logger.info("Retrying failed download: {}. delay={}, retries={}, cause={}", path, minDelay, retries, cause);
			Thread.sleep(minDelay);
		}
	}

	private String digest(String path, SupplierWithException<Digest> digest) throws MalformedURLException, Exception {
		// https://issues.sonatype.org/browse/NEXUS-4900
		String remoteDigest = client.build()
			.headers("User-Agent", "Bnd")
			.asString()
			.retries(3)
			.go(new URI(base + path));

		if (remoteDigest == null)
			return NO_DIGEST;

		Matcher matcher = DIGEST_POLLUTED.matcher(remoteDigest);
		if (!matcher.find()) {
			return "Invalid digest " + remoteDigest + " expected " + digest.get();
		}
		remoteDigest = matcher.group(0);
		if (!remoteDigest.equalsIgnoreCase(digest.get()
			.asHex())) {
			return "Digest values not equal file=" + digest.get() + " remote cleaned up=" + remoteDigest;
		}
		return null;
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
	public String getUser() {
		try {
			return client.getUserFor(base);
		} catch (MalformedURLException e) {
			return "no user for invalid url " + e.getMessage();
		} catch (Exception e) {
			return "no user : " + Exceptions.causes(e);
		}
	}

	@Override
	public URI toURI(String remotePath) throws Exception {
		return new URI(base + remotePath);
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public boolean isRemote() {
		return remote;
	}

	@Override
	public String toString() {
		return "RemoteRepo [base=" + base + ", id=" + id + ", user=" + getUser() + "]";
	}

}

