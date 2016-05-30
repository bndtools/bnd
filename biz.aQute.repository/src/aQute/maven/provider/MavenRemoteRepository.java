package aQute.maven.provider;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MetadataParser.ProgramMetadata;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.service.reporter.Reporter;

public class MavenRemoteRepository extends MavenBackingRepository {
	final HttpClient						client;
	final Map<Revision,RevisionMetadata>	revisions	= new ConcurrentHashMap<>();
	final Map<Program,ProgramMetadata>		programs	= new ConcurrentHashMap<>();
	final String							base;

	public MavenRemoteRepository(File root, HttpClient client, String base, Reporter reporter) throws Exception {
		super(root, base, reporter);
		this.client = client;
		this.base = base;
	}

	public TaggedData fetch(String path, File file) throws Exception {
		URL url = new URL(base + path);
		int n = 0;
		while (true)
			try {
				reporter.trace("Fetching %s", path);

				TaggedData tag = client.build().headers("User-Agent", "Bnd").useCache(file).asTag().go(url);
				reporter.trace("Fetched %s", tag);
				if (tag.getState() == State.UPDATED) {

					// https://issues.sonatype.org/browse/NEXUS-4900

					if (!path.endsWith("/maven-metadata.xml")) {
						URL shaUrl = new URL(base + path + ".sha1");
						URL md5Url = new URL(base + path + ".md5");
						Promise<String> sha = client.build().asString().timeout(15000).async(shaUrl);
						Promise<String> md5 = client.build().asString().timeout(15000).async(md5Url);
						if (sha.getFailure() == null) {
							String fileSha = SHA1.digest(file).asHex();
							checkDigest(fileSha, sha.getValue(), file);
						} else if (md5.getFailure() == null) {
							String fileMD5 = MD5.digest(file).asHex();
							checkDigest(fileMD5, md5.getValue(), file);
						}
					}
				}
				return tag;
			} catch (Exception e) {
				n++;
				if (n > 3)
					throw e;
				Thread.sleep(1000 * n);
			}
	}

	public void store(File file, String path) throws Exception {

		int n = 0;
		URL url = new URL(base + path);
		SHA1 sha1 = SHA1.digest(file);
		MD5 md5 = MD5.digest(file);

		Promise<String> psha = client.build()
				.put()
				.upload(sha1.asHex())
				.asString()
				.async(new URL(base + path + ".sha1"));
		Promise<String> pmd5 = client.build().put().upload(md5.asHex()).asString().async(new URL(base + path + ".md5"));

		TaggedData go = client.build().put().upload(file).updateTag().asTag().go(url);

		if (go.getResponseCode() != HttpURLConnection.HTTP_CREATED && go.getResponseCode() != HttpURLConnection.HTTP_OK)
			throw new IOException("Could not store " + path + " from " + file + " with " + go);

		if (psha.getFailure() != null)
			throw (Exception) psha.getFailure();

		if (pmd5.getFailure() != null)
			throw (Exception) psha.getFailure();

	}

	public boolean delete(String path) throws Exception {
		URL url = new URL(base + path);
		TaggedData go = client.build().put().delete().get(TaggedData.class).go(url);
		if (go == null)
			return false;

		if (go.getResponseCode() == HttpURLConnection.HTTP_OK
				|| go.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
			client.build().delete().async(new URL(base + path + ".sha1"));
			client.build().delete().async(new URL(base + path + ".md5"));
			return true;
		}

		throw new HttpRequestException(go);
	}

	public void close() {

	}

	@Override
	public String toString() {
		return "RemoteRepo [base=" + base + ", id=" + id + "]";
	}

	public String getUser() throws Exception {
		return client.getUserFor(base);
	}

	@Override
	public URI toURI(String remotePath) throws Exception {
		return new URI(base + remotePath);
	}
}
